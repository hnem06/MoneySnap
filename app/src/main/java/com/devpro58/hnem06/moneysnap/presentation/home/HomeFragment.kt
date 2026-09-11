package com.devpro58.hnem06.moneysnap.presentation.home

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import coil.load
import com.devpro58.hnem06.moneysnap.core.utils.MonthlyTrend
import com.devpro58.hnem06.moneysnap.core.utils.TrendDirection
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.core.utils.MoneyFormatter
import com.devpro58.hnem06.moneysnap.core.utils.localizedLabel
import com.devpro58.hnem06.moneysnap.core.utils.setupTopAppBarNavigation
import com.devpro58.hnem06.moneysnap.databinding.FragmentHomeBinding
import com.devpro58.hnem06.moneysnap.domain.model.Expense
import com.devpro58.hnem06.moneysnap.presentation.history.EditExpenseBottomSheet
import com.devpro58.hnem06.moneysnap.presentation.history.ExpenseDetailBottomSheet
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import timber.log.Timber

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]
        setupTopAppBarNavigation(view)

        binding.addExpenseFab.setOnClickListener {
            findNavController().navigate(R.id.addExpenseFragment)
        }
        binding.seeAllRecentButton.setOnClickListener {
            findNavController().navigate(R.id.historyFragment)
        }
        binding.monthlyExpensesCard.setOnClickListener {
            findNavController().navigate(R.id.historyFragment)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is HomeUiState.Content -> renderContent(state)
                // Error was previously emitted and silently dropped, leaving the last-rendered
                // numbers on screen as if they were still current.
                is HomeUiState.Error -> {
                    Timber.e("Home dashboard failed: %s", state.message)
                    binding.homeEmptyText.text = getString(R.string.error_home_load_failed)
                    binding.homeEmptyText.visibility = View.VISIBLE
                    binding.recentSnapsContainer.removeAllViews()
                }
                HomeUiState.Loading -> Unit
            }
        }
        viewModel.actionResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            val message = when {
                result.action == HomeExpenseAction.Delete && result.result.isSuccess -> R.string.delete_success
                result.action == HomeExpenseAction.Delete -> R.string.delete_failed
                result.result.isSuccess -> R.string.edit_success
                else -> R.string.edit_failed
            }
            Toast.makeText(
                requireContext(),
                message,
                Toast.LENGTH_SHORT
            ).show()
            viewModel.consumeActionResult()
        }
    }

    private fun renderContent(state: HomeUiState.Content) {
        val dashboard = state.dashboard
        binding.monthlyTotalText.text = MoneyFormatter.formatVnd(dashboard.monthlyTotal)
        binding.todayTotalText.text = MoneyFormatter.formatVnd(dashboard.todayTotal)
        binding.monthlyExpenseCountText.text = dashboard.monthlyExpenseCount.toString()
        // Reset the text as well as the visibility: the error branch reuses this view, and
        // without this a recovered load would still be showing the failure message.
        binding.homeEmptyText.setText(R.string.home_empty)
        binding.homeEmptyText.visibility =
            if (dashboard.recentExpenses.isEmpty()) View.VISIBLE else View.GONE

        bindMonthlyTrend(dashboard.monthlyTotal, dashboard.previousMonthTotal)

        binding.recentSnapsContainer.removeAllViews()
        dashboard.recentExpenses.forEach { expense ->
            binding.recentSnapsContainer.addView(createExpenseCard(expense))
        }
    }

    /**
     * Fills in `monthlyTrendText`, which was designed and laid out but never computed — it sat
     * `visibility="gone"` with zero code references. Spending more than last month is shown in
     * the danger colour, less in the success colour; on a user's first month there is nothing to
     * compare against, so the row stays hidden rather than claiming a meaningless +100%.
     */
    private fun bindMonthlyTrend(currentTotal: Long, previousTotal: Long) {
        val trend = MonthlyTrend.of(currentTotal, previousTotal)
        val percent = trend.percent

        if (trend.direction == TrendDirection.Unknown || percent == null) {
            binding.monthlyTrendText.visibility = View.GONE
            return
        }

        binding.monthlyTrendText.visibility = View.VISIBLE
        binding.monthlyTrendText.text = when (trend.direction) {
            TrendDirection.Up -> getString(R.string.trend_up, percent)
            TrendDirection.Down -> getString(R.string.trend_down, percent)
            else -> getString(R.string.trend_flat)
        }
        binding.monthlyTrendText.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                when (trend.direction) {
                    TrendDirection.Up -> R.color.danger_red
                    TrendDirection.Down -> R.color.success_green
                    else -> R.color.text_muted
                }
            )
        )
    }

    private fun createExpenseCard(expense: Expense): View {
        val availableWidth = resources.displayMetrics.widthPixels - dp(40) - dp(12)
        val cardWidth = availableWidth / 2
        val card = MaterialCardView(requireContext()).apply {
            radius = dp(20).toFloat()
            cardElevation = dp(1).toFloat()
            strokeWidth = dp(1)
            strokeColor = color(R.color.border_gray)
            setCardBackgroundColor(color(R.color.card_bg))
            isClickable = true
            isFocusable = true
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(3), dp(3), dp(3), dp(9))
            }
            setOnClickListener { showExpenseDetail(expense) }
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> animateScale(view, 0.96f)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> animateScale(view, 1f)
                }
                false
            }
        }

        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }
        val imageFrame = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                cardWidth
            )
            setBackgroundColor(color(R.color.icon_square_bg))
        }
        val receiptImage = ImageView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = expense.title
            val localFile = expense.localReceiptPath?.let(::File)?.takeIf(File::exists)
            load(localFile ?: expense.remoteReceiptUrl) {
                placeholder(R.drawable.ic_camera)
                error(R.drawable.ic_camera)
                fallback(R.drawable.ic_camera)
            }
        }
        imageFrame.addView(receiptImage)
        imageFrame.addView(createCategoryBadge(expense, cardWidth))

        val details = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(11), dp(12), dp(13))
            addView(TextView(requireContext()).apply {
                text = expense.title
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                textSize = 13f
                setTextColor(color(R.color.text_dark))
                setTypeface(typeface, Typeface.BOLD)
            })
            addView(TextView(requireContext()).apply {
                text = MoneyFormatter.formatVnd(expense.amount)
                textSize = 17f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setTextColor(color(R.color.secondary_green))
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(5) }
            })
        }
        content.addView(imageFrame)
        content.addView(details)
        card.addView(content)
        return card
    }

    private fun createCategoryBadge(expense: Expense, cardWidth: Int) = TextView(requireContext()).apply {
        layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            dp(26),
            Gravity.TOP or Gravity.END
        ).apply {
            topMargin = dp(8)
            marginEnd = dp(8)
        }
        gravity = Gravity.CENTER
        setPadding(dp(10), 0, dp(10), 0)
        text = expense.category.localizedLabel(requireContext())
        textSize = 10f
        maxLines = 1
        maxWidth = cardWidth - dp(16)
        ellipsize = TextUtils.TruncateAt.END
        setTextColor(color(R.color.icon_neutral))
        setTypeface(typeface, Typeface.BOLD)
        background = GradientDrawable().apply {
            cornerRadius = dp(13).toFloat()
            setColor(Color.argb(232, 255, 255, 255))
        }
    }

    private fun showExpenseDetail(expense: Expense) {
        ExpenseDetailBottomSheet()
            .setExpense(expense)
            .setOnDeleteListener(viewModel::deleteExpense)
            .setOnEditListener(::showEditSheet)
            .setOnRetrySyncListener(viewModel::retrySync)
            .show(childFragmentManager, ExpenseDetailBottomSheet.TAG)
    }

    private fun showEditSheet(expense: Expense) {
        EditExpenseBottomSheet.newInstance(expense)
            .setPaymentMethods(viewModel.paymentMethods.value.orEmpty())
            .setOnSaveListener(viewModel::updateExpense)
            .show(childFragmentManager, EditExpenseBottomSheet.TAG)
    }

    private fun animateScale(view: View, scale: Float) {
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, View.SCALE_X, scale),
                ObjectAnimator.ofFloat(view, View.SCALE_Y, scale)
            )
            duration = 120
            start()
        }
    }

    private fun color(resId: Int) = ContextCompat.getColor(requireContext(), resId)
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
