package sg.gov.tech.securebiometricappkotlin

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyStore
import java.util.concurrent.Executor
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class SecureBiometricManager(context: MainActivity, listener: BiometricListener) {
    private var context : Context = context
    private lateinit var executor : Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: PromptInfo
    private lateinit var biometricManager: BiometricManager
    private var listener: BiometricListener = listener
    private lateinit var iv : ByteArray
    /* This should not be hardcoded on app layer*/
    private var KEY_NAME = java.lang.String.valueOf(R.string.secret_key_name)
    private var VALIDITY_DURATION = -1;

    init{
        generateSecretKey(
            KeyGenParameterSpec.Builder(
                KEY_NAME,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setUserAuthenticationRequired(true)
                // Invalidate the keys if the user has registered a new biometric
                // credential, such as a new fingerprint. Can call this method only
                // on Android 7.0 (API level 24) or higher. The variable
                // "invalidatedByBiometricEnrollment" is true by default.
                .setInvalidatedByBiometricEnrollment(true)
                // The other important property is setUserAuthenticationValidityDurationSeconds().
                // If it is set to -1 then the key can only be unlocked using Fingerprint or Biometrics.
                // If it is set to any other value, the key can be unlocked using a device screenlock too.
                .setUserAuthenticationValidityDurationSeconds(VALIDITY_DURATION)
                .build()
        )
    }

    /**
     * This method is called when the onClick is called.
     *  It will first setup the biometric authentication dialog and
     * gets  an instance of SecretKey and then initializes the Cipher
     * with the key. The secret key uses [DECRYPT_MODE][Cipher.DECRYPT_MODE].
     * @param isLockFlag
     */
    fun showBiometricDialog(isLockFlag : Boolean) {
        // Initialize everything needed for authentication
        setupBiometricPrompt()
        val cipher = getCipher()
        val secretKey = getSecretKey()

        // Encrypt mode
        if (!isLockFlag){
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        }
        // Decrypt mode
        else{
            cipher.init(
                Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv)
            )
        }
        // Prompt appears when user clicks authentication button.
        biometricPrompt.authenticate(
            promptInfo,
            BiometricPrompt.CryptoObject(cipher)
        )
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
                    // For decryption later on, we need to keep hold of the cipher's initialization vector
                    iv = result.cryptoObject?.cipher?.iv!!
                    listener.onSuccess(result)
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

    /**
     * This method generates an instance of SecretKey
     */
    private fun generateSecretKey(keyGenParameterSpec: KeyGenParameterSpec) {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
        )
        keyGenerator.init(keyGenParameterSpec)
        keyGenerator.generateKey()
    }

    /**
     * This method gets an instance of SecretKey
     */
    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")

        // Before the keystore can be accessed, it must be loaded.
        keyStore.load(null)
        return keyStore.getKey(KEY_NAME, null) as SecretKey
    }

    /**
     * This method gets a cipher instance
     */
    private fun getCipher(): Cipher {
        return Cipher.getInstance(
            KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7
        )
    }

}