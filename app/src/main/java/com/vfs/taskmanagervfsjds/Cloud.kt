package com.vfs.taskmanagervfsjds

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.database

class Cloud {

    companion object
    {
        lateinit var auth: FirebaseAuth
        val db = Firebase.database

    }
}