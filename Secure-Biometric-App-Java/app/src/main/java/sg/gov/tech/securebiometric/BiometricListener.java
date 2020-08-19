package sg.gov.tech.securebiometric;

import androidx.biometric.BiometricPrompt;

public interface BiometricListener {
    void onSuccess(BiometricPrompt.AuthenticationResult result);
    void onFailed();
}
