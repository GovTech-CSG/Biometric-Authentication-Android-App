package sg.gov.tech.securebiometric;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;


public class MainActivity extends AppCompatActivity {

    private String inputSecret;
    private boolean isLockFlag;
    private byte[] encryptedInfo;
    EditText textField;
    Button secureInput_button;
    SecureBiometricManager secureBiometricManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isLockFlag = false;
        textField = findViewById(R.id.editText);
        secureBiometricManager = new SecureBiometricManager(MainActivity.this, listener);
        secureInput_button = findViewById(R.id.secureButton);
        secureInput_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                secureBiometricManager.showBiometricDialog(isLockFlag);
            }
        });

        secureBiometricManager.checkBiometricIsAvailable();
    }

    BiometricListener listener = new BiometricListener() {
        @Override
        public void onSuccess(BiometricPrompt.AuthenticationResult result) {
            if (isLockFlag){
                // Decrypt data
                try {
                    encryptedInfo= result.getCryptoObject().getCipher().doFinal(encryptedInfo);
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
                String decryptedString = new String(encryptedInfo, StandardCharsets.UTF_8);

                // turn button text red and enable EditText field
                secureInput_button.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
                secureInput_button.setText("Encrypt Text");
                isLockFlag = false;
                textField.setText(decryptedString);
                textField.setEnabled(true);

            }
            else{
                // Encrypt data
                inputSecret = textField.getText().toString();
                encryptedInfo = new byte[0];
                try {
                    encryptedInfo = result.getCryptoObject().getCipher().doFinal(
                            inputSecret.getBytes(Charset.defaultCharset()));
                } catch (BadPaddingException e) {
                    e.printStackTrace();
                } catch (IllegalBlockSizeException e) {
                    e.printStackTrace();
                }
                // convert bytes[] to string
                String encryptedString = new String(encryptedInfo, StandardCharsets.UTF_8);
                Log.d("MY_APP_TAG", "Encrypted string: " + encryptedString);

                //turn button text green and disable EditText field
                secureInput_button.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
                secureInput_button.setText("Decrypt Text");
                isLockFlag = true;
                textField.setText(encryptedString);
                textField.setEnabled(false);
            }

        }

        @Override
        public void onFailed() {
            String input = "Error in authenticating biometric";
            Log.e("MY_APP_TAG", input);
            Toast.makeText(MainActivity.this, input, Toast.LENGTH_LONG).show();
        }
    };

}
