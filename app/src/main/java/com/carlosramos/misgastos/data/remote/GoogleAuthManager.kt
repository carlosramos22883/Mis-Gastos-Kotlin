package com.carlosramos.misgastos.data.remote

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleAuthManager(private val context: Context) {

    private val serverClientId = "130078023125-vf9utt6p205pb7o2bc4eo77c0t5t3uiq.apps.googleusercontent.com"

    suspend fun signIn(): String? {
        return try {
            Log.d("GOOGLE_AUTH", "🔵 Iniciando flujo de Google Auth...")
            Log.d("GOOGLE_AUTH", "🔵 Server Client ID: $serverClientId")

            val credentialManager = CredentialManager.create(context)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(serverClientId)
                .setFilterByAuthorizedAccounts(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            Log.d("GOOGLE_AUTH", "🔵 Solicitando credencial...")

            val result = credentialManager.getCredential(
                context = context,
                request = request
            )

            val credential = result.credential
            Log.d("GOOGLE_AUTH", "🔵 Tipo de credencial: ${credential.javaClass.simpleName}")

            // Manejar ambos tipos de credenciales
            return when (credential) {
                is GoogleIdTokenCredential -> {
                    Log.d("GOOGLE_AUTH", "✅ GoogleIdTokenCredential obtenido directamente")
                    credential.idToken
                }
                is CustomCredential -> {
                    Log.d("GOOGLE_AUTH", "🔵 CustomCredential detectado, intentando convertir...")
                    Log.d("GOOGLE_AUTH", "🔵 Tipo de CustomCredential: ${credential.type}")

                    // Si es un Google ID token, convertirlo
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        val googleIdCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        Log.d("GOOGLE_AUTH", "✅ CustomCredential convertido a GoogleIdTokenCredential")
                        googleIdCredential.idToken
                    } else {
                        Log.e("GOOGLE_AUTH", "❌ CustomCredential no es de tipo Google ID Token")
                        null
                    }
                }
                else -> {
                    Log.e("GOOGLE_AUTH", "❌ Tipo de credencial no soportado: ${credential.javaClass.name}")
                    null
                }
            }
        } catch (e: GetCredentialException) {
            Log.e("GOOGLE_AUTH", "❌ GetCredentialException: ${e.message}")
            Log.e("GOOGLE_AUTH", "❌ Tipo de error: ${e.type}")
            e.printStackTrace()
            null
        } catch (e: Exception) {
            Log.e("GOOGLE_AUTH", "❌ Exception: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}