package com.askemkay.flutterwave.raid

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import com.askemkay.flutterwave.raid.activities.LoginActivity
import com.askemkay.flutterwave.raid.models.RecyclerViewClickListenerInterface
import com.askemkay.flutterwave.raid.models.Story
import com.askemkay.flutterwave.raid.models.StoryHolder
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import org.jetbrains.anko.alert
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.FirebaseDatabase
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import java.util.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
RecyclerViewClickListenerInterface{
    override fun onClick(v: View, position: Int) {

        toast(v.findViewById<TextView>(R.id.pushValue).text.toString()).show()
    }

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mGoogleApiClient: GoogleApiClient
    private lateinit var context: Context

    private lateinit var userDisplayName: TextView
    private lateinit var userEmail: TextView
    private lateinit var imageView: ImageView
    private lateinit var storiesList: RecyclerView
    private lateinit var realAdapter: FirebaseRecyclerAdapter<Story, StoryHolder>
    private lateinit var viewManager: RecyclerView.LayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        initComponents()
        setUserDetailsInNav()

        populateRecyclerView("")
    }

    private fun initComponents() {
        context = this@MainActivity

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        mAuth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()

        mGoogleApiClient.connect()

        storiesList = findViewById<RecyclerView>(R.id.storiesList).apply {
            setHasFixedSize(true)

            viewManager = LinearLayoutManager(context)
            layoutManager = viewManager


//            adapter = realAdapter
        }

        val headerView = findViewById<NavigationView>(R.id.nav_view).getHeaderView(0)
        userDisplayName = headerView.findViewById(R.id.user_display_name_tv)
        userEmail = headerView.findViewById(R.id.user_email_tv)
        imageView = headerView.findViewById(R.id.imageView)
    }

    private fun populateRecyclerView(collectionName: String){
//        val query = FirebaseFirestore.getInstance()
//                .collection(collectionName)
//                .orderBy("timestamp")
//                .limit(50)

        val realQuery = FirebaseDatabase.getInstance().reference
                .child("stories")
                .limitToLast(50)

//        val options = FirestoreRecyclerOptions.Builder<Story>()
//                .setQuery(query, Story::class.java)
//                .build()

        val realOptions = FirebaseRecyclerOptions.Builder<Story>()
                .setQuery(realQuery, Story::class.java)
                .build()


        realAdapter = object: FirebaseRecyclerAdapter<Story, StoryHolder>(realOptions){
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryHolder {
                val view = LayoutInflater.from(context).inflate(R.layout.story_layout, parent, false)

                return  StoryHolder(view, this@MainActivity)
            }

            override fun onBindViewHolder(holder: StoryHolder, position: Int, model: Story) {
                holder.category.text = model.category
                holder.excerpt.text = model.excerpt
                holder.length.text = model.length
                holder.timeStamp.text = model.timestamp
                holder.uploadedBy.text = model.uploadedBy
                holder.pushValue.text = model.suid
                Log.e("Bind", "${model.category}: ${model.suid}")
            }

            override fun onDataChanged() {
                super.onDataChanged()
                realAdapter.notifyDataSetChanged()
            }


//            override fun getItemCount() = realAdapter.itemCount
        }




        realAdapter.notifyDataSetChanged()
        storiesList.adapter = realAdapter

        val story = Story(
                suid = Random().nextInt().toString(),
                length = "A length",
                excerpt = "An Excerpt",
                category = "A Category",
                uploadedBy = userDisplayName.text.toString()
        )
        FirebaseDatabase.getInstance().getReference("stories").child(story.suid.toString())
                .setValue(story).addOnCompleteListener {
                    if(!it.isSuccessful) {
                        longToast("Failed to Add Story\n${it.exception?.message}").show()
                    }
                }
    }

    override fun onStart() {
        super.onStart()
        realAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        realAdapter.stopListening()
    }

    private fun setUserDetailsInNav() {
        userDisplayName.text = mAuth.currentUser?.displayName ?: context.getString(R.string.unidentified_user)
        userEmail.text = mAuth.currentUser?.email ?: context.getString(R.string.unidentified_email)

        val photoUrl = mAuth.currentUser?.photoUrl

        /*if (photoUrl != null) {
            Glide.with(context)
                    .load(photoUrl)
                    .centerCrop()
                    .crossFade()
                    .into(imageView)
        }*/
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.add_account -> {
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            }
            R.id.view_accounts -> {

            }

            R.id.sign_out -> {
                alert {
                    title = "Sure to sign out?"
                    message = "You are about to exit the app"
                    positiveButton("Exit") {
                        AuthUI.getInstance()
                                .signOut(this@MainActivity)
                                .addOnCompleteListener {
                                    // user is now signed out
                                    finish()
                                }
                    }
                    negativeButton("Just Sign Out") {
                        AuthUI.getInstance()
                                .signOut(this@MainActivity)
                                .addOnCompleteListener {
                                    // user is now signed out
                                    startActivity(Intent(this@MainActivity, SplashActivity::class.java))
                                    finish()
                                }
                    }

                    neutralPressed("Cancel") {
                        it.dismiss()
                    }
                }.show()
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
