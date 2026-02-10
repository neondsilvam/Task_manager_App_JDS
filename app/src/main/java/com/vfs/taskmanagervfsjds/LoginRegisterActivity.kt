package com.vfs.taskmanagervfsjds

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.vfs.taskmanagervfsjds.Cloud.Companion.db

class LoginRegisterActivity : AppCompatActivity() {

    lateinit var tvTitle: TextView
    lateinit var etUsername: EditText
    lateinit var etEmail: EditText
    lateinit var etPassword: EditText
    lateinit var btnAction: Button
    lateinit var btnSwitch: Button
    lateinit var tvStatus: TextView

    lateinit var ReturnBtn : Button
    lateinit var LogOutBtn : Button

    // Mode tracking
    var isRegisterMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_register)

        // Initialize Firebase Auth
        Cloud.auth = FirebaseAuth.getInstance()

        // Initialize views
        initializeViews()

        // Set up button listeners
        setupListeners()

        ReturnBtn = findViewById(R.id.ReturnTaskButton_id)
        ReturnBtn.setOnClickListener {
            val intent = Intent(this, DisplayTasks::class.java)
            startActivity(intent)
        }
    }

    fun initializeViews() {
        tvTitle = findViewById(R.id.tvTitle_id)
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnAction = findViewById(R.id.btnAction)
        btnSwitch = findViewById(R.id.btnSwitch)
        tvStatus = findViewById(R.id.tvStatus)
    }

    fun setupListeners() {
        btnSwitch.setOnClickListener {
            switchMode()
        }

        btnAction.setOnClickListener {
            if (isRegisterMode) {
                handleRegister()
            } else {
                handleLogin()
            }
        }

        LogOutBtn = findViewById(R.id.LogOutBtn_id)
        LogOutBtn.setOnClickListener {
            handleLogOut()
        }
    }

    fun handleLogOut(){
        if (Cloud.auth.currentUser != null) {
            Cloud.auth.signOut()
            tvStatus.text = "Logged out successfully"
        }
    }

    fun switchMode() {
        isRegisterMode = !isRegisterMode

        if (isRegisterMode) {
            tvTitle.text = "Register"
            btnAction.text = "Register"
            btnSwitch.text = "Switch to Login"
            etUsername.visibility = View.VISIBLE
            tvStatus.text = "Ready to register"
            clearFields()
        } else {
            tvTitle.text = "Login"
            btnAction.text = "Login"
            btnSwitch.text = "Switch to Register"
            etUsername.visibility = View.GONE
            tvStatus.text = "Ready to login"
            clearFields()
        }
    }

    fun handleRegister() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        when {
            username.isEmpty() -> tvStatus.text = "Please enter a username"
            email.isEmpty() -> tvStatus.text = "Please enter an email"
            password.isEmpty() -> tvStatus.text = "Please enter a password"
            !isValidEmail(email) -> tvStatus.text = "Please enter a valid email"
            password.length < 6 -> tvStatus.text = "Password must be at least 6 characters"
            else -> {
                Cloud.auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = task.result?.user
                            val mail = task.result?.user?.email
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(username)
                                .build()

                            user?.updateProfile(profileUpdates)

                            // Fix: Add the user to the database inside the success listener
                            user?.uid?.let { uid ->
                                db.reference
                                    .child("users")
                                    .child(uid)
                                    .child("username")
                                    .setValue(username)
                                    .addOnSuccessListener {
                                        tvStatus.text = "Registration successful for $username!"
                                    }
                                    .addOnFailureListener {
                                        tvStatus.text = "Failed to save user data: ${it.message}"
                                    }

                                db.reference
                                    .child("users")
                                    .child(uid)
                                    .child("email")
                                    .setValue(mail)
                                    .addOnFailureListener {
                                        tvStatus.text = "Failed to save user data: ${it.message}"
                                    }
                            }
                        } else {
                            tvStatus.text = "Registration failed: ${task.exception?.message}"
                        }
                    }
            }
        }
    }

    fun handleLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        when {
            email.isEmpty() -> tvStatus.text = "Please enter an email"
            password.isEmpty() -> tvStatus.text = "Please enter a password"
            !isValidEmail(email) -> tvStatus.text = "Please enter a valid email"
            else -> {
                Cloud.auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            tvStatus.text = "Login successful for $email!"
                        } else {
                            tvStatus.text = "Login failed: ${task.exception?.message}"
                        }
                    }
            }
        }
    }

    fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun clearFields() {
        etUsername.text.clear()
        etEmail.text.clear()
        etPassword.text.clear()
    }
}
