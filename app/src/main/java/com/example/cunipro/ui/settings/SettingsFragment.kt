package com.example.cunipro.ui.settings

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import com.example.cunipro.R
import com.example.cunipro.util.LanguageManager
import com.example.cunipro.util.TermsDialogHelper
import com.example.cunipro.util.setupTopActions
import com.example.cunipro.databinding.DialogBreedingDurationsBinding
import com.example.cunipro.databinding.DialogChangePasswordBinding
import com.example.cunipro.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupTopActions(
            binding.includeTopActions.btnTopFinance,
            binding.includeTopActions.btnTopNotifications,
            binding.includeTopActions.btnTopSettings,
            binding.includeTopActions.tvNotificationBadge
        )

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_editProfileFragment)
        }

        binding.btnPrefAlerts.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_alertSettingsFragment)
        }

        binding.btnPrefBreeding.setOnClickListener {
            showBreedingDurationsDialog()
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.btnTerms.setOnClickListener {
            TermsDialogHelper.showTermsDialog(requireContext())
        }

        binding.btnSyncNow.setOnClickListener {
            viewModel.triggerManualSync()
        }

        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.setting_logout)
                .setMessage(R.string.settings_logout_confirm_msg)
                .setPositiveButton(R.string.btn_yes) { _, _ ->
                    viewModel.signOut()
                    findNavController().navigate(R.id.action_settingsFragment_to_loginFragment)
                }
                .setNegativeButton(R.string.btn_cancel, null)
                .show()
        }

        setupLanguageButtons()
        setupSwitches()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshUserData()
    }

    private fun setupLanguageButtons() {
        binding.btnLangFr.setOnClickListener {
            viewModel.setLanguageIsFr(true)
            LanguageManager.setLanguage(requireContext(), true)
            Toast.makeText(requireContext(), getString(R.string.settings_lang_set_fr), Toast.LENGTH_SHORT).show()
        }

        binding.btnLangEn.setOnClickListener {
            viewModel.setLanguageIsFr(false)
            LanguageManager.setLanguage(requireContext(), false)
            Toast.makeText(requireContext(), getString(R.string.settings_lang_set_en), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSwitches() {
        binding.switchSyncWifiOnly.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setSyncWifiOnly(isChecked)
        }

        binding.switchAutoCloudBackup.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setAutoCloudBackup(isChecked)
        }
    }

    private fun observeViewModel() {
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.tvUserName.text = name
        }

        viewModel.userEmail.observe(viewLifecycleOwner) { email ->
            binding.tvUserEmail.text = email
        }

        viewModel.userFarmName.observe(viewLifecycleOwner) { farm ->
            binding.tvFarmName.text = farm
        }

        viewModel.userInitials.observe(viewLifecycleOwner) { initials ->
            binding.tvAvatarInitials.text = initials
        }

        viewModel.isLanguageFr.observe(viewLifecycleOwner) { isFr ->
            if (isFr) {
                binding.btnLangFr.setBackgroundResource(R.drawable.bg_pill_selected)
                binding.btnLangFr.setTextColor(Color.WHITE)

                binding.btnLangEn.setBackgroundColor(Color.TRANSPARENT)
                binding.btnLangEn.setTextColor(Color.parseColor("#1C1B1F"))
            } else {
                binding.btnLangEn.setBackgroundResource(R.drawable.bg_pill_selected)
                binding.btnLangEn.setTextColor(Color.WHITE)

                binding.btnLangFr.setBackgroundColor(Color.TRANSPARENT)
                binding.btnLangFr.setTextColor(Color.parseColor("#1C1B1F"))
            }
        }

        viewModel.syncWifiOnly.observe(viewLifecycleOwner) { enabled ->
            binding.switchSyncWifiOnly.isChecked = enabled
        }

        viewModel.autoCloudBackup.observe(viewLifecycleOwner) { enabled ->
            binding.switchAutoCloudBackup.isChecked = enabled
        }

        viewModel.lastSyncTime.observe(viewLifecycleOwner) { timeText ->
            binding.tvLastSync.text = getString(R.string.settings_last_sync_format, timeText)
        }

        viewModel.dataToSyncCount.observe(viewLifecycleOwner) { count ->
            binding.tvDataToSyncCount.text = count.toString()
        }

        viewModel.palpationDays.observe(viewLifecycleOwner) { updateBreedingSubtitle() }
        viewModel.gestationDays.observe(viewLifecycleOwner) { updateBreedingSubtitle() }
        viewModel.sevrageDays.observe(viewLifecycleOwner) { updateBreedingSubtitle() }

        viewModel.isSyncing.observe(viewLifecycleOwner) { syncing ->
            binding.btnSyncNow.isEnabled = !syncing
        }

        viewModel.syncResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    Toast.makeText(requireContext(), getString(R.string.settings_sync_success), Toast.LENGTH_SHORT).show()
                } else {
                    val errMsg = it.exceptionOrNull()?.localizedMessage ?: "Erreur de synchronisation"
                    Toast.makeText(requireContext(), errMsg, Toast.LENGTH_LONG).show()
                }
                viewModel.clearSyncResult()
            }
        }
    }

    private fun updateBreedingSubtitle() {
        val palp = viewModel.palpationDays.value ?: 12
        val gest = viewModel.gestationDays.value ?: 31
        val sev = viewModel.sevrageDays.value ?: 35
        binding.tvBreedingDurationsText.text = getString(R.string.settings_breeding_durations_format, palp, gest, sev)
    }

    private fun showBreedingDurationsDialog() {
        val dialogBinding = DialogBreedingDurationsBinding.inflate(layoutInflater)
        dialogBinding.etDurationPalpation.setText((viewModel.palpationDays.value ?: 12).toString())
        dialogBinding.etDurationGestation.setText((viewModel.gestationDays.value ?: 31).toString())
        dialogBinding.etDurationSevrage.setText((viewModel.sevrageDays.value ?: 35).toString())

        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.btn_save) { _, _ ->
                val palp = dialogBinding.etDurationPalpation.text.toString().toIntOrNull() ?: 12
                val gest = dialogBinding.etDurationGestation.text.toString().toIntOrNull() ?: 31
                val sev = dialogBinding.etDurationSevrage.text.toString().toIntOrNull() ?: 35
                viewModel.updateBreedingDurations(palp, gest, sev)
                Toast.makeText(requireContext(), getString(R.string.settings_durations_updated), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun showChangePasswordDialog() {
        val dialogBinding = DialogChangePasswordBinding.inflate(layoutInflater)
        val authRepo = (requireActivity().application as com.example.cunipro.CuniProApplication).authRepository

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.profile_btn_change_pass, null)
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val curr = dialogBinding.etCurrentPassword.text.toString().trim()
                val newPass = dialogBinding.etNewPassword.text.toString().trim()
                val conf = dialogBinding.etConfirmPassword.text.toString().trim()

                if (curr.isEmpty()) {
                    dialogBinding.tilCurrentPassword.error = getString(R.string.settings_password_current_empty)
                    return@setOnClickListener
                } else {
                    dialogBinding.tilCurrentPassword.error = null
                }

                if (newPass.length < 6) {
                    dialogBinding.tilNewPassword.error = getString(R.string.settings_password_min_length)
                    return@setOnClickListener
                } else {
                    dialogBinding.tilNewPassword.error = null
                }

                if (newPass != conf) {
                    dialogBinding.tilConfirmPassword.error = getString(R.string.settings_password_mismatch)
                    return@setOnClickListener
                } else {
                    dialogBinding.tilConfirmPassword.error = null
                }

                lifecycleScope.launch {
                    val result = authRepo.updatePassword(curr, newPass)
                    result.onSuccess {
                        Toast.makeText(requireContext(), getString(R.string.settings_password_updated), Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }.onFailure { ex ->
                        val msg = ex.localizedMessage ?: getString(R.string.settings_password_incorrect)
                        dialogBinding.tilCurrentPassword.error = msg
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
