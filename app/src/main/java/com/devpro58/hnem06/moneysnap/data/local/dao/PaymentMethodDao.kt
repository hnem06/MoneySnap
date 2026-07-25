package com.devpro58.hnem06.moneysnap.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.devpro58.hnem06.moneysnap.data.local.entity.PaymentMethodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {

    @Query(
        """
        SELECT * FROM payment_methods
        WHERE userId = :userId
        ORDER BY builtInKey IS NULL,
            CASE builtInKey
                WHEN 'cash' THEN 0
                WHEN 'bank_transfer' THEN 1
                ELSE 2
            END,
            createdAtMillis ASC,
            name COLLATE NOCASE ASC
        """
    )
    fun observeByUser(userId: String): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_methods WHERE userId = :userId")
    suspend fun getByUser(userId: String): List<PaymentMethodEntity>

    @Query("SELECT * FROM payment_methods WHERE id = :methodId LIMIT 1")
    suspend fun getById(methodId: String): PaymentMethodEntity?

    @Upsert
    suspend fun upsert(method: PaymentMethodEntity)

    @Upsert
    suspend fun upsertAll(methods: List<PaymentMethodEntity>)

    @Query(
        """
        UPDATE payment_methods
        SET name = :name,
            updatedAtMillis = :updatedAtMillis
        WHERE id = :methodId AND builtInKey IS NULL
        """
    )
    suspend fun updateCustomName(
        methodId: String,
        name: String,
        updatedAtMillis: Long
    )

    @Query("DELETE FROM payment_methods WHERE id = :methodId AND builtInKey IS NULL")
    suspend fun deleteCustomById(methodId: String)

    @Query("DELETE FROM payment_methods WHERE userId = :userId AND builtInKey IS NULL")
    suspend fun deleteAllCustomByUser(userId: String)

    @Query(
        "DELETE FROM payment_methods WHERE userId = :userId " +
            "AND builtInKey IS NULL AND id NOT IN (:remoteIds)"
    )
    suspend fun deleteCustomNotIn(userId: String, remoteIds: List<String>)

    @Transaction
    suspend fun replaceCustomMethods(userId: String, methods: List<PaymentMethodEntity>) {
        if (methods.isEmpty()) {
            deleteAllCustomByUser(userId)
        } else {
            deleteCustomNotIn(userId, methods.map { it.id })
            upsertAll(methods)
        }
    }
}
