package com.vationx.accessdemo.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.vationx.access.sdk.AccessSDK
import com.vationx.accessdemo.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class SignInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)
        GlobalScope.launch {
            delay(1500L)
            if (AccessSDK.userModule.isAuth()) {
                startActivity(Intent(this@SignInActivity, MainActivity::class.java))
            } else {
                startActivity(Intent(this@SignInActivity, EntryActivity::class.java))
            }
            finish()
        }
    }
}