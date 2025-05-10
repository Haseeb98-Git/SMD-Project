package com.haseebali.savelife

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.haseebali.savelife.models.User

class UserProfileActivity : AppCompatActivity() {
    private lateinit var tvName: TextView
    private lateinit var ivProfilePicture: ImageView
    private lateinit var tvRoles: TextView
    private lateinit var tvDonorAvailability: TextView
    private lateinit var btnSendMessage: Button
    private lateinit var btnSendAppointment: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        // Initialize views
        tvName = findViewById(R.id.tvName)
        ivProfilePicture = findViewById(R.id.ivProfilePicture)
        tvRoles = findViewById(R.id.tvRoles)
        tvDonorAvailability = findViewById(R.id.tvDonorAvailability)
        btnSendMessage = findViewById(R.id.btnSendMessage)
        btnSendAppointment = findViewById(R.id.btnSendAppointment)

        // Get user ID from intent
        val userId = intent.getStringExtra("userId") ?: return
        val currentUserId = intent.getStringExtra("currentUserId") ?: return

        // Load user data
        loadUserData(userId)

        // Set up button click listeners
        btnSendMessage.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("userName", tvName.text.toString())
            }
            startActivity(intent)
        }

        btnSendAppointment.setOnClickListener {
            // TODO: Implement send appointment request functionality
        }
    }

    private fun loadUserData(userId: String) {
        FirebaseDatabase.getInstance().getReference("users")
            .child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    user?.let {
                        // Set name
                        tvName.text = it.fullName

                        // Set profile picture
                        if (it.profilePicture.isNotEmpty()) {
                            Glide.with(this@UserProfileActivity)
                                .load(Constants.SERVER_IMAGES_URL + it.profilePicture)
                                .into(ivProfilePicture)
                        }

                        // Set roles
                        val roles = mutableListOf<String>()
                        if (it.roles?.donor == true) roles.add("Donor")
                        if (it.roles?.requester == true) roles.add("Requester")
                        tvRoles.text = "Roles: ${roles.joinToString(", ")}"

                        // Set donor availability if applicable
                        if (it.roles?.donor == true) {
                            tvDonorAvailability.visibility = View.VISIBLE
                            tvDonorAvailability.text = "Donor Availability: ${it.donorAvailability}"
                        } else {
                            tvDonorAvailability.visibility = View.GONE
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
} 