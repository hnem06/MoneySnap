package com.devpro58.hnem06.moneysnap.presentation.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import coil.load
import com.devpro58.hnem06.moneysnap.R
import com.devpro58.hnem06.moneysnap.core.utils.MoneyFormatter
import com.devpro58.hnem06.moneysnap.core.utils.localizedName
import com.devpro58.hnem06.moneysnap.domain.model.AuthUser
import com.devpro58.hnem06.moneysnap.domain.model.PaymentMethod
import com.devpro58.hnem06.moneysnap.domain.usecase.onboarding.GetLanguageCodeUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.onboarding.SetLanguageCodeUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.settings.GetDarkModeUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.settings.GetMonthlyBudgetUseCase
import com.devpro58.hnem06.moneysnap.domain.usecase.settings.SetDarkModeUseCase
import com.devpro58.hnem06.moneysnap.presentation.auth.AuthScreen
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.materialswitch.MaterialSwitch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    @Inject lateinit var getLanguageCode: GetLanguageCodeUseCase
    @Inject lateinit var setLanguageCode: SetLanguageCodeUseCase
    @Inject lateinit var getDarkMode: GetDarkModeUseCase
    @Inject lateinit var setDarkMode: SetDarkModeUseCase
    @Inject lateinit var getMonthlyBudget: GetMonthlyBudgetUseCase

    private lateinit var viewModel: ProfileViewModel
    private var paymentMethods: List<PaymentMethod> = emptyList()
    private var paymentMethodsSheetView: View? = null
    private var currentUser: AuthUser? = null

    private val pickAvatarLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.changeAvatar(it.toString()) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        // Account header (name, email, avatar) — driven by accountState
        viewModel.accountState.observe(viewLifecycleOwner) { state ->
            currentUser = state.user
            bindAccountHeader(view, state.user)

            state.message?.let { messageRes ->
                Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
                viewModel.consumeAccountMessage()
            }
        }

        // Avatar actions
        val avatarClickListener = View.OnClickListener { showAvatarOptions() }
        view.findViewById<View>(R.id.profileAvatarLarge).setOnClickListener(avatarClickListener)
        view.findViewById<View>(R.id.btnEditAvatar).setOnClickListener(avatarClickListener)

        // Account Settings rows
        setupRow(view.findViewById(R.id.rowPersonalInfo),
            R.drawable.ic_profile_person, getString(R.string.profile_personal_info))
        view.findViewById<View>(R.id.rowPersonalInfo).setOnClickListener {
            showPersonalInfoDialog()
        }
        setupRow(view.findViewById(R.id.rowPaymentMethods),
            R.drawable.ic_detail_payment, getString(R.string.profile_payment_methods),
            subtitle = getString(R.string.payment_methods_profile_subtitle))
        view.findViewById<View>(R.id.rowPaymentMethods).setOnClickListener {
            showPaymentMethodsDialog()
        }
        setupRow(view.findViewById(R.id.rowSecurity),
            R.drawable.ic_profile_security, getString(R.string.profile_security))

        // App Settings rows
        setupRow(view.findViewById(R.id.rowCurrency),
            R.drawable.ic_profile_currency, getString(R.string.profile_currency),
            value = "VND")
        bindMonthlyBudgetRow(view)
        view.findViewById<View>(R.id.rowMonthlyBudget).setOnClickListener {
            findNavController().navigate(R.id.budgetFragment)
        }
        setupRow(view.findViewById(R.id.rowLanguage),
            R.drawable.ic_profile_language, getString(R.string.profile_language),
            value = getLanguageLabel(currentLanguageCode()))
        view.findViewById<View>(R.id.rowLanguage).setOnClickListener {
            showLanguageDialog()
        }

        // Dark mode: reflect the stored preference; toggling persists it and
        // re-applies night mode (activities recreate with the new palette)
        val darkModeSwitch = view.findViewById<MaterialSwitch>(R.id.switchDarkMode)
        darkModeSwitch.isChecked = getDarkMode()
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked == getDarkMode()) return@setOnCheckedChangeListener
            setDarkMode(isChecked)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Support & Legal rows
        setupRow(view.findViewById(R.id.rowHelpCenter),
            R.drawable.ic_profile_info, getString(R.string.profile_help_center))
        setupRow(view.findViewById(R.id.rowPrivacyPolicy),
            R.drawable.ic_profile_privacy, getString(R.string.profile_privacy_policy))
        setupRow(view.findViewById(R.id.rowAbout),
            R.drawable.ic_profile_info, getString(R.string.profile_about))

        // Logout
        view.findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_logout_confirm_title)
                .setMessage(R.string.profile_logout_confirm_message)
                .setNegativeButton(R.string.profile_logout_cancel, null)
                .setPositiveButton(R.string.profile_logout_ok) { _, _ ->
                    viewModel.signOut()
                }
                .show()
        }

        viewModel.signedOut.observe(viewLifecycleOwner) { signedOut ->
            if (!signedOut) return@observe
            val intent = Intent(requireContext(), AuthScreen::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        viewModel.paymentMethods.observe(viewLifecycleOwner) { methods ->
            paymentMethods = methods
            updatePaymentMethodsRow(view, methods)
            paymentMethodsSheetView?.let { bindPaymentMethodsSheet(it) }
        }

        viewModel.paymentMethodAction.observe(viewLifecycleOwner) { action ->
            action ?: return@observe
            val messageRes = when (action) {
                PaymentMethodAction.Saved -> R.string.payment_methods_saved
                PaymentMethodAction.Deleted -> R.string.payment_methods_deleted
                PaymentMethodAction.DefaultLocked -> R.string.payment_methods_default_locked
                PaymentMethodAction.PermissionDenied -> R.string.payment_methods_permission_error
                PaymentMethodAction.NetworkError -> R.string.payment_methods_network_error
                PaymentMethodAction.Error -> R.string.payment_methods_error
            }
            Toast.makeText(requireContext(), messageRes, Toast.LENGTH_SHORT).show()
            viewModel.consumePaymentMethodAction()
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let(::bindMonthlyBudgetRow)
    }

    private fun bindMonthlyBudgetRow(view: View) {
        setupRow(
            rowView = view.findViewById(R.id.rowMonthlyBudget),
            iconRes = R.drawable.ic_wallet,
            title = getString(R.string.profile_monthly_budget),
            subtitle = getString(R.string.profile_monthly_budget_subtitle),
            value = MoneyFormatter.formatVnd(getMonthlyBudget())
        )
    }

    private fun bindAccountHeader(view: View, user: AuthUser?) {
        view.findViewById<TextView>(R.id.profileUserName).text =
            user?.displayName ?: getString(R.string.profile_default_user)
        view.findViewById<TextView>(R.id.profileUserEmail).text = user?.email ?: ""

        val avatar = view.findViewById<ShapeableImageView>(R.id.profileAvatarLarge)
        if (user?.photoUrl != null) {
            avatar.setPadding(0, 0, 0, 0)
            avatar.load(user.photoUrl) {
                placeholder(R.drawable.user_profile_placeholder)
                error(R.drawable.user_profile_placeholder)
            }
        } else {
            val pad = dp(12)
            avatar.setPadding(pad, pad, pad, pad)
            avatar.setImageResource(R.drawable.user_profile_placeholder)
        }
    }

    private fun showAvatarOptions() {
        if (viewModel.accountState.value?.busy == true) return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.avatar_options_title)
            .setItems(
                arrayOf(
                    getString(R.string.avatar_view),
                    getString(R.string.avatar_change),
                    getString(R.string.avatar_remove)
                )
            ) { _, which ->
                when (which) {
                    0 -> showAvatarPreview()
                    1 -> pickAvatarLauncher.launch("image/*")
                    2 -> confirmRemoveAvatar()
                }
            }
            .show()
    }

    private fun showAvatarPreview() {
        val photoUrl = currentUser?.photoUrl
        if (photoUrl == null) {
            Toast.makeText(requireContext(), R.string.avatar_none, Toast.LENGTH_SHORT).show()
            return
        }

        val previewView = layoutInflater.inflate(R.layout.dialog_avatar_preview, null)
        previewView.findViewById<ImageView>(R.id.avatarPreviewImage).load(photoUrl)

        MaterialAlertDialogBuilder(requireContext())
            .setView(previewView)
            .setPositiveButton(R.string.payment_methods_close, null)
            .show()
    }

    private fun confirmRemoveAvatar() {
        if (currentUser?.photoUrl == null) {
            Toast.makeText(requireContext(), R.string.avatar_none, Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.avatar_remove_confirm_title)
            .setMessage(R.string.avatar_remove_confirm_message)
            .setNegativeButton(R.string.payment_methods_cancel, null)
            .setPositiveButton(R.string.avatar_remove_confirm_ok) { _, _ ->
                viewModel.deleteAvatar()
            }
            .show()
    }

    private fun showPersonalInfoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_personal_info, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.personalInfoNameInput)
        val emailText = dialogView.findViewById<TextView>(R.id.personalInfoEmailText)

        nameInput.setText(currentUser?.displayName.orEmpty())
        nameInput.setSelection(nameInput.text.length)
        emailText.text = currentUser?.email ?: "—"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_personal_info)
            .setView(dialogView)
            .setNegativeButton(R.string.payment_methods_cancel, null)
            .setPositiveButton(R.string.personal_info_save) { _, _ ->
                val newName = nameInput.text.toString().trim()
                when {
                    newName.isBlank() ->
                        Toast.makeText(requireContext(), R.string.error_name_empty, Toast.LENGTH_SHORT).show()
                    newName != currentUser?.displayName ->
                        viewModel.changeDisplayName(newName)
                }
            }
            .show()
    }

    private fun updatePaymentMethodsRow(view: View, methods: List<PaymentMethod>) {
        val subtitle = methods
            .take(3)
            .joinToString(separator = ", ") { it.localizedName(requireContext()) }
            .ifBlank { getString(R.string.payment_methods_profile_subtitle) }

        setupRow(
            rowView = view.findViewById(R.id.rowPaymentMethods),
            iconRes = R.drawable.ic_detail_payment,
            title = getString(R.string.profile_payment_methods),
            subtitle = subtitle
        )
    }

    private fun showPaymentMethodsDialog() {
        val sheet = BottomSheetDialog(requireContext(), R.style.App_BottomSheet_Transparent)
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_payment_methods, null)
        paymentMethodsSheetView = sheetView

        sheetView.findViewById<MaterialButton>(R.id.paymentMethodSheetAddButton)
            .setOnClickListener { showPaymentMethodInputDialog() }
        bindPaymentMethodsSheet(sheetView)

        sheet.setContentView(sheetView)
        sheet.setOnDismissListener {
            if (paymentMethodsSheetView == sheetView) {
                paymentMethodsSheetView = null
            }
        }
        sheet.show()
    }

    private fun bindPaymentMethodsSheet(sheetView: View) {
        val container = sheetView.findViewById<LinearLayout>(R.id.paymentMethodsContainer)
        container.removeAllViews()

        paymentMethods.forEach { method ->
            val itemView = layoutInflater.inflate(R.layout.item_payment_method, container, false)
            itemView.findViewById<TextView>(R.id.paymentMethodName).text =
                method.localizedName(requireContext())

            val badge = itemView.findViewById<TextView>(R.id.paymentMethodBadge)
            val editButton = itemView.findViewById<MaterialButton>(R.id.paymentMethodEditButton)
            val deleteButton = itemView.findViewById<MaterialButton>(R.id.paymentMethodDeleteButton)

            if (method.isBuiltIn) {
                badge.visibility = View.VISIBLE
                editButton.visibility = View.GONE
                deleteButton.visibility = View.GONE
                itemView.setOnClickListener {
                    Toast.makeText(
                        requireContext(),
                        R.string.payment_methods_default_locked,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                badge.visibility = View.GONE
                editButton.visibility = View.VISIBLE
                deleteButton.visibility = View.VISIBLE
                itemView.setOnClickListener { showPaymentMethodInputDialog(method) }
                editButton.setOnClickListener { showPaymentMethodInputDialog(method) }
                deleteButton.setOnClickListener { confirmDeletePaymentMethod(method) }
            }

            container.addView(itemView)
        }
    }

    private fun showPaymentMethodInputDialog(method: PaymentMethod? = null) {
        if (method?.isBuiltIn == true) {
            Toast.makeText(
                requireContext(),
                R.string.payment_methods_default_locked,
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val input = EditText(requireContext()).apply {
            hint = getString(R.string.payment_methods_name_hint)
            setText(method?.name.orEmpty())
            setSingleLine(true)
            setPadding(dp(16), dp(8), dp(16), dp(8))
            if (text.isNotEmpty()) {
                setSelection(text.length)
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(
                if (method == null) {
                    R.string.payment_methods_add_title
                } else {
                    R.string.payment_methods_edit_title
                }
            )
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
                    return@setPositiveButton
                }
                if (hasDuplicatePaymentMethodName(name, method)) {
                    Toast.makeText(
                        requireContext(),
                        R.string.payment_methods_duplicate,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setPositiveButton
                }

                if (method == null) {
                    viewModel.addPaymentMethod(name)
                } else {
                    viewModel.updatePaymentMethod(method, name)
                }
            }
            .show()
    }

    private fun hasDuplicatePaymentMethodName(
        name: String,
        currentMethod: PaymentMethod?
    ): Boolean =
        paymentMethods.any { method ->
            method.id != currentMethod?.id &&
                method.localizedName(requireContext()).equals(name, ignoreCase = true)
        }

    private fun confirmDeletePaymentMethod(method: PaymentMethod) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.payment_methods_delete_title)
            .setMessage(
                getString(
                    R.string.payment_methods_delete_message,
                    method.localizedName(requireContext())
                )
            )
            .setNegativeButton(R.string.payment_methods_cancel, null)
            .setPositiveButton(R.string.payment_methods_delete) { _, _ ->
                viewModel.deletePaymentMethod(method)
            }
            .show()
    }

    private fun showLanguageDialog() {
        val currentCode = currentLanguageCode()
        val checkedIndex = LANGUAGE_OPTIONS.indexOfFirst { it.code == currentCode }
            .coerceAtLeast(0)
        val labels = LANGUAGE_OPTIONS
            .map { getString(it.labelRes) }
            .toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.profile_language_dialog_title)
            .setSingleChoiceItems(labels, checkedIndex) { dialog, which ->
                val selectedCode = LANGUAGE_OPTIONS[which].code
                if (selectedCode != currentCode) {
                    setLanguageCode(selectedCode)
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(selectedCode)
                    )
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun currentLanguageCode(): String {
        val supportedCodes = LANGUAGE_OPTIONS.map { it.code }
        val storedCode = getLanguageCode()
        if (storedCode != null && storedCode in supportedCodes) return storedCode

        val appLocaleCode = AppCompatDelegate.getApplicationLocales()
            .get(0)
            ?.language
        return appLocaleCode?.takeIf { it in supportedCodes } ?: DEFAULT_LANGUAGE_CODE
    }

    private fun getLanguageLabel(languageCode: String): String {
        val option = LANGUAGE_OPTIONS.firstOrNull { it.code == languageCode }
            ?: LANGUAGE_OPTIONS.first()
        return getString(option.labelRes)
    }

    private fun setupRow(
        rowView: View,
        iconRes: Int,
        title: String,
        subtitle: String? = null,
        value: String? = null
    ) {
        rowView.findViewById<ImageView>(R.id.rowIcon).setImageResource(iconRes)
        rowView.findViewById<TextView>(R.id.rowTitle).text = title

        val subtitleView = rowView.findViewById<TextView>(R.id.rowSubtitle)
        if (subtitle != null) {
            subtitleView.text = subtitle
            subtitleView.visibility = View.VISIBLE
        }

        val valueView = rowView.findViewById<TextView>(R.id.rowValue)
        if (value != null) {
            valueView.text = value
            valueView.visibility = View.VISIBLE
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private data class LanguageOption(
        val code: String,
        val labelRes: Int
    )

    private companion object {
        const val DEFAULT_LANGUAGE_CODE = "vi"

        val LANGUAGE_OPTIONS = listOf(
            LanguageOption("vi", R.string.profile_language_vi_value),
            LanguageOption("en", R.string.profile_language_en_value)
        )
    }
}
