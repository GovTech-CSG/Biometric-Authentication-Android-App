package sg.gov.tech.securebiometricappkotlin

import androidx.biometric.BiometricPrompt

interface BiometricListener {
    fun onSuccess(result: BiometricPrompt.AuthenticationResult?)
    fun onFailed()
}