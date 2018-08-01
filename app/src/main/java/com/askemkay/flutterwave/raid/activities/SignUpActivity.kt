package com.askemkay.flutterwave.raid.activities

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
//import com.alc.askemkay.journalapp.R
import com.askemkay.flutterwave.raid.MainActivity
import com.askemkay.flutterwave.raid.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlinx.android.synthetic.main.app_bar_main.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.longToast

class SignUpActivity : AppCompatActivity() {

    private lateinit var mEmailField: TextInputEditText
    private lateinit var mPasswordField: TextInputEditText
    private lateinit var mPasswordFieldAgain: TextInputEditText
    private lateinit var mSignUpButton: Button
    private lateinit var mProgressBar: ProgressBar

    private lateinit var mAuth: FirebaseAuth

//    private val TAG = SignUpActivity::class.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initializeComponents()
    }

    private fun initializeComponents(){
        mSignUpButton = findViewById(R.id.signUpButton)
        mEmailField = findViewById(R.id.emailField)
        mPasswordField = findViewById(R.id.passwordField)
        mPasswordFieldAgain = findViewById(R.id.passwordFieldAgain)
        mProgressBar = findViewById(R.id.progressBar)

//        setSupportActionBar(toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mAuth = FirebaseAuth.getInstance()

        mSignUpButton.setOnClickListener {
            startSignInFlow()
        }

    }

    private fun startSignInFlow() {
        mProgressBar.visibility = View.VISIBLE
        longToast(validateInput(listOf(mEmailField, mPasswordField, mPasswordFieldAgain)).toString()).show()
        if (validateInput(listOf(mEmailField, mPasswordField, mPasswordFieldAgain))) {
            if (mPasswordField.text.toString() == mPasswordFieldAgain.text.toString()) {

                mAuth.createUserWithEmailAndPassword(mEmailField.text.toString(),
                        mPasswordField.text.toString())
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                val user = mAuth.currentUser
                                user?.sendEmailVerification()
                                mProgressBar.visibility = View.GONE
                                val mainActivityIntent = Intent(this@SignUpActivity,
                                        MainActivity::class.java)

                                mainActivityIntent.putExtra("displayName", mAuth.currentUser?.displayName)
                                mainActivityIntent.putExtra("email", mAuth.currentUser?.email)

                                startActivity(mainActivityIntent)
                                finish()

                            } else {
                                alert {
                                    message = "Error: ${it.exception?.message}"
                                }.show()
                            }
                        }
            } else mPasswordFieldAgain.error = "Passwords do not match."
        }
    }

    private fun validateInput(views: List<View>): Boolean{
        for (view in views){
            if (view is TextInputEditText){
                if (view == mEmailField){
                    val emailSupplied = mEmailField.text.toString()
                    val matched =  emailSupplied.matches(("^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]|[\\w-]{2,}))@"
                            + "((([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                            + "[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\."
                            + "([0-1]?[0-9]{1,2}|25[0-5]|2[0-4][0-9])\\.([0-1]?"
                            + "[0-9]{1,2}|25[0-5]|2[0-4][0-9]))|"
                            + "([a-zA-Z]+[\\w-]+\\.)+[a-zA-Z]{2,4})$").toRegex())

                    if (!matched) {
                        emailField.error = "Enter a valid email"
                        return false
                    }
                } else if (view == mPasswordField || view == mPasswordFieldAgain){
                    val passwordOkay = view.text.toString().length >= 6

                    if (!passwordOkay) {
                        view.error = "Enter a valid password"
                        return false
                    }
                }
            }
        }
        return true
    }
}
