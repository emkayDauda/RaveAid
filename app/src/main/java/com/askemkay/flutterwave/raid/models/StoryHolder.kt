package com.askemkay.flutterwave.raid.models

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView
import com.askemkay.flutterwave.raid.R

// Created by ask_emkay on 8/1/18.
class StoryHolder(v: View, listener: RecyclerViewClickListenerInterface): RecyclerView.ViewHolder(v), View.OnClickListener{
    override fun onClick(v: View) {
        v.findViewById<TextView>(R.id.excerpt)
        mListenerInterface.onClick(v, adapterPosition)
    }

    private var mListenerInterface = listener

    val timeStamp: TextView = v.findViewById(R.id.date)
    val length: TextView = v.findViewById(R.id.length)
    val excerpt: TextView = v.findViewById(R.id.excerpt)
    val category: TextView = v.findViewById(R.id.category)
    val uploadedBy: TextView = v.findViewById(R.id.uploaded_by)
    val pushValue:TextView = v.findViewById(R.id.pushValue)

    init {
        v.setOnClickListener(this)
    }

}

interface RecyclerViewClickListenerInterface{
    fun onClick(v:View, position: Int)
}