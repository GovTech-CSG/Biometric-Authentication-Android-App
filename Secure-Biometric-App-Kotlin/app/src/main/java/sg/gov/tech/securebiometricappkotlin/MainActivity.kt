package sg.gov.tech.securebiometricappkotlin

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {

    private lateinit var inputSecret : String
    private var  isLockFlag : Boolean = false
    private lateinit var encryptedInfo : ByteArray
    private lateinit var textField : EditText
    private lateinit var secureInput_button : Button
    private lateinit var secureBiometricManager : SecureBiometricManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textField = findViewById(R.id.editText)
        secureInput_button = findViewById(R.id.secureButton)
        secureBiometricManager = SecureBiometricManager(this,listener);
        secureInput_button.setOnClickListener(){
            secureBiometricManager.showBiometricDialog(isLockFlag)
        }

        secureBiometricManager.checkBiometricIsAvailable()
    }

    private var listener: BiometricListener = object : BiometricListener {
        override fun onSuccess(result: BiometricPrompt.AuthenticationResult?) = if (isLockFlag) {
            // Decrypt data
            encryptedInfo = result!!.cryptoObject!!.cipher!!.doFinal(encryptedInfo)
            val decryptedString =
                String(encryptedInfo, StandardCharsets.UTF_8)

            // turn button text red and enable EditText field
            secureInput_button.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark))
            secureInput_button.text = "Encrypt Text"
            isLockFlag = false
            textField.setText(decryptedString)
            textField.isEnabled = true

        } else {
            // Encrypt data
            inputSecret = textField.text.toString()
            encryptedInfo = ByteArray(0)

            if (result != null && inputSecret !="")  {
                encryptedInfo = result.cryptoObject?.cipher?.doFinal(inputSecret.toByteArray(Charset.defaultCharset())
                )!!

                // convert bytes[] to string
                val encryptedString =
                    String(encryptedInfo, StandardCharsets.UTF_8)
                Log.d("MY_APP_TAG", "Encrypted string: $encryptedString")

                //turn button text green and disable EditText field
                secureInput_button.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
                secureInput_button.text = "Decrypt Text"
                isLockFlag = true
                textField.setText(encryptedString)
                textField.isEnabled = false
            }
            else{
                onFailed()
            }
        }

        override fun onFailed() {
            val input = "Error in authenticating biometric"
            Log.e("MY_APP_TAG", input)
            Toast.makeText(this@MainActivity, input, Toast.LENGTH_LONG).show()
        }
    }

}