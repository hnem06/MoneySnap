package com.devpro58.hnem06.moneysnap.presentation.expense.add

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.core.utils.localizedName
import com.devpro58.hnem06.moneysnap.core.utils.setupTopAppBarNavigation
import com.devpro58.hnem06.moneysnap.core.utils.VndAmountFormatter
import com.devpro58.hnem06.moneysnap.core.utils.enableVndAmountFormatting
import com.devpro58.hnem06.moneysnap.databinding.FragmentAddExpenseBinding
import com.devpro58.hnem06.moneysnap.domain.model.ExpenseCategory
import com.devpro58.hnem06.moneysnap.domain.model.PaymentMethod
import com.devpro58.hnem06.moneysnap.domain.model.PaymentMethodSyncError
import com.devpro58.hnem06.moneysnap.domain.model.PaymentMethodSyncException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class AddExpenseFragment : Fragment() {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AddExpenseViewModel

    private var selectedReceiptUri: Uri? = null
    private var cameraReceiptUri: Uri? = null
    private var selectedCategory = ExpenseCategory.Food
    private var paymentMethods: List<PaymentMethod> = emptyList()
    private var selectedPaymentMethod: PaymentMethod? = null
    private var pendingPaymentMethodSelectionId: String? = null
    private var selectedSpentAtMillis: Long = System.currentTimeMillis()
    private var openedFromHistory = false

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) updateReceiptPreview(uri)
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraReceiptUri?.let { updateReceiptPreview(it) }
        }
    }

    /**
     * CAMERA is declared in the manifest, so ACTION_IMAGE_CAPTURE throws SecurityException when
     * it has not been granted. It used to be requested once on the Welcome screen — which ignored
     * the result and is never shown to returning users — so any user who declined, or who simply
     * installed before that screen existed, crashed on "Take photo".
     */
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCamera()
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            Toast.makeText(requireContext(), R.string.camera_permission_denied, Toast.LENGTH_LONG)
                .show()
        } else {
            // Permanently denied — the system dialog will not appear again, so offer Settings.
            showCameraPermissionSettingsDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[AddExpenseViewModel::class.java]
        setupTopAppBarNavigation(view)
        openedFromHistory = arguments
            ?.getLong(ARG_SPENT_AT_MILLIS, NO_SELECTED_DATE)
            ?.let { it != NO_SELECTED_DATE }
            ?: false
        if (openedFromHistory) {
            setupHistoryBackNavigation()
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                returnToHistory()
            }
        }
        binding.amountEditText.enableVndAmountFormatting()
        selectedSpentAtMillis = arguments
            ?.getLong(ARG_SPENT_AT_MILLIS, NO_SELECTED_DATE)
            ?.takeIf { it != NO_SELECTED_DATE }
            ?: System.currentTimeMillis()
        updateSelectedDateLabel()
        binding.expenseDateInput.setOnClickListener { showExpenseDatePicker() }
        setupCategoryChips()
        setupPaymentMethodDropdown()

        binding.receiptPickerContainer.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        binding.pickImageButton.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        binding.takePhotoButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                launchCamera()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
        binding.saveExpenseButton.setOnClickListener {
            submitExpense()
        }

        binding.addPaymentMethodButton.setOnClickListener {
            showPaymentMethodInputDialog()
        }

        viewModel.paymentMethods.observe(viewLifecycleOwner) { methods ->
            bindPaymentMethods(methods)
        }
        viewModel.receiptScanState.observe(viewLifecycleOwner) { state ->
            binding.receiptScanProgress.visibility =
                if (state == ReceiptScanUiState.Scanning) View.VISIBLE else View.GONE
            binding.pickImageButton.isEnabled = state != ReceiptScanUiState.Scanning
            binding.takePhotoButton.isEnabled = state != ReceiptScanUiState.Scanning
            when (state) {
                is ReceiptScanUiState.Recognized -> {
                    applyReceiptDraft(state.draft)
                    Toast.makeText(requireContext(), R.string.receipt_scan_applied, Toast.LENGTH_SHORT).show()
                    viewModel.consumeReceiptScanState()
                }
                ReceiptScanUiState.NoUsefulData -> {
                    Toast.makeText(requireContext(), R.string.receipt_scan_no_data, Toast.LENGTH_SHORT).show()
                    viewModel.consumeReceiptScanState()
                }
                ReceiptScanUiState.Error -> {
                    Toast.makeText(requireContext(), R.string.receipt_scan_failed, Toast.LENGTH_SHORT).show()
                    viewModel.consumeReceiptScanState()
                }
                ReceiptScanUiState.Idle,
                ReceiptScanUiState.Scanning -> Unit
            }
        }
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.saveExpenseButton.isEnabled = state !is AddExpenseUiState.Saving
            when (state) {
                AddExpenseUiState.Saved -> {
                    Toast.makeText(requireContext(), R.string.add_saved, Toast.LENGTH_LONG).show()
                    viewModel.resetState()
                    if (openedFromHistory) {
                        returnToHistory()
                    } else {
                        findNavController().navigate(R.id.homeFragment)
                    }
                }
                is AddExpenseUiState.Error -> {
                    val message = state.message.ifBlank {
                        getString(R.string.error_save_expense_failed)
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                    viewModel.resetState()
                }
                AddExpenseUiState.Idle,
                AddExpenseUiState.Saving -> Unit
            }
        }

        viewModel.paymentMethodResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            result.onSuccess { method ->
                pendingPaymentMethodSelectionId = method.id
                selectedPaymentMethod = method
                binding.paymentMethodDropdown.setText(method.localizedName(requireContext()), false)
                Toast.makeText(
                    requireContext(),
                    R.string.payment_methods_saved,
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure {
                val message = when ((it as? PaymentMethodSyncException)?.error) {
                    PaymentMethodSyncError.PermissionDenied -> R.string.payment_methods_permission_error
                    PaymentMethodSyncError.Network -> R.string.payment_methods_network_error
                    else -> R.string.payment_methods_error
                }
                Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            viewModel.consumePaymentMethodResult()
        }
    }

    private fun setupHistoryBackNavigation() {
        binding.topAppBar.apply {
            setNavigationIcon(R.drawable.ic_arrow_back)
            navigationContentDescription = getString(R.string.cd_back_to_history)
            setNavigationOnClickListener { returnToHistory() }
        }
    }

    private fun returnToHistory() {
        val navController = findNavController()
        if (navController.currentDestination?.id == R.id.historyFragment) return
        val returnedToExistingHistory = navController.popBackStack(
            R.id.historyFragment,
            false
        )
        if (!returnedToExistingHistory) {
            navController.navigate(R.id.historyFragment)
        }
    }

    private fun setupCategoryChips() {
        binding.categoryChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            selectedCategory = when (checkedIds.firstOrNull()) {
                R.id.categoryTransportChip -> ExpenseCategory.Transport
                R.id.categoryShoppingChip -> ExpenseCategory.Shopping
                R.id.categoryEntertainmentChip -> ExpenseCategory.Entertainment
                R.id.categoryBillsChip -> ExpenseCategory.Bills
                R.id.categoryOtherChip -> ExpenseCategory.Other
                else -> ExpenseCategory.Food
            }
        }
    }

    private fun setupPaymentMethodDropdown() {
        binding.paymentMethodDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedPaymentMethod = paymentMethods.getOrNull(position)
        }
    }

    private fun bindPaymentMethods(methods: List<PaymentMethod>) {
        paymentMethods = methods
        val labels = methods.map { it.localizedName(requireContext()) }
        binding.paymentMethodDropdown.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, labels)
        )

        val methodToSelect = pendingPaymentMethodSelectionId
            ?.let { id -> methods.firstOrNull { it.id == id } }
            ?: selectedPaymentMethod?.let { selected ->
                methods.firstOrNull { it.id == selected.id }
            }
            ?: methods.firstOrNull()

        selectedPaymentMethod = methodToSelect
        pendingPaymentMethodSelectionId = null

        binding.paymentMethodDropdown.setText(
            methodToSelect?.localizedName(requireContext()).orEmpty(),
            false
        )
    }

    private fun showPaymentMethodInputDialog() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.payment_methods_name_hint)
            setSingleLine(true)
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.payment_methods_add_title)
            .setView(input)
            .setNegativeButton(R.string.payment_methods_cancel, null)
            .setPositiveButton(R.string.payment_methods_save) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isBlank()) {
                    Toast.makeText(
                        requireContext(),
                        R.string.payment_methods_name_empty,
                        Toast.LENGTH_SHORT
                    ).show()
                } else if (paymentMethods.any { it.localizedName(requireContext()).equals(name, ignoreCase = true) }) {
                    Toast.makeText(
                        requireContext(),
                        R.string.payment_methods_duplicate,
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    viewModel.addPaymentMethod(name)
                }
            }
            .show()
    }

    private fun submitExpense() {
        val amountText = binding.amountEditText.text.toString().trim()
        val title = binding.titleEditText.text.toString().trim()

        if (amountText.isBlank()) {
            binding.amountEditText.error = getString(R.string.error_amount_empty)
            binding.amountEditText.requestFocus()
            return
        }
        if (title.isBlank()) {
            binding.titleEditText.error = getString(R.string.error_title_empty)
            binding.titleEditText.requestFocus()
            return
        }

        val amount = VndAmountFormatter.parse(amountText)
        if (amount == null || amount <= 0L) {
            binding.amountEditText.error = getString(R.string.error_amount_empty)
            binding.amountEditText.requestFocus()
            return
        }

        viewModel.saveExpense(
            amount = amount,
            title = title,
            category = selectedCategory,
            paymentMethod = selectedPaymentMethod?.localizedName(requireContext())
                ?: binding.paymentMethodDropdown.text.toString().trim(),
            note = binding.noteEditText.text.toString().trim(),
            receiptSourceUri = selectedReceiptUri?.toString(),
            spentAtMillis = selectedSpentAtMillis
        )
    }

    private fun showExpenseDatePicker() {
        val selected = Calendar.getInstance().apply { timeInMillis = selectedSpentAtMillis }
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                selected.set(Calendar.YEAR, year)
                selected.set(Calendar.MONTH, month)
                selected.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                selectedSpentAtMillis = selected.timeInMillis
                updateSelectedDateLabel()
            },
            selected.get(Calendar.YEAR),
            selected.get(Calendar.MONTH),
            selected.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateSelectedDateLabel() {
        binding.expenseDateInput.text = SimpleDateFormat(
            "EEEE, dd/MM/yyyy",
            Locale.getDefault()
        ).format(selectedSpentAtMillis)
    }

    private fun updateReceiptPreview(uri: Uri) {
        selectedReceiptUri = uri
        binding.receiptPreview.setImageURI(uri)
        binding.receiptHintContainer.visibility = View.GONE
        viewModel.scanReceipt(uri.toString())
    }

    private fun applyReceiptDraft(draft: com.devpro58.hnem06.moneysnap.domain.model.ReceiptDraft) {
        draft.totalAmount?.let { amount ->
            binding.amountEditText.setText(VndAmountFormatter.format(amount))
            binding.amountEditText.setSelection(binding.amountEditText.text.length)
        }
        draft.spentAtMillis?.let { timestamp ->
            selectedSpentAtMillis = timestamp
            updateSelectedDateLabel()
        }
        draft.suggestedCategory?.let(::selectCategory)
    }

    private fun selectCategory(category: ExpenseCategory) {
        val chipId = when (category) {
            ExpenseCategory.Food -> R.id.categoryFoodChip
            ExpenseCategory.Transport -> R.id.categoryTransportChip
            ExpenseCategory.Shopping -> R.id.categoryShoppingChip
            ExpenseCategory.Entertainment -> R.id.categoryEntertainmentChip
            ExpenseCategory.Bills -> R.id.categoryBillsChip
            ExpenseCategory.Travel,
            ExpenseCategory.Uncategorized,
            ExpenseCategory.Other -> R.id.categoryOtherChip
        }
        binding.categoryChipGroup.check(chipId)
    }

    private fun launchCamera() {
        val uri = createCameraReceiptUri()
        cameraReceiptUri = uri
        takePictureLauncher.launch(uri)
    }

    private fun showCameraPermissionSettingsDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.camera_permission_rationale_title)
            .setMessage(R.string.camera_permission_rationale_message)
            .setNegativeButton(R.string.camera_permission_cancel, null)
            .setPositiveButton(R.string.camera_permission_open_settings) { _, _ ->
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", requireContext().packageName, null)
                    )
                )
            }
            .show()
    }

    private fun createCameraReceiptUri(): Uri {
        val directory = File(requireContext().cacheDir, "receipt_camera").apply { mkdirs() }
        val file = File(directory, "receipt_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_SPENT_AT_MILLIS = "spentAtMillis"
        private const val NO_SELECTED_DATE = -1L
    }
}
