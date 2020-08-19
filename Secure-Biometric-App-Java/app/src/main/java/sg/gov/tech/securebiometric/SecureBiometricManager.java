package sg.gov.tech.securebiometric;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.Executor;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;


public class SecureBiometricManager {
    private Context context;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private BiometricManager biometricManager;
    private BiometricListener listener;
    byte[] iv;
    /* This should not be hardcoded on app layer*/
    private String KEY_NAME = String.valueOf(R.string.secret_key_name);
    int VALIDITY_DURATION = -1;


    SecureBiometricManager(Context context, BiometricListener listener){
        this.context = context;
        this.listener = listener;
        generateSecretKey(new KeyGenParameterSpec.Builder(
                KEY_NAME,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
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
                .build());
    }

    /**
     * This method is called when the onClick is called.
     *  It will first setup the biometric authentication dialog and
     * gets  an instance of SecretKey and then initializes the Cipher
     * with the key. The secret key uses [DECRYPT_MODE][Cipher.DECRYPT_MODE].
     * @param isLockFlag
     */
    void showBiometricDialog(boolean isLockFlag) {
        // Initialize everything needed for authentication
        setupBiometricPrompt();

        Cipher cipher = getCipher();
        SecretKey secretKey = getSecretKey();
        try {
            // Encrypt String
            if(!isLockFlag){
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            }
            // Decrypt String
            else{
                cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            }
            // Prompt appears when user clicks authentication button.
            biometricPrompt.authenticate(promptInfo, new BiometricPrompt.CryptoObject(cipher));
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method checks if the device can support biometric authentication APIs
     */
    public void checkBiometricIsAvailable(){
        String input;
        biometricManager = BiometricManager.from(this.context);
        switch (biometricManager.canAuthenticate()) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                input = "App can authenticate using biometrics.";
                Log.d("MY_APP_TAG", input);
                Toast.makeText(context, input, Toast.LENGTH_LONG).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                input = "No biometric features available on this device.";
                Log.e("MY_APP_TAG", input);
                Toast.makeText(context, input, Toast.LENGTH_LONG).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                input = "App can authenticate using biometrics.";
                Log.e("MY_APP_TAG", "Biometric features are currently unavailable.");
                Toast.makeText(context, input, Toast.LENGTH_LONG).show();
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                input = "The user hasn't associated any biometric credentials with their account..";
                Log.e("MY_APP_TAG", input);
                Toast.makeText(context, input, Toast.LENGTH_LONG).show();
                break;
        }
    }

    /**
     * This method setups the biometric authentication dialog
     */
    private void setupBiometricPrompt(){
        executor = ContextCompat.getMainExecutor(context);
        biometricPrompt = new BiometricPrompt((FragmentActivity) context,
                executor,
                new BiometricPrompt.AuthenticationCallback() {

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Toast.makeText(context,
                                "Authentication error: " + errString, Toast.LENGTH_SHORT)
                                .show();
                        listener.onFailed();
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result){
                        super.onAuthenticationSucceeded(result);
                        listener.onSuccess(result);
                        // For decryption later on, we need to keep hold of the cipher's initialization vector
                        iv = result.getCryptoObject().getCipher().getIV();
                    }

                    @Override
                    public void onAuthenticationFailed(){
                        super.onAuthenticationFailed();
                        Toast.makeText(context, "Authentication failed",
                                Toast.LENGTH_SHORT)
                                .show();
                        listener.onFailed();

                    }
                });

        // Create prompt dialog
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric login for sample app")
                .setSubtitle("Log in using your biometric")
                .setNegativeButtonText("Use account password")
                .build();
    }

    /**
     * This method generates an instance of SecretKey
     */
    private void generateSecretKey(KeyGenParameterSpec keyGenParameterSpec) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyGenerator.init(keyGenParameterSpec);
            keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method gets an instance of SecretKey
     */
    private SecretKey getSecretKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            // Before the keystore can be accessed, it must be loaded.
            keyStore.load(null);
            return ((SecretKey)keyStore.getKey(KEY_NAME, null));
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (CertificateException ex) {
            ex.printStackTrace();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        } catch (IOException | UnrecoverableKeyException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * This method gets a cipher instance
     */
    private Cipher getCipher() {
        try {
            return Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
