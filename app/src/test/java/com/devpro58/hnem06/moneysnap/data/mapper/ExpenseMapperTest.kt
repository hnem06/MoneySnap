package com.devpro58.hnem06.moneysnap.data.mapper

import com.devpro58.hnem06.moneysnap.data.local.entity.ExpenseEntity
import com.devpro58.hnem06.moneysnap.domain.model.Expense
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseCategory
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseSyncStatus
import com.devpro58.hnem06.moneysnap.domain.model.IncomeCategory
import com.devpro58.hnem06.moneysnap.domain.model.ReceiptUploadStatus
import com.devpro58.hnem06.moneysnap.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Expense and income share one `category` column, discriminated by `type`. These tests pin that
 * contract down — a mistake here mislabels a salary as spending, which inflates totals and fires
 * false over-budget alerts.
 */
class ExpenseMapperTest {

    private fun entity(
        category: String = "Food",
        type: String = TransactionType.Expense.name
    ) = ExpenseEntity(
        id = "exp-1",
        userId = "user-1",
        amount = 45_000L,
        currency = "VND",
        title = "Cà phê",
        category = category,
        paymentMethod = null,
        note = null,
        localReceiptPath = null,
        remoteReceiptUrl = null,
        receiptUploadStatus = ReceiptUploadStatus.None.name,
        syncStatus = ExpenseSyncStatus.Synced.name,
        spentAtMillis = 1_757_462_400_000L,
        createdAtMillis = 1L,
        updatedAtMillis = 1L,
        type = type
    )

    private fun domain(
        type: TransactionType = TransactionType.Expense,
        category: ExpenseCategory = ExpenseCategory.Food,
        incomeCategory: IncomeCategory? = null
    ) = Expense(
        id = "exp-1",
        userId = "user-1",
        amount = 45_000L,
        currency = "VND",
        title = "Cà phê",
        category = category,
        paymentMethod = null,
        note = null,
        localReceiptPath = null,
        remoteReceiptUrl = null,
        receiptUploadStatus = ReceiptUploadStatus.None,
        syncStatus = ExpenseSyncStatus.Synced,
        spentAtMillis = 1_757_462_400_000L,
        createdAtMillis = 1L,
        updatedAtMillis = 1L,
        type = type,
        incomeCategory = incomeCategory
    )

    @Test
    fun `a row with no type reads as an expense`() {
        // Matches the Room and Firestore defaults: nothing written before income existed was
        // anything other than spending.
        val expense = entity(type = TransactionType.Expense.name).toDomain()

        assertEquals(TransactionType.Expense, expense.type)
        assertEquals(ExpenseCategory.Food, expense.category)
        assertNull(expense.incomeCategory)
    }

    @Test
    fun `income reads its category from the shared column`() {
        val income = entity(category = "Salary", type = TransactionType.Income.name).toDomain()

        assertEquals(TransactionType.Income, income.type)
        assertEquals(IncomeCategory.Salary, income.incomeCategory)
        // Income must not surface as a real expense category, or it would appear in the
        // category breakdown on the Stats screen.
        assertEquals(ExpenseCategory.Uncategorized, income.category)
    }

    @Test
    fun `income writes its own category, not the expense one`() {
        val entity = domain(
            type = TransactionType.Income,
            category = ExpenseCategory.Food,
            incomeCategory = IncomeCategory.Bonus
        ).toEntity()

        assertEquals("Bonus", entity.category)
        assertEquals("Income", entity.type)
    }

    @Test
    fun `income missing a category falls back rather than writing an expense category`() {
        // Defensive: a caller that forgot to set incomeCategory must not silently persist "Food"
        // as an income category.
        val entity = domain(
            type = TransactionType.Income,
            category = ExpenseCategory.Food,
            incomeCategory = null
        ).toEntity()

        assertEquals(IncomeCategory.OtherIncome.name, entity.category)
    }

    @Test
    fun `an unrecognised income category degrades to OtherIncome`() {
        val income = entity(category = "NotARealThing", type = "Income").toDomain()
        assertEquals(IncomeCategory.OtherIncome, income.incomeCategory)
    }

    @Test
    fun `signed amount is negative for spending and positive for income`() {
        assertEquals(-45_000L, domain().signedAmount)
        assertEquals(
            45_000L,
            domain(type = TransactionType.Income, incomeCategory = IncomeCategory.Salary)
                .signedAmount
        )
        assertTrue(domain(type = TransactionType.Income).isIncome)
    }

    @Test
    fun `round trips an expense unchanged`() {
        val original = domain()
        assertEquals(original, original.toEntity().toDomain())
    }

    @Test
    fun `round trips income unchanged`() {
        val original = domain(
            type = TransactionType.Income,
            category = ExpenseCategory.Uncategorized,
            incomeCategory = IncomeCategory.Investment
        )
        assertEquals(original, original.toEntity().toDomain())
    }
}
