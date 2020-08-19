package sg.gov.tech.insecure_biometric_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    private var  isLockFlag : Boolean = false
    private lateinit var textField : EditText
    private lateinit var secureInput_button : Button
    private lateinit var inSecureBiometricManager : InsecureBiometricManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textField = findViewById(R.id.editText)
        secureInput_button = findViewById(R.id.secureButton)
        inSecureBiometricManager = InsecureBiometricManager(this,listener);
        secureInput_button.setOnClickListener(){
            inSecureBiometricManager.showBiometricDialog(isLockFlag)
        }

        inSecureBiometricManager.checkBiometricIsAvailable()
    }

    private var listener: BiometricListener = object : BiometricListener {
        override fun onSuccess(){
            if(isLockFlag){
                // turn button text red and enable EditText field
                secureInput_button.setBackgroundColor(resources.getColor(android.R.color.holo_red_dark))
                secureInput_button.text = "Lock Text"
                isLockFlag = false
                textField.isEnabled = true
            }
            else{
                //turn button text green and disable EditText field
                secureInput_button.setBackgroundColor(resources.getColor(android.R.color.holo_green_dark))
                secureInput_button.text = "Unlock Text"
                isLockFlag = true
                textField.isEnabled = false
            }
        }
        override fun onFailed() {
            val input = "Error in authenticating biometric"
            Log.e("MY_APP_TAG", input)
            Toast.makeText(this@MainActivity, input, Toast.LENGTH_LONG).show()
        }
    }
}
