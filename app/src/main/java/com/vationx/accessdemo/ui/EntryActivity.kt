package com.vationx.accessdemo.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import com.vationx.access.sdk.AccessSDK
import com.vationx.accessdemo.R
import com.vationx.common.SDKCallback
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview

@FlowPreview
@ExperimentalCoroutinesApi
class EntryActivity : AppCompatActivity() {
    lateinit var account: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)
        val etAccount = findViewById<AppCompatEditText>(R.id.et_account)
        val etCode = findViewById<AppCompatEditText>(R.id.et_code)
        val btAuth = findViewById<AppCompatButton>(R.id.bt_auth)
        val btSignIn = findViewById<AppCompatButton>(R.id.bt_signin)

        btAuth.setOnClickListener {
            AccessSDK.userModule.getVerificationCodeForEmail(etAccount.text.toString(),
                object : SDKCallback<Unit> {
                    override fun onFailure(code: Int, message: String) {
                        Toast.makeText(
                            this@EntryActivity,
                            "request auth code failed, error code:$code message: $message",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onSuccess(result: Unit) {
                        account = etAccount.text.toString()
                        Toast.makeText(
                            this@EntryActivity,
                            "an email or sms text was sent to you account, please enter the code and login",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }
        btSignIn.setOnClickListener {
            AccessSDK.userModule.signInWithVerificationCodeForEmail(account, etCode.text.toString(),
                object : SDKCallback<Unit> {
                    override fun onFailure(code: Int, message: String) {
                        Toast.makeText(
                            this@EntryActivity,
                            "login failed, error code:$code message: $message",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onSuccess(result: Unit) {
                        Toast.makeText(
                            this@EntryActivity,
                            "login success!",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(Intent(this@EntryActivity, MainActivity::class.java))
                        finish()
                    }
                })
        }
    }
}