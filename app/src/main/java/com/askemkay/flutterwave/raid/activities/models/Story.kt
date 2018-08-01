package com.askemkay.flutterwave.raid.activities.models

import com.google.firebase.firestore.ServerTimestamp

// Created by ask_emkay on 8/1/18.
data class Story(
        val length: String,
        val excerpt: String,
        val  category: String,
        val topic: String,
        @ServerTimestamp val timestamp: ServerTimestamp?
){
    constructor():this("", "", "" ,"", null)
}