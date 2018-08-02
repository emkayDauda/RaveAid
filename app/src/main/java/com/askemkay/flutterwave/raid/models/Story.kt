package com.askemkay.flutterwave.raid.models

import android.support.annotation.Keep
import java.text.SimpleDateFormat
import java.util.*

// Created by ask_emkay on 8/1/18.
@Keep
data class Story(
        val suid: String = Random().nextInt().toString(),
        val length: String,
        val excerpt: String,
        val category: String,
        val title: String,
        val uploadedBy: String,
        val timestamp: String = SimpleDateFormat("EEE, MMM d, ''yy", Locale.ENGLISH)
                .format(Calendar.getInstance().time)
){
    //Empty Constructor required for firebase
    constructor():this(length = "", excerpt = "", category = "" , title = "", uploadedBy = "", timestamp = "")

//    constructor(excerpt: String, uploadedBy: String, timestamp: String?) : this()


}