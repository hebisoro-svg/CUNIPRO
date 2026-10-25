package com.example.cunipro.ui.auth

import android.content.Context
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.cunipro.CuniProApplication
import com.example.cunipro.R
import com.example.cunipro.data.repository.AuthRepository
import com.example.cunipro.databinding.FragmentRegisterBinding
import com.example.cunipro.util.LanguageManager
import com.example.cunipro.util.TermsDialogHelper
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private var isFrench: Boolean = true
    private lateinit var authRepository: AuthRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val app = requireActivity().application as CuniProApplication
        authRepository = app.authRepository

        isFrench = LanguageManager.isFrench(requireContext())

        applyLanguage(isFrench)

        binding.btnLangFr.setOnClickListener {
            if (!isFrench) {
                isFrench = true
                LanguageManager.saveLanguagePref(requireContext(), true)
                applyLanguage(true)
            }
        }

        binding.btnLangEn.setOnClickListener {
            if (isFrench) {
                isFrench = false
                LanguageManager.saveLanguagePref(requireContext(), false)
                applyLanguage(false)
            }
        }

        binding.btnCreateAccount.setOnClickListener {
            validateAndRegister()
        }

        binding.tvTermsLabel.setOnClickListener {
            TermsDialogHelper.showTermsDialog(requireContext())
        }

        binding.tvLoginLink.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }
    }

    private fun applyLanguage(isFr: Boolean) {
        val darkGreen = ContextCompat.getColor(requireContext(), R.color.cuni_green_primary_dark)
        val white = ContextCompat.getColor(requireContext(), R.color.white)

        binding.tilFullName.isHintEnabled = false
        binding.tilFullName.hint = null
        binding.tilEmail.isHintEnabled = false
        binding.tilEmail.hint = null
        binding.tilPassword.isHintEnabled = false
        binding.tilPassword.hint = null
        binding.tilConfirmPassword.isHintEnabled = false
        binding.tilConfirmPassword.hint = null

        if (isFr) {
            binding.btnLangFr.setBackgroundResource(R.drawable.bg_language_selected)
            binding.btnLangFr.setTextColor(darkGreen)

            binding.btnLangEn.setBackgroundResource(android.R.color.transparent)
            binding.btnLangEn.setTextColor(white)

            binding.tvRegisterTitle.text = "Créer votre compte"
            binding.tvRegisterSubtitle.text = "Suivez votre élevage de lapins, même sans connexion."

            binding.tvLabelFullName.text = "Nom complet"
            binding.etFullName.hint = "Ex. Awa Koné"

            binding.tvLabelEmail.text = "Adresse e-mail"
            binding.etEmail.hint = "vous@exemple.com"

            binding.tvLabelPassword.text = "Mot de passe"
            binding.etPassword.hint = "8 caractères minimum"

            binding.tvLabelConfirmPassword.text = "Confirmer le mot de passe"
            binding.etConfirmPassword.hint = "Saisissez-le à nouveau"

            val termsHtml = "J'accepte les <font color='#2E7D32'><b>conditions d'utilisation</b></font>"
            binding.tvTermsLabel.text = Html.fromHtml(termsHtml, Html.FROM_HTML_MODE_LEGACY)

            binding.btnCreateAccount.text = "Créer mon compte"

            val loginHtml = "Déjà un compte ? <font color='#2E7D32'><b>Se connecter</b></font>"
            binding.tvLoginLink.text = Html.fromHtml(loginHtml, Html.FROM_HTML_MODE_LEGACY)

            binding.tvOfflineBadge.text = "Fonctionne aussi sans internet"
            binding.tvLoadingText.text = "Création du compte..."
        } else {
            binding.btnLangEn.setBackgroundResource(R.drawable.bg_language_selected)
            binding.btnLangEn.setTextColor(darkGreen)

            binding.btnLangFr.setBackgroundResource(android.R.color.transparent)
            binding.btnLangFr.setTextColor(white)

            binding.tvRegisterTitle.text = "Create your account"
            binding.tvRegisterSubtitle.text = "Track your rabbit farm, even without a connection."

            binding.tvLabelFullName.text = "Full name"
            binding.etFullName.hint = "e.g. Awa Koné"

            binding.tvLabelEmail.text = "Email address"
            binding.etEmail.hint = "you@example.com"

            binding.tvLabelPassword.text = "Password"
            binding.etPassword.hint = "8 characters minimum"

            binding.tvLabelConfirmPassword.text = "Confirm password"
            binding.etConfirmPassword.hint = "Enter it again"

            val termsHtml = "I accept the <font color='#2E7D32'><b>terms of use</b></font>"
            binding.tvTermsLabel.text = Html.fromHtml(termsHtml, Html.FROM_HTML_MODE_LEGACY)

            binding.btnCreateAccount.text = "Create my account"

            val loginHtml = "Already have an account? <font color='#2E7D32'><b>Log in</b></font>"
            binding.tvLoginLink.text = Html.fromHtml(loginHtml, Html.FROM_HTML_MODE_LEGACY)

            binding.tvOfflineBadge.text = "Works offline too"
            binding.tvLoadingText.text = "Creating account..."
        }
    }

    private fun validateAndRegister() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (fullName.isEmpty()) {
            val err = if (isFrench) "Veuillez entrer votre nom complet" else "Please enter your full name"
            binding.tilFullName.error = err
            return
        } else {
            binding.tilFullName.error = null
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            val err = if (isFrench) "Veuillez entrer un e-mail valide" else "Please enter a valid email"
            binding.tilEmail.error = err
            return
        } else {
            binding.tilEmail.error = null
        }

        if (password.length < 8) {
            val err = if (isFrench) "8 caractères minimum requis" else "Minimum 8 characters required"
            binding.tilPassword.error = err
            return
        } else {
            binding.tilPassword.error = null
        }

        if (password != confirmPassword) {
            val err = if (isFrench) "Les mots de passe ne correspondent pas" else "Passwords do not match"
            binding.tilConfirmPassword.error = err
            return
        } else {
            binding.tilConfirmPassword.error = null
        }

        if (!binding.cbTerms.isChecked) {
            val err = if (isFrench) "Veuillez accepter les conditions d'utilisation" else "Please accept the terms of use"
            Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            val result = authRepository.signUpWithEmail(email, password, fullName)
            setLoading(false)

            result.onSuccess { user ->
                val prefs = requireContext().getSharedPreferences("cunipro_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("user_registered", true)
                    .putBoolean("is_logged_in", true)
                    .putString("user_name", fullName)
                    .putString("user_email", user.email ?: email)
                    .apply()

                LanguageManager.setLanguage(requireContext(), isFrench)

                val successMsg = if (isFrench) "Compte créé avec succès ! Bienvenue $fullName" else "Account created successfully! Welcome $fullName"
                Toast.makeText(requireContext(), successMsg, Toast.LENGTH_SHORT).show()

                findNavController().navigate(R.id.dashboardFragment)
            }.onFailure { exception ->
                val errorMessage = when (exception) {
                    is FirebaseAuthUserCollisionException -> if (isFrench) "Cet e-mail est déjà utilisé par un autre compte." else "This email is already in use by another account."
                    is FirebaseAuthWeakPasswordException -> if (isFrench) "Le mot de passe est trop faible." else "The password is too weak."
                    else -> exception.localizedMessage ?: if (isFrench) "Erreur lors de la création du compte." else "Error creating account."
                }
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.cardLoadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnCreateAccount.isEnabled = !isLoading
        binding.etFullName.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
        binding.cbTerms.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
