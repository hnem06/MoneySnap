package com.devpro58.hnem06.moneysnap.presentation.budget

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.core.utils.VndAmountFormatter
import com.devpro58.hnem06.moneysnap.core.utils.setupTopAppBarNavigation
import com.devpro58.hnem06.moneysnap.databinding.FragmentBudgetBinding
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BudgetViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        viewModel.save()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTopAppBarNavigation(view)
        view.findViewById<MaterialToolbar>(R.id.topAppBar).apply {
            setNavigationIcon(R.drawable.ic_arrow_back)
            navigationContentDescription = getString(R.string.cd_back_to_profile)
            setNavigationOnClickListener { findNavController().popBackStack() }
        }
        binding.budgetMonthText.text = getString(
            R.string.budget_month_value,
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
        )

        binding.budgetSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    viewModel.updateBudget((progress + 1L) * BudgetViewModel.STEP)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        binding.saveBudgetButton.setOnClickListener { saveBudgetWithNotificationPermission() }
        binding.skipBudgetButton.setOnClickListener { findNavController().popBackStack() }

        viewModel.budget.observe(viewLifecycleOwner) { amount ->
            binding.budgetAmountText.text = VndAmountFormatter.format(amount)
            val progress = (amount / BudgetViewModel.STEP - 1L).toInt()
                .coerceIn(0, binding.budgetSeekBar.max)
            if (binding.budgetSeekBar.progress != progress) {
                binding.budgetSeekBar.progress = progress
            }
        }
        viewModel.saved.observe(viewLifecycleOwner) { saved ->
            if (!saved) return@observe
            Toast.makeText(requireContext(), R.string.budget_saved, Toast.LENGTH_SHORT).show()
            viewModel.consumeSaved()
            findNavController().popBackStack()
        }
    }

    private fun saveBudgetWithNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.save()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
