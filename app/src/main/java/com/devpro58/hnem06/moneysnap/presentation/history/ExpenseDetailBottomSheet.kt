package com.devpro58.hnem06.moneysnap.presentation.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import coil.load
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.core.utils.DateLabelFormatter
import com.devpro58.hnem06.moneysnap.core.utils.MoneyFormatter
import com.devpro58.hnem06.moneysnap.core.utils.localizedLabel
import com.devpro58.hnem06.moneysnap.domain.model.Expense
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseCategory
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseSyncStatus
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

/**
 * Bottom sheet dialog that displays full expense details.
 * Matches the Material 3 design reference with category chip,
 * amount, title, info rows, and Delete/Edit action buttons.
 */
class ExpenseDetailBottomSheet : BottomSheetDialogFragment() {

    private var expense: Expense? = null
    private var onDelete: ((String) -> Unit)? = null
    private var onEdit: ((Expense) -> Unit)? = null
    private var onRetrySync: ((String) -> Unit)? = null

    override fun getTheme(): Int = R.style.App_BottomSheet_Transparent

    fun setOnRetrySyncListener(listener: (String) -> Unit): ExpenseDetailBottomSheet {
        this.onRetrySync = listener
        return this
    }

    fun setExpense(expense: Expense): ExpenseDetailBottomSheet {
        this.expense = expense
        return this
    }

    fun setOnDeleteListener(listener: (String) -> Unit): ExpenseDetailBottomSheet {
        this.onDelete = listener
        return this
    }

    fun setOnEditListener(listener: (Expense) -> Unit): ExpenseDetailBottomSheet {
        this.onEdit = listener
        return this
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_expense_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val exp = expense ?: run { dismiss(); return }

        // Category chip
        val categoryIcon = view.findViewById<ImageView>(R.id.detailCategoryIcon)
        val categoryLabel = view.findViewById<TextView>(R.id.detailCategoryLabel)
        categoryIcon.setImageResource(categoryIconRes(exp.category))
        categoryLabel.text = exp.category.localizedLabel(requireContext())

        // Amount
        view.findViewById<TextView>(R.id.detailAmount).text =
            MoneyFormatter.formatVnd(exp.amount)

        // Title / note
        val titleView = view.findViewById<TextView>(R.id.detailTitle)
        titleView.text = exp.title.ifBlank { exp.note ?: "" }
        if (titleView.text.isBlank()) titleView.visibility = View.GONE

        // Date & Time
        view.findViewById<TextView>(R.id.detailDateTime).text = formatDateTime(exp.spentAtMillis)

        // Payment Method
        val paymentText = view.findViewById<TextView>(R.id.detailPaymentMethod)
        paymentText.text = exp.paymentMethod?.ifBlank { null }
            ?: getString(R.string.detail_payment_cash)

        // Receipt section — show image inline using Coil (same as HomeFragment)
        val receiptSection = view.findViewById<LinearLayout>(R.id.detailReceiptSection)
        val receiptImage = view.findViewById<ImageView>(R.id.detailReceiptImage)
        val localFile = exp.localReceiptPath?.let { File(it) }?.takeIf { it.exists() }
        val receiptSource: Any? = localFile ?: exp.remoteReceiptUrl

        if (receiptSource != null) {
            receiptSection.visibility = View.VISIBLE
            receiptImage.load(receiptSource) {
                placeholder(R.drawable.ic_camera)
                error(R.drawable.ic_camera)
                fallback(R.drawable.ic_camera)
            }
        } else {
            receiptSection.visibility = View.GONE
        }

        bindSyncStatus(view, exp)

        // ---------- DELETE button ----------
        view.findViewById<MaterialButton>(R.id.btnDelete).setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_confirm_title)
                .setMessage(R.string.delete_confirm_message)
                .setNegativeButton(R.string.delete_confirm_cancel, null)
                .setPositiveButton(R.string.delete_confirm_ok) { _, _ ->
                    onDelete?.invoke(exp.id)
                    dismiss()
                }
                .show()
        }

        // ---------- EDIT button ----------
        view.findViewById<MaterialButton>(R.id.btnEdit).setOnClickListener {
            dismiss()
            onEdit?.invoke(exp)
        }
    }

    /**
     * Shows sync state only when there is something to say. `Retry` is offered exclusively for
     * [ExpenseSyncStatus.Failed]: a row that is merely queued will be picked up on its own, and
     * a button that re-queues an already-queued item just teaches the user it does nothing.
     */
    private fun bindSyncStatus(view: View, exp: Expense) {
        val section = view.findViewById<LinearLayout>(R.id.detailSyncSection)
        val statusText = view.findViewById<TextView>(R.id.detailSyncStatus)
        val retryButton = view.findViewById<MaterialButton>(R.id.btnRetrySync)

        val messageRes = when (exp.syncStatus) {
            ExpenseSyncStatus.Failed -> R.string.sync_status_failed
            ExpenseSyncStatus.PendingUpload,
            ExpenseSyncStatus.LocalOnly -> R.string.sync_status_pending
            // Synced needs no note; PendingDelete rows are filtered out before they reach the UI.
            ExpenseSyncStatus.Synced,
            ExpenseSyncStatus.PendingDelete -> null
        }

        if (messageRes == null) {
            section.visibility = View.GONE
            return
        }

        section.visibility = View.VISIBLE
        statusText.text = getString(messageRes)
        statusText.contentDescription = getString(R.string.cd_sync_pending)
        retryButton.visibility =
            if (exp.syncStatus == ExpenseSyncStatus.Failed) View.VISIBLE else View.GONE
        retryButton.setOnClickListener {
            onRetrySync?.invoke(exp.id)
            Toast.makeText(requireContext(), R.string.sync_retry_queued, Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun formatDateTime(millis: Long): String {
        val group = DateLabelFormatter.historyGroup(requireContext(), millis)
        val time = DateLabelFormatter.time(millis)
        return "$group, $time"
    }

    private fun categoryIconRes(category: ExpenseCategory): Int = when (category) {
        ExpenseCategory.Food -> R.drawable.ic_category_food
        ExpenseCategory.Transport -> R.drawable.ic_detail_payment
        ExpenseCategory.Shopping -> R.drawable.ic_detail_receipt
        ExpenseCategory.Entertainment -> R.drawable.ic_category_food
        ExpenseCategory.Bills -> R.drawable.ic_detail_calendar
        ExpenseCategory.Travel -> R.drawable.ic_detail_payment
        ExpenseCategory.Uncategorized -> R.drawable.ic_detail_receipt
        ExpenseCategory.Other -> R.drawable.ic_detail_receipt
    }

    companion object {
        const val TAG = "ExpenseDetailBottomSheet"
    }
}
