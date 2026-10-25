package com.example.cunipro.data.repository

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthRepository(private val context: Context) {

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    val currentUser: FirebaseUser?
        get() = firebaseAuth.currentUser

    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    suspend fun signUpWithEmail(email: String, pass: String, displayName: String): Result<FirebaseUser> {
        return try {
            val authResult = awaitTask(firebaseAuth.createUserWithEmailAndPassword(email, pass))
            val user = authResult.user
            if (user != null && displayName.isNotBlank()) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
                awaitTask(user.updateProfile(profileUpdates))
            }
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Utilisateur introuvable après création du compte."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = awaitTask(firebaseAuth.signInWithEmailAndPassword(email, pass))
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Utilisateur introuvable."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePassword(currentPass: String, newPass: String): Result<Unit> {
        return try {
            val user = firebaseAuth.currentUser
            if (user != null && !user.email.isNullOrBlank()) {
                val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, currentPass)
                awaitTask(user.reauthenticate(credential))
                awaitTask(user.updatePassword(newPass))
                Result.success(Unit)
            } else {
                val prefs = context.getSharedPreferences("cunipro_settings_prefs", Context.MODE_PRIVATE)
                val savedPass = prefs.getString("pref_user_local_password", null)
                if (savedPass != null && savedPass != currentPass) {
                    Result.failure(Exception("Le mot de passe actuel est incorrect"))
                } else {
                    prefs.edit().putString("pref_user_local_password", newPass).apply()
                    Result.success(Unit)
                }
            }
        } catch (_: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Le mot de passe actuel est incorrect"))
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: ""
            if (msg.contains("password", ignoreCase = true) || msg.contains("credential", ignoreCase = true) || msg.contains("invalid", ignoreCase = true)) {
                Result.failure(Exception("Le mot de passe actuel est incorrect"))
            } else {
                Result.failure(e)
            }
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
        val prefs = context.getSharedPreferences("cunipro_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_logged_in", false).apply()
    }

    private suspend fun <T> awaitTask(task: Task<T>): T = suspendCancellableCoroutine { continuation ->
        task.addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }.addOnFailureListener { exception ->
            if (continuation.isActive) continuation.resumeWithException(exception)
        }.addOnCanceledListener {
            if (continuation.isActive) continuation.cancel()
        }
    }
}
