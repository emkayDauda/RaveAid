package com.askemkay.flutterwave.raid

import android.content.DialogInterface
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.androidstudy.networkmanager.Tovuti
import com.askemkay.flutterwave.raid.activities.LoginActivity
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import org.jetbrains.anko.alert
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import java.util.*
import com.firebase.ui.auth.ErrorCodes
import android.R.attr.data
import android.app.Activity
import android.util.Log
import com.firebase.ui.auth.IdpResponse



class SplashActivity : AppCompatActivity() {

    private lateinit var alert: DialogInterface
    private lateinit var mAuth: FirebaseAuth

    private val RC_SIGN_IN = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mAuth = FirebaseAuth.getInstance()

        if (mAuth.currentUser != null) {
            val mainActivity = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(mainActivity)
            finish()
        } else {

            Tovuti.from(this).monitor { _, isConnected, _ ->


                if (isConnected) {
                    startActivityForResult(
                            AuthUI.getInstance().createSignInIntentBuilder()
                                    .setAvailableProviders(Arrays.asList(
                                            AuthUI.IdpConfig.GoogleBuilder().build(),
                                            AuthUI.IdpConfig.EmailBuilder().build()
                                    ))
                                    .build(), RC_SIGN_IN
                    )
                } else {
                    alert = alert {
                        title = "Internet Access"
                        message = "You'll need an active internet access to proceed"
                        negativeButton("Exit Application") {
                            finish()
                        }
                    }.show()
                }
            }
        }

    }

    override fun onStop() {
        Tovuti.from(this).stop()
        super.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            // Successfully signed in
            if (resultCode == Activity.RESULT_OK) {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            } else {
                // Sign in failed
                if (response == null) {
                    // User pressed back button
                    longToast("Sign In Failed...").show()
                    return
                }

            }
        }
    }
}
