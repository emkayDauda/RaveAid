package com.askemkay.flutterwave.raid.activities

import android.content.Context
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import com.askemkay.flutterwave.raid.R
import com.askemkay.flutterwave.raid.models.Story
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

import kotlinx.android.synthetic.main.activity_add_story.*
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

class AddStory : AppCompatActivity() {

    private lateinit var storyText: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var title: EditText
    private lateinit var topic: EditText
    private lateinit var rootRef: DatabaseReference
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_story)
        setSupportActionBar(toolbar)

        initComponents()
        fab.setOnClickListener { view -> addStory(view) }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun addStory(view: View) {
        progressBar.visibility = View.VISIBLE
        val storyBody = storyText.text.toString()
        val category = categorySpinner.selectedItem.toString()
        val topic = topic.text.toString()
        val storyTitle = title.text.toString()

        val currentDate = SimpleDateFormat("EEE, MMM d, ''yy", Locale.ENGLISH)
                .format(Calendar.getInstance().time)

        if (storyBody.isNotEmpty() && storyTitle.isNotEmpty()) {
            var length = 0.0
            //for each word, assume a reading time of 0.01 minutes (0.6 seconds)
            storyBody.split("[ ]+".toRegex()).forEach { length += 0.01 }
            val userEmail = getSharedPreferences(MainActivity.PREFERENCE_NAME, Context.MODE_PRIVATE)
                    .getString(MainActivity.USER_EMAIL, "")

            val userName = getSharedPreferences(MainActivity.PREFERENCE_NAME, Context.MODE_PRIVATE)
                    .getString(MainActivity.USER_NAME, "")

            val story = Story(
                    excerpt = storyBody,
                    title = storyTitle,
                    length = if (length < 1.0) "<1 minute read" else "${length.roundToInt()} minutes read",
                    category = category + if (topic.isNotEmpty()) "($topic)" else "",
                    uploadedBy = userName,
                    timestamp = currentDate
            )

            storyText.setText("")
            title.setText("")



            //Remove punctuation before using as firebase reference.
            val userEmailForFirebase = userEmail.filter { it != '.' }
            if (userEmail.isNotEmpty()){
                rootRef.child("general")
                        .child(if (story.category == "Poem") "poems" else "stories")
                        .child(story.suid).setValue(story)

                rootRef.child("users")
                        .child(userEmailForFirebase)
                        .child(if (story.category == "Poem") "poems" else "stories")
                        .child(story.suid)
                        .setValue(story)
                        .addOnCompleteListener {
                            if (it.isSuccessful) {
                                progressBar.visibility = View.GONE
                                Snackbar.make(view, "Story Added", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show()
                            } else {
                                Snackbar.make(view, "Failed...", Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show()
                            }
                        }

                rootRef.child("users")
                        .child(userEmailForFirebase)
                        .child("storiesBought")
                        .child(story.suid)
                        .setValue("purchased")
            } else {
                longToast(getString(R.string.error_unidentified_user))
            }
        } else {
            progressBar.visibility = View.GONE
            if (storyBody.isEmpty()) storyText.error = "Required"
            if (storyTitle.isEmpty()) title.error = "Required"
        }

    }

    private fun initComponents() {
        storyText = findViewById(R.id.new_entry_edit_text)
        categorySpinner = findViewById(R.id.categorySpinner)
        topic = findViewById(R.id.topic)
        title = findViewById(R.id.story_title)
        progressBar = findViewById(R.id.addStoryProgressBar)
        topic.gravity = Gravity.CENTER

        categorySpinner.gravity = Gravity.CENTER

        val categoryAdapter = ArrayAdapter.createFromResource(this@AddStory,
                R.array.categories, android.R.layout.simple_spinner_dropdown_item)

        categorySpinner.adapter = categoryAdapter
        categorySpinner.setSelection(0)

        rootRef = FirebaseDatabase.getInstance().reference
    }

}
