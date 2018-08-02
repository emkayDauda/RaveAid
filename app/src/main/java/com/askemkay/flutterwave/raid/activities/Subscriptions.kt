package com.askemkay.flutterwave.raid.activities

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import com.askemkay.flutterwave.raid.R
import com.flutterwave.raveandroid.RavePayManager

import kotlinx.android.synthetic.main.activity_subscriptions.*
import org.jetbrains.anko.AlertBuilder
import org.jetbrains.anko.alert
import java.util.*
import android.R.attr.data
import android.support.constraint.ConstraintLayout
import android.view.View
import android.widget.*
import com.flutterwave.raveandroid.RavePayActivity
import com.flutterwave.raveandroid.RaveConstants
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.jetbrains.anko.toast


class Subscriptions : AppCompatActivity() {

    companion object {
        val USER_STORIES_LEFT = "user_reads"
    }

    private lateinit var subscribeButton: Button
    private lateinit var subscriptionSpinner: Spinner
    private lateinit var priceTextView: TextView
    private lateinit var confirmPayAlert: DialogInterface

    private lateinit var emailForFirebase: String
    private lateinit var name: String
    private lateinit var email: String

    var selected = "One"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscriptions)
        setSupportActionBar(toolbar)
selected
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initComponents()
        savecurrentSub()
    }

    private fun initComponents(){
        subscribeButton = findViewById(R.id.subscribe)
        subscriptionSpinner = findViewById(R.id.subscriptionsSpinner)
        priceTextView = findViewById(R.id.price_text_view)

        val priceAdapter = ArrayAdapter.createFromResource(this@Subscriptions,
                R.array.subscriptions, android.R.layout.simple_spinner_dropdown_item)

        subscriptionSpinner.adapter = priceAdapter
        subscriptionSpinner.setSelection(0)


        name = getSharedPreferences(MainActivity.PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getString(MainActivity.USER_NAME, "")
        email = getSharedPreferences(MainActivity.PREFERENCE_NAME, Context.MODE_PRIVATE)
                .getString(MainActivity.USER_EMAIL, "")

        emailForFirebase = email.filter { it != '.' }

        var price: String = priceTextView.text.toString()

        subscriptionSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                toast("Nothing selected...").show()
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selected = subscriptionSpinner.selectedItem.toString()
                when(selected){
                    "One" -> {priceTextView.text = getString(R.string.price_of_one)}
                    "Three" -> {priceTextView.text = getString(R.string.price_of_three)}
                    "Five" -> {priceTextView.text = getString(R.string.price_of_five)}
                    "Ten" -> {priceTextView.text = getString(R.string.price_of_five)}
                }

                price = priceTextView.text.toString()


            }

        }

        subscribeButton.setOnClickListener {
            confirmPayAlert = alert {
                title = "Confirm"
                message = "You are about to be charged $price for $selected " +
                        if (selected == "One") "story" else "stories"

                positiveButton("OK"){
                    val debitAmount = price.split("[ ]+".toRegex())[0].toDouble()

                    val names = name.split("[ ]+".toRegex())
                    val raver = RavePayManager(this@Subscriptions).setAmount(debitAmount)
                            .setCountry("NG")
                            .setCurrency("NGN")
                            .setEmail(email)
                            .setfName(names.first())
                            .setlName(names.last())
                            .setNarration("RAid Charge for stories purchase(Qty: $selected)")
                            .setPublicKey("FLWPUBK-205ee6547a9929d31f98529ca4f61b66-X")
                            .setSecretKey("FLWSECK-394293d71e229e59b0398836ff5b1124-X")
                            .setTxRef((Random().nextInt(9999) + 10000).toString())
                            .acceptAccountPayments(true)
                            .acceptCardPayments(true)
                            .acceptMpesaPayments(false)
                            .acceptGHMobileMoneyPayments(false)
                            .onStagingEnv(true)
//                            .withTheme(styleId)
                            .initialize()
                }
                negativeButton("Cancel"){
                    confirmPayAlert.dismiss()
                }
            }.show()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RaveConstants.RAVE_REQUEST_CODE && data != null) {
            val message = data.getStringExtra("response")
            when (resultCode) {
                RavePayActivity.RESULT_SUCCESS -> {
//                    Toast.makeText(this, "SUCCESS $message", Toast.LENGTH_SHORT).show()
                    val oldSub = getSharedPreferences(MainActivity.PREFERENCE_NAME, Context.MODE_PRIVATE)
                            .getString(USER_STORIES_LEFT, "")

                    if (!oldSub.isNullOrEmpty()){
                        try {
                            toast("oldSub: $oldSub")
                            val oldValue = oldSub.toInt()
                            val newSub = oldValue + when(selected){
                                "One" -> 1
                                "Three" -> 3
                                "Five" -> 5
                                "Ten" -> 10
                                else -> 0
                            }
                            FirebaseDatabase.getInstance().reference
                                    .child("users")
                                    .child(emailForFirebase)
                                    .child("subscriptions").setValue(newSub).addOnCompleteListener {
                                        if (it.isSuccessful) {
                                            toast("Subscription added to your account").show()
                                            savecurrentSub()
                                        }
                                        else{
                                            toast("Failed: ${it.exception?.message}").show()
                                        }
                                    }
                        } catch (a: NumberFormatException){
                            toast("Failed to parse: ${a.message}").show()
                        }
                    }

                }
                RavePayActivity.RESULT_ERROR -> Toast.makeText(this, "ERROR $message", Toast.LENGTH_SHORT).show()
                RavePayActivity.RESULT_CANCELLED -> Toast.makeText(this, "CANCELLED $message", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun savecurrentSub(){
        FirebaseDatabase.getInstance().reference
                .child("users")
                .child(emailForFirebase)
                .child("subscriptions").ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                toast("Cancelled an operation...")
            }

            override fun onDataChange(p0: DataSnapshot) {
                val value = p0.value.toString()
                toast("Data changed: ${p0.getValue(Int::class.java)}")
                getSharedPreferences(MainActivity.PREFERENCE_NAME, Context.MODE_PRIVATE)
                        .edit().putString(USER_STORIES_LEFT, value).commit()
            }
        })
    }

}
