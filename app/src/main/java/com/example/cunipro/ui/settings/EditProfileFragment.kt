package com.example.cunipro.ui.settings

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.cunipro.databinding.DialogChangePasswordBinding
import com.example.cunipro.databinding.FragmentEditProfileBinding

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnChangePasswordInProfile.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.btnSaveProfile.setOnClickListener {
            val name = binding.etFullName.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val farm = binding.etFarmName.text.toString().trim()

            if (name.isBlank()) {
                Toast.makeText(requireContext(), "Veuillez entrer votre nom complet", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.saveProfile(name, phone, farm)
            }
        }

        // Live calculation of initials as user types name
        binding.etFullName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.tvAvatarInitials.text = viewModel.computeInitials(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            if (binding.etFullName.text.toString() != name) {
                binding.etFullName.setText(name)
            }
        }

        viewModel.userEmail.observe(viewLifecycleOwner) { email ->
            binding.etEmail.setText(email)
        }

        viewModel.userPhone.observe(viewLifecycleOwner) { phone ->
            binding.etPhone.setText(phone)
        }

        viewModel.userFarmName.observe(viewLifecycleOwner) { farm ->
            binding.etFarmName.setText(farm)
        }

        viewModel.userInitials.observe(viewLifecycleOwner) { initials ->
            binding.tvAvatarInitials.text = initials
        }

        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                Toast.makeText(requireContext(), "Profil enregistré avec succès", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun showChangePasswordDialog() {
        val dialogBinding = DialogChangePasswordBinding.inflate(layoutInflater)
        val authRepo = (requireActivity().application as com.example.cunipro.CuniProApplication).authRepository

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton("Changer", null)
            .setNegativeButton("Annuler", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val curr = dialogBinding.etCurrentPassword.text.toString().trim()
                val newPass = dialogBinding.etNewPassword.text.toString().trim()
                val conf = dialogBinding.etConfirmPassword.text.toString().trim()

                if (curr.isEmpty()) {
                    dialogBinding.tilCurrentPassword.error = "Veuillez entrer votre mot de passe actuel"
                    return@setOnClickListener
                } else {
                    dialogBinding.tilCurrentPassword.error = null
                }

                if (newPass.length < 6) {
                    dialogBinding.tilNewPassword.error = "Le nouveau mot de passe doit contenir au moins 6 caractères"
                    return@setOnClickListener
                } else {
                    dialogBinding.tilNewPassword.error = null
                }

                if (newPass != conf) {
                    dialogBinding.tilConfirmPassword.error = "Les mots de passe ne correspondent pas"
                    return@setOnClickListener
                } else {
                    dialogBinding.tilConfirmPassword.error = null
                }

                lifecycleScope.launch {
                    val result = authRepo.updatePassword(curr, newPass)
                    result.onSuccess {
                        Toast.makeText(requireContext(), "Mot de passe modifié avec succès", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }.onFailure { ex ->
                        val msg = ex.localizedMessage ?: "Le mot de passe actuel est incorrect"
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
