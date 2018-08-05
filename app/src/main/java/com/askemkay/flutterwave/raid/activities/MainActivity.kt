package com.askemkay.flutterwave.raid.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import com.askemkay.flutterwave.raid.R
import com.askemkay.flutterwave.raid.models.RecyclerViewClickListenerInterface
import com.askemkay.flutterwave.raid.models.Story
import com.askemkay.flutterwave.raid.models.StoryHolder
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import me.zhanghai.android.materialprogressbar.MaterialProgressBar
import org.jetbrains.anko.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
RecyclerViewClickListenerInterface{

    companion object {
        const val PREFERENCE_NAME = "raid"
        const val USER_NAME = "user_display_name"
        const val USER_EMAIL = "user_email"

    }
    override fun onClick(v: View, position: Int) {
        val sUid = v.findViewById<TextView>(R.id.pushValue).text.toString()
        val sOwner = v.findViewById<TextView>(R.id.uploaded_by).text.toString()
        val email = userEmail.text.toString().filter { it != '.' }
        var category = v.findViewById<TextView>(R.id.category).text.toString()
        val sTitle = v.findViewById<TextView>(R.id.title).text.toString()

        //change value of category to be used as child node in database request
        category = if (category == "Poem") "poems" else "stories"

        /*Check if story has been purchased by user. If not, purchase it...*/
        rootRef.child("users").child(email).child("storiesBought").child(sUid)
                .addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                        toast("A transaction was cancelled...").show()
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        val exists = p0.getValue(String::class.java)
                        if (exists != null){
                            showStory(sUid, category)
                        } else{
                            alert {
                                title = "Make a purchase?"
                                message = "Do you want to purchase \"$sTitle\" by $sOwner?"

                                positiveButton("OK"){
                                    chargeSubscriptions(email, sUid, category, true)
                                }

                                noButton {}
                            }.show()

                        }
                    }
                })
    }

    private fun showStory(sUid: String, category: String) {
        rootRef.child("general").child(category).child(sUid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {
                        toast("")
                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        val story = p0.getValue(Story::class.java)
                        if (story != null) {
                            alert {
                                title = story.title
                                message = story.excerpt
                            }.show()
                        }
                    }
                })
        alert {

        }
    }

    fun chargeSubscriptions(email: String, sUid: String, category: String, showStory: Boolean = false){
        val currentSub = preferences.getString(Subscriptions.USER_STORIES_LEFT, "")

        if (currentSub.isNotEmpty()) {
            val currentSubValue = currentSub.toInt()
            if (currentSubValue > 0){
                rootRef.child("users").child(email).child("storiesBought")
                        .child(sUid)
                        .setValue("purchased")
                        .addOnCompleteListener {
                            if(it.isSuccessful){
                                preferences.edit().putString(Subscriptions.USER_STORIES_LEFT,
                                        (currentSubValue - 1).toString()).apply()

                                rootRef.child("users")
                                        .child(email)
                                        .child("subscriptions")
                                        .setValue((currentSubValue - 1).toString())

                                //If user has tried to view a story before, show that story after successful charge
                                if (showStory) showStory(sUid, category)
                                longToast("Story purchased. You have${currentSubValue - 1} subscriptions left").show()
                            }
                        }
            } else{
                alert {
                    title = "Oops"
                    message = "You don't have an active subscription, but we can fix that..."

                    positiveButton("Buy Now"){
                        startActivity(Intent(this@MainActivity, Subscriptions::class.java))
                    }
                    cancelButton {}
                }.show()

            }
        }
    }

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mGoogleApiClient: GoogleApiClient
    private lateinit var context: Context
    private lateinit var rootRef: DatabaseReference

    private lateinit var userDisplayName: TextView
    private lateinit var userEmail: TextView
    private lateinit var imageView: ImageView
    private lateinit var storiesList: RecyclerView
    private lateinit var realAdapter: FirebaseRecyclerAdapter<Story, StoryHolder>
    private lateinit var poemsAdapter: FirebaseRecyclerAdapter<Story, StoryHolder>
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var progressBar: MaterialProgressBar
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        initComponents()
        setUserDetailsInNav()

        /*Get Current Subscriptions*/

        populateRecyclerView("stories")

        fab.setOnClickListener {
            startActivity(Intent(this@MainActivity, AddStory::class.java))
        }

        saveCurrentSub(preferences.getString(USER_EMAIL, ""))
    }

    private fun initComponents() {
        context = this@MainActivity

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)

        rootRef = FirebaseDatabase.getInstance().reference

        mAuth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()

        mGoogleApiClient.connect()

        progressBar = findViewById(R.id.loadingProgressBar)
        storiesList = findViewById<RecyclerView>(R.id.storiesList).apply {
            setHasFixedSize(true)

            viewManager = LinearLayoutManager(context)
            layoutManager = viewManager


//            adapter = realAdapter
        }

        storiesList.addOnScrollListener(object: RecyclerView.OnScrollListener() {

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                //Make Fab transparent when recycler view is at the last item
                if(!recyclerView.canScrollVertically(1)){
                    fab.alpha = 0.5f
                }
                else fab.alpha =1.0f
            }
        })



        val headerView = findViewById<NavigationView>(R.id.nav_view).getHeaderView(0)
        userDisplayName = headerView.findViewById(R.id.user_display_name_tv)
        userEmail = headerView.findViewById(R.id.user_email_tv)
        imageView = headerView.findViewById(R.id.imageView)
    }

    private fun populateRecyclerView(category: String){

//        storiesList.adapter = null
        val realQuery = rootRef
                .child("general")
                .child(category)
                .limitToLast(100)

        val poemsQuery = rootRef
                .child("general")
                .child("poems")
                .limitToLast(100)

//        val options = FirestoreRecyclerOptions.Builder<Story>()
//                .setQuery(query, Story::class.java)
//                .build()

        val realOptions = FirebaseRecyclerOptions.Builder<Story>()
                .setQuery(realQuery, Story::class.java)
                .build()

        val poemsOptions = FirebaseRecyclerOptions.Builder<Story>()
                .setQuery(poemsQuery, Story::class.java)
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
                holder.title.text = model.title

                Log.e("Bind", "${model.category}: ${model.suid}")
            }

            override fun onDataChanged() {
                super.onDataChanged()
                realAdapter.notifyDataSetChanged()
            }

        }
        poemsAdapter = object: FirebaseRecyclerAdapter<Story, StoryHolder>(poemsOptions){
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
                holder.title.text = model.title

                Log.e("Bind", "${model.category}: ${model.suid}")
            }

            override fun onDataChanged() {
                super.onDataChanged()
                realAdapter.notifyDataSetChanged()
            }

        }




        realAdapter.notifyDataSetChanged()
        storiesList.adapter = realAdapter

    }

    override fun onStart() {
        super.onStart()
        realAdapter.startListening()
        poemsAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        realAdapter.stopListening()
        poemsAdapter.stopListening()
    }

    private fun setUserDetailsInNav() {
        val userName = mAuth.currentUser?.displayName
        val userMail = mAuth.currentUser?.email
        userDisplayName.text = userName ?: context.getString(R.string.unidentified_user)
        userEmail.text = userMail ?: context.getString(R.string.unidentified_email)

        preferences.edit().putString(USER_NAME, mAuth.currentUser?.displayName).apply()
        preferences.edit().putString(USER_EMAIL, mAuth.currentUser?.email).apply()

        checkRegisteredEmails(userMail?.filter { it != '.' })

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
            R.id.view_stories -> {
                storiesList.adapter = realAdapter
                return true
            }

            R.id.view_poems -> {
                storiesList.adapter = poemsAdapter
                realAdapter.notifyDataSetChanged()
                return true
            }
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

            R.id.subscriptions -> {
                startActivity(Intent(this@MainActivity, Subscriptions::class.java))
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

    private fun checkRegisteredEmails(email: String?){
        if (email != null) {
            rootRef
                    .child("allEmails")
                    .child(email)
                    .addListenerForSingleValueEvent(object: ValueEventListener {
                        override fun onCancelled(p0: DatabaseError) {
                            toast("A transaction was cancelled...").show()
                        }

                        override fun onDataChange(p0: DataSnapshot) {
                            val userActive = p0.getValue(String::class.java)
                            if (userActive == null){
                                bestowFreebiesUponUser(email)
                            } else {

                            }
                        }
                    })
        }
    }

    private fun bestowFreebiesUponUser(email: String) {
        rootRef
                .child("users")
                .child(email)
                .child("subscriptions")
                .setValue(2)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        rootRef
                                .child("allEmails")
                                .child(email)
                                .setValue("Active")
                                .addOnCompleteListener {
                                    if (it.isSuccessful) {
                                        longToast("Congratulations, you have been given two free stories...")
                                    }

                                }
                    }
                    else bestowFreebiesUponUser(email)
                }
    }

    fun saveCurrentSub(emailForFirebase: String){
        FirebaseDatabase.getInstance().reference
                .child("users")
                .child(emailForFirebase.filter { it != '.' })
                .child("subscriptions").ref.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                toast("Cancelled an operation...")
            }

            override fun onDataChange(p0: DataSnapshot) {
                val value = p0.value.toString()
//                toast("Data changed: ${p0.getValue(Int::class.java)}")
                getSharedPreferences(MainActivity.PREFERENCE_NAME, Context.MODE_PRIVATE)
                        .edit().putString(Subscriptions.USER_STORIES_LEFT, value).commit()
            }
        })
    }
}
