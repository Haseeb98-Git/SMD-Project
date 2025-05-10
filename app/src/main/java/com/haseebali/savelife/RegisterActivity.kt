package com.haseebali.savelife

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.haseebali.savelife.R
import com.haseebali.savelife.models.Roles
import com.haseebali.savelife.models.User
import java.text.SimpleDateFormat
import java.util.*

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val fullNameEditText = findViewById<TextInputEditText>(R.id.fullNameEditText)
        val usernameEditText = findViewById<TextInputEditText>(R.id.usernameEditText)
        val emailEditText = findViewById<TextInputEditText>(R.id.emailEditText)
        val passwordEditText = findViewById<TextInputEditText>(R.id.passwordEditText)
        val registerButton = findViewById<MaterialButton>(R.id.registerButton)
        val loginTextView = findViewById<android.widget.TextView>(R.id.loginTextView)

        registerButton.setOnClickListener {
            val fullName = fullNameEditText.text.toString()
            val username = usernameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (fullName.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.let { firebaseUser ->
                            // Create user object
                            val newUser = User(
                                uid = firebaseUser.uid,
                                fullName = fullName,
                                username = username,
                                email = email,
                                roles = Roles(),
                                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                                    .format(Date())
                            )

                            // Save user to database
                            database.reference.child("users")
                                .child(firebaseUser.uid)
                                .setValue(newUser)
                                .addOnSuccessListener {
                                    // Sign out the user after registration
                                    auth.signOut()
                                    // Show success message
                                    Toast.makeText(this, "Registration successful! Please login.", Toast.LENGTH_SHORT).show()
                                    // Go to login screen
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Failed to save user data: ${e.message}",
                                        Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
        }

        loginTextView.setOnClickListener {
            finish()
        }
    }
} 