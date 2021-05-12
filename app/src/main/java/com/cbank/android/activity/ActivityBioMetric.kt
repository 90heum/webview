package com.cbank.android.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.cbank.android.R
import com.cbank.android.databinding.ActivityBiometricBinding


class ActivityBioMetric : AppCompatActivity() {

    private lateinit var binding: ActivityBiometricBinding

    private var hIntro: Handler? = null

    private var nStatus : Int? = null

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_biometric
        )

        setInit()
        setEvent()

    }

    override fun onResume() {
        super.onResume()

        nStatus = BiometricManager.from(this).canAuthenticate()
    }

    private fun setUI() {}

    private fun setInit() {
        binding.activity = this@ActivityBioMetric

        biometricPrompt = createBiometricPrompt()
        promptInfo = createPromptInfo()

    }

    private fun setEvent() {}

    fun actionReg(){

        if (nStatus == BiometricManager.BIOMETRIC_SUCCESS) {

            biometricPrompt.authenticate(promptInfo)

        }else if (nStatus == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE) {

            Toast.makeText(
                applicationContext,
                "BIOMETRIC_ERROR_HW_UNAVAILABLE",
                Toast.LENGTH_LONG
            ).show()


        }else if (nStatus == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {

            Toast.makeText(
                applicationContext,
                "BIOMETRIC_ERROR_NONE_ENROLLED",
                Toast.LENGTH_LONG
            ).show()


            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                startActivity(intent)
            }else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    val intent = Intent(Settings.ACTION_FINGERPRINT_ENROLL)
                    startActivity(intent)
                }else {
                    val intent = Intent(Settings.ACTION_SETTINGS)
                    startActivity(intent)
                }
            }


        }else if (nStatus == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE) {

            Toast.makeText(
                applicationContext,
                "BIOMETRIC_ERROR_NO_HARDWARE",
                Toast.LENGTH_LONG
            ).show()

        }else {
            Toast.makeText(
                applicationContext,
                "사용할 수 없는 기기",
                Toast.LENGTH_LONG
            ).show()

        }
    }


    private fun createBiometricPrompt(): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.d("TAG", "$errorCode :: $errString")
            }
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.d("TAG", "인증 실패")

                Toast.makeText(
                    applicationContext,
                    "인증 실패",
                    Toast.LENGTH_LONG
                ).show()


            }
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(
                    applicationContext,
                    "인증 성공",
                    Toast.LENGTH_LONG
                ).show()
                //인증 완료 시 작업

            }
        }
        //The API requires the client/Activity context for displaying the prompt view
        return BiometricPrompt(this, executor, callback)
    }


    private fun createPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle("생체인증")
            .setDescription("생체인증을 시작합니다.")
            .setConfirmationRequired(false)
            .setNegativeButtonText(getString(R.string.prompt_info_use_app_password))
            .build()
    }

}