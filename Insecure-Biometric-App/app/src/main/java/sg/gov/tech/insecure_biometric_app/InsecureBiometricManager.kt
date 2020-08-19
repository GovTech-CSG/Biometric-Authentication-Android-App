package sg.gov.tech.insecure_biometric_app

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

class InsecureBiometricManager(context: MainActivity, listener: BiometricListener){

    private var context : Context = context
    private lateinit var executor : Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: PromptInfo
    private lateinit var biometricManager: BiometricManager
    private var listener: BiometricListener = listener

    /**
     * This method is called when the onClick is called.
     *  It will first setup the biometric authentication dialog
     * @param isLockFlag
     */
    fun showBiometricDialog(isLockFlag : Boolean) {
        // Initialize everything needed for authentication
        setupBiometricPrompt()

        // Prompt appears when user clicks authentication button.
        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * This method checks if the device can support biometric authentication APIs
     */
    fun checkBiometricIsAvailable(){
        var input: String
        biometricManager = BiometricManager.from(context)
        when (biometricManager.canAuthenticate()) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                Log.d("MY_APP_TAG", "App can authenticate using biometrics.")
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                Log.e("MY_APP_TAG", "No biometric features available on this device.")
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                Log.e("MY_APP_TAG", "Biometric features are currently unavailable.")
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                Log.e("MY_APP_TAG", "The user hasn't associated " +
                        "any biometric credentials with their account.")
        }
    }

    /**
     * This method setups the biometric authentication dialog
     */
    private fun setupBiometricPrompt(){
        executor = ContextCompat.getMainExecutor(context)
        biometricPrompt = BiometricPrompt(
            (context as FragmentActivity)!!, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int,
                                                   errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    listener.onFailed()
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Toast.makeText(context,
                        "Authentication succeeded!", Toast.LENGTH_SHORT)
                        .show()
                    listener.onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(context, "Authentication failed",
                        Toast.LENGTH_SHORT)
                        .show()
                }
            })

        // Create prompt dialog
        promptInfo = PromptInfo.Builder()
            .setTitle("Biometric login for my app")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Use account password")
            .build()
    }


}
