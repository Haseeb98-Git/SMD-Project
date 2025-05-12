package com.haseebali.savelife

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.haseebali.savelife.models.User
import com.google.android.material.textfield.TextInputEditText
import android.app.DatePickerDialog
import android.app.Dialog
import com.haseebali.savelife.models.Appointment
import com.haseebali.savelife.models.Roles
import java.time.Instant
import java.util.*

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
            showAppointmentRequestDialog()
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

    private fun showAppointmentRequestDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_appointment_request)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val etCountry = dialog.findViewById<TextInputEditText>(R.id.etCountry)
        val etCity = dialog.findViewById<TextInputEditText>(R.id.etCity)
        val etVenue = dialog.findViewById<TextInputEditText>(R.id.etVenue)
        val etDate = dialog.findViewById<TextInputEditText>(R.id.etDate)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnSend = dialog.findViewById<Button>(R.id.btnSend)

        // Get current user's role
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val targetUserId = intent.getStringExtra("userId") ?: return

        FirebaseDatabase.getInstance().getReference("users")
            .child(currentUserId)
            .child("roles")
            .get()
            .addOnSuccessListener { snapshot ->
                val roles = snapshot.getValue(Roles::class.java) ?: Roles()
                
                // Determine donor and requester IDs based on roles
                val (donorId, requesterId) = if (roles.donor) {
                    Pair(currentUserId, targetUserId)
                } else if (roles.requester) {
                    Pair(targetUserId, currentUserId)
                } else {
                    Toast.makeText(this, "You must be either a donor or requester to create appointments", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    return@addOnSuccessListener
                }

                btnSend.setOnClickListener {
                    val country = etCountry.text.toString()
                    val city = etCity.text.toString()
                    val venue = etVenue.text.toString()
                    val date = etDate.text.toString()

                    if (country.isEmpty() || city.isEmpty() || venue.isEmpty() || date.isEmpty()) {
                        Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val appointmentData = hashMapOf<String, Any>(
                        "donorId" to donorId,
                        "requesterId" to requesterId,
                        "createdBy" to currentUserId,
                        "country" to country,
                        "city" to city,
                        "venue" to venue,
                        "date" to date,
                        "status" to "pending",
                        "createdAt" to Date().time.toString()
                    )

                    val appointmentRef = FirebaseDatabase.getInstance().getReference("appointments").push()
                    appointmentRef.setValue(appointmentData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Appointment request sent successfully", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error sending appointment request", Toast.LENGTH_SHORT).show()
                        }
                }
            }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
} 