package com.askemkay.flutterwave.raid.activities

//import android.support.test.orchestrator.junit.BundleJUnitUtils.getResult
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.TextInputEditText
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
//import com.alc.askemkay.journalapp.R
import com.askemkay.flutterwave.raid.MainActivity
import com.askemkay.flutterwave.raid.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import org.jetbrains.anko.alert


const val RC_SIGN_IN = 1001
val TAG = LoginActivity::class.java.simpleName

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var mEmailField: TextInputEditText
    private lateinit var mPasswordField: TextInputEditText
    private lateinit var mSignUpButton: Button
    private lateinit var mSignInButton: Button
    private lateinit var mProgressBar: ProgressBar
    private lateinit var googleSignInButton: SignInButton
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initializeComponents()

        mAuth = FirebaseAuth.getInstance()

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.web_client_id))
                .requestEmail()
                .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this@LoginActivity, gso)

    }

    private fun initializeComponents(){
        mEmailField = findViewById(R.id.emailField)
        mPasswordField = findViewById(R.id.passwordField)
        mSignInButton = findViewById(R.id.signInButton)
        mSignUpButton = findViewById(R.id.signUpButton)
        googleSignInButton = findViewById(R.id.sign_in_with_google)
        mProgressBar = findViewById(R.id.progressBar)

        mSignUpButton.setOnClickListener(this)
        mSignInButton.setOnClickListener(this)
        googleSignInButton.setOnClickListener(this)

    }

    private fun signInWithGoogle(){
        val googleIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(googleIntent, RC_SIGN_IN)
    }

    private fun signInWithEmailAndPassword(email: String, password: String){
        mProgressBar.visibility = View.VISIBLE
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    mProgressBar.visibility = View.GONE
                    if (it.isSuccessful){

                        val mainActivityIntent = Intent(this@LoginActivity,
                                MainActivity::class.java)
                        mainActivityIntent.putExtra("displayName", mAuth.currentUser?.displayName)
                        mainActivityIntent.putExtra("email", mAuth.currentUser?.email)
                        startActivity(mainActivityIntent)
                        finish()
                    } else {
                        alert {
                            message = "Failed: ${it.exception?.message}"
                        }.show()
                    }
                }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            RC_SIGN_IN ->{
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)
                    firebaseAuthWithGoogle(account)
                } catch (e: ApiException) {
                    // Google Sign In failed, update UI appropriately
                    Log.w(TAG, "Google sign in failed", e)
                    // ...
                }

            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id)

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithCredential:success")
                        val user = mAuth.currentUser

                        val mainActivityIntent = Intent(this@LoginActivity, MainActivity::class.java)
                        mainActivityIntent.putExtra("displayName", mAuth.currentUser?.displayName)
                        mainActivityIntent.putExtra("email", mAuth.currentUser?.email)

                        startActivity(mainActivityIntent)
                        finish()
                    } else {

                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithCredential:failure", task.exception)
                        Snackbar.make(findViewById(R.id.loginConstraintLayout), "Authentication Failed.", Snackbar.LENGTH_SHORT).show()

                    }

                }
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.signInButton -> {
                val email = mEmailField.text.toString()
                val password = mPasswordField.text.toString()

                if (TextUtils.isEmpty(mEmailField.text)) mEmailField.error = "This field is required"
                if (TextUtils.isEmpty(mPasswordField.text)) mPasswordField.error = "This field is required"

                if (email.isNotEmpty() && password.isNotEmpty()){
                    signInWithEmailAndPassword(email, password)
                }
            }

            R.id.signUpButton -> {
                startActivity(Intent(this@LoginActivity, SignUpActivity::class.java))
                finish()
            }

            R.id.sign_in_with_google -> {
                signInWithGoogle()
            }
        }

    }

}
