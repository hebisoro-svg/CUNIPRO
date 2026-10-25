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
import com.example.cunipro.databinding.FragmentLoginBinding
import com.example.cunipro.util.LanguageManager
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private var isFrench: Boolean = true
    private lateinit var authRepository: AuthRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
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

        binding.btnLogin.setOnClickListener {
            validateAndLogin()
        }

        binding.tvRegisterLink.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }
    }

    private fun applyLanguage(isFr: Boolean) {
        val darkGreen = ContextCompat.getColor(requireContext(), R.color.cuni_green_primary_dark)
        val white = ContextCompat.getColor(requireContext(), R.color.white)

        binding.tilEmail.isHintEnabled = false
        binding.tilEmail.hint = null
        binding.tilPassword.isHintEnabled = false
        binding.tilPassword.hint = null

        if (isFr) {
            binding.btnLangFr.setBackgroundResource(R.drawable.bg_language_selected)
            binding.btnLangFr.setTextColor(darkGreen)

            binding.btnLangEn.setBackgroundResource(android.R.color.transparent)
            binding.btnLangEn.setTextColor(white)

            binding.tvLoginTitle.text = "Se connecter"
            binding.tvLoginSubtitle.text = "Accédez à la gestion de votre élevage."

            binding.tvLabelEmail.text = "Adresse e-mail"
            binding.etEmail.hint = "vous@exemple.com"

            binding.tvLabelPassword.text = "Mot de passe"
            binding.etPassword.hint = "Saisissez votre mot de passe"

            binding.btnLogin.text = "Se connecter"

            val registerHtml = "Pas encore de compte ? <font color='#2E7D32'><b>S'inscrire</b></font>"
            binding.tvRegisterLink.text = Html.fromHtml(registerHtml, Html.FROM_HTML_MODE_LEGACY)

            binding.tvOfflineBadge.text = "Fonctionne aussi sans internet"
            binding.tvLoadingText.text = "Connexion en cours..."
        } else {
            binding.btnLangEn.setBackgroundResource(R.drawable.bg_language_selected)
            binding.btnLangEn.setTextColor(darkGreen)

            binding.btnLangFr.setBackgroundResource(android.R.color.transparent)
            binding.btnLangFr.setTextColor(white)

            binding.tvLoginTitle.text = "Log in"
            binding.tvLoginSubtitle.text = "Access your rabbit farm management."

            binding.tvLabelEmail.text = "Email address"
            binding.etEmail.hint = "you@example.com"

            binding.tvLabelPassword.text = "Password"
            binding.etPassword.hint = "Enter your password"

            binding.btnLogin.text = "Log in"

            val registerHtml = "Don't have an account? <font color='#2E7D32'><b>Sign up</b></font>"
            binding.tvRegisterLink.text = Html.fromHtml(registerHtml, Html.FROM_HTML_MODE_LEGACY)

            binding.tvOfflineBadge.text = "Works offline too"
            binding.tvLoadingText.text = "Logging in..."
        }
    }

    private fun validateAndLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            val err = if (isFrench) "Veuillez entrer un e-mail valide" else "Please enter a valid email"
            binding.tilEmail.error = err
            return
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            val err = if (isFrench) "Veuillez entrer votre mot de passe" else "Please enter your password"
            binding.tilPassword.error = err
            return
        } else {
            binding.tilPassword.error = null
        }

        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            val result = authRepository.signInWithEmail(email, password)
            setLoading(false)

            result.onSuccess { user ->
                val syncRepo = com.example.cunipro.data.repository.FirebaseSyncRepository(requireContext())
                syncRepo.syncCloudToLocal()

                val prefs = requireContext().getSharedPreferences("cunipro_prefs", Context.MODE_PRIVATE)
                val displayName = user.displayName ?: email.substringBefore("@")
                prefs.edit()
                    .putBoolean("user_registered", true)
                    .putBoolean("is_logged_in", true)
                    .putString("user_name", displayName)
                    .putString("user_email", user.email ?: email)
                    .apply()

                LanguageManager.setLanguage(requireContext(), isFrench)

                val successMsg = if (isFrench) "Connexion réussie ! Bon retour $displayName" else "Login successful! Welcome back $displayName"
                Toast.makeText(requireContext(), successMsg, Toast.LENGTH_SHORT).show()

                findNavController().navigate(R.id.dashboardFragment)
            }.onFailure { exception ->
                val errorMessage = when (exception) {
                    is FirebaseAuthInvalidUserException -> if (isFrench) "Aucun compte associé à cet e-mail." else "No account found with this email."
                    is FirebaseAuthInvalidCredentialsException -> if (isFrench) "Mot de passe incorrect ou identifiants invalides." else "Incorrect password or invalid credentials."
                    else -> exception.localizedMessage ?: if (isFrench) "Erreur de connexion." else "Login error."
                }
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.cardLoadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
