package com.devpro58.hnem06.moneysnap.data.repository

import com.devpro58.hnem06.moneysnap.data.local.dao.PaymentMethodDao
import com.devpro58.hnem06.moneysnap.data.local.entity.PaymentMethodEntity
import com.devpro58.hnem06.moneysnap.data.mapper.toDomain
import com.devpro58.hnem06.moneysnap.data.remote.firestore.FirestorePaymentMethodSource
import com.devpro58.hnem06.moneysnap.domain.model.PaymentMethod
import com.devpro58.hnem06.moneysnap.domain.model.PaymentMethodSyncError
import com.devpro58.hnem06.moneysnap.domain.model.PaymentMethodSyncException
import com.devpro58.hnem06.moneysnap.domain.repository.PaymentMethodRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FirebaseFirestoreException

@Singleton
class PaymentMethodRepositoryImpl @Inject constructor(
    private val paymentMethodDao: PaymentMethodDao,
    private val remoteSource: FirestorePaymentMethodSource
) : PaymentMethodRepository {

    override fun observePaymentMethods(userId: String): Flow<List<PaymentMethod>> =
        channelFlow {
            launch {
                runCatching { synchronizeExistingMethods(userId) }
                remoteSource.observePaymentMethods(userId)
                    .catch { /* Keep serving the Room cache while offline. */ }
                    .collectLatest { remoteMethods ->
                        val uniqueMethods = remoteMethods
                            .sortedByDescending { it.updatedAtMillis }
                            .distinctBy { it.name.trim().lowercase() }
                        paymentMethodDao.replaceCustomMethods(userId, uniqueMethods)
                    }
            }
            paymentMethodDao.observeByUser(userId).collectLatest { methods ->
                send(methods.map { it.toDomain() })
            }
        }

    override suspend fun ensureDefaultPaymentMethods(userId: String) {
        if (userId.isBlank()) return

        val existing = paymentMethodDao.getByUser(userId)
        val existingBuiltInKeys = existing.mapNotNullTo(mutableSetOf()) { it.builtInKey }
        val now = System.currentTimeMillis()

        if (PaymentMethod.BUILT_IN_CASH !in existingBuiltInKeys) {
            paymentMethodDao.upsert(
                defaultMethod(
                    userId = userId,
                    builtInKey = PaymentMethod.BUILT_IN_CASH,
                    now = now
                )
            )
        }

        if (PaymentMethod.BUILT_IN_BANK_TRANSFER !in existingBuiltInKeys) {
            paymentMethodDao.upsert(
                defaultMethod(
                    userId = userId,
                    builtInKey = PaymentMethod.BUILT_IN_BANK_TRANSFER,
                    now = now
                )
            )
        }
    }

    override suspend fun addPaymentMethod(userId: String, name: String): PaymentMethod {
        val now = System.currentTimeMillis()
        val trimmedName = name.trim()
        require(trimmedName.isNotBlank()) { "Payment method name is blank" }

        val method = PaymentMethodEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = trimmedName,
            builtInKey = null,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        runRemoteWrite { remoteSource.upsertPaymentMethod(method) }
        paymentMethodDao.upsert(method)
        return method.toDomain()
    }

    override suspend fun updatePaymentMethod(methodId: String, name: String) {
        val trimmedName = name.trim()
        require(trimmedName.isNotBlank()) { "Payment method name is blank" }
        val existing = paymentMethodDao.getById(methodId)
            ?.takeIf { it.builtInKey == null }
            ?: error("Payment method not found")
        val updated = existing.copy(
            name = trimmedName,
            updatedAtMillis = System.currentTimeMillis()
        )
        runRemoteWrite { remoteSource.upsertPaymentMethod(updated) }
        paymentMethodDao.upsert(updated)
    }

    override suspend fun deletePaymentMethod(methodId: String) {
        val existing = paymentMethodDao.getById(methodId)
            ?.takeIf { it.builtInKey == null }
            ?: return
        runRemoteWrite { remoteSource.deletePaymentMethod(existing.userId, methodId) }
        paymentMethodDao.deleteCustomById(methodId)
    }

    private suspend fun synchronizeExistingMethods(userId: String) {
        val remote = remoteSource.getPaymentMethods(userId)
        if (remote.isNotEmpty()) {
            remoteSource.markSyncInitialized(userId)
            paymentMethodDao.replaceCustomMethods(userId, remote)
            return
        }

        val localCustomMethods = paymentMethodDao.getByUser(userId)
            .filter { it.builtInKey == null }
        if (!remoteSource.migrateLegacyMethods(userId, localCustomMethods)) {
            // An initialized but empty cloud collection means the user deleted all methods.
            paymentMethodDao.replaceCustomMethods(userId, emptyList())
        }
    }

    private suspend fun <T> runRemoteWrite(block: suspend () -> T): T = try {
        block()
    } catch (error: FirebaseFirestoreException) {
        val syncError = when (error.code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            FirebaseFirestoreException.Code.UNAUTHENTICATED -> PaymentMethodSyncError.PermissionDenied
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> PaymentMethodSyncError.Network
            else -> PaymentMethodSyncError.Unknown
        }
        throw PaymentMethodSyncException(syncError, error)
    }

    private fun defaultMethod(userId: String, builtInKey: String, now: Long): PaymentMethodEntity =
        PaymentMethodEntity(
            id = "$userId:$builtInKey",
            userId = userId,
            name = builtInKey,
            builtInKey = builtInKey,
            createdAtMillis = now,
            updatedAtMillis = now
        )
}
