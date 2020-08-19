package sg.gov.tech.insecure_biometric_app

interface BiometricListener {
    fun onSuccess()
    fun onFailed()
}