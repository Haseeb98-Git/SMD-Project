package com.haseebali.savelife

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.haseebali.savelife.models.DonorRegistration
import com.haseebali.savelife.models.RequesterRegistration

class DonationDetailsActivity : AppCompatActivity() {
    private lateinit var tvTitle: TextView
    private lateinit var tvBloodType: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvDescription: TextView
    private lateinit var btnViewProfile: Button
    private lateinit var btnSendMessage: Button
    private lateinit var btnSendAppointment: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_donation_details)

        // Initialize views
        tvTitle = findViewById(R.id.tvTitle)
        tvBloodType = findViewById(R.id.tvBloodType)
        tvLocation = findViewById(R.id.tvLocation)
        tvAddress = findViewById(R.id.tvAddress)
        tvStatus = findViewById(R.id.tvStatus)
        tvDescription = findViewById(R.id.tvDescription)
        btnViewProfile = findViewById(R.id.btnViewProfile)
        btnSendMessage = findViewById(R.id.btnSendMessage)
        btnSendAppointment = findViewById(R.id.btnSendAppointment)

        // Get data from intent
        val userId = intent.getStringExtra("userId") ?: return
        val isDonor = intent.getBooleanExtra("isDonor", false)
        val currentUserId = intent.getStringExtra("currentUserId") ?: return

        // Set title based on type
        tvTitle.text = if (isDonor) "Donor Details" else "Blood Request Details"

        // Load data from Firebase
        if (isDonor) {
            loadDonorDetails(userId)
        } else {
            loadRequesterDetails(userId)
        }

        // Set up button click listeners
        btnViewProfile.setOnClickListener {
            // TODO: Implement view profile functionality
        }

        btnSendMessage.setOnClickListener {
            // TODO: Implement send message functionality
        }

        btnSendAppointment.setOnClickListener {
            // TODO: Implement send appointment request functionality
        }
    }

    private fun loadDonorDetails(userId: String) {
        FirebaseDatabase.getInstance().getReference("donorRegistrations")
            .child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val donorRegistration = snapshot.getValue(DonorRegistration::class.java)
                    donorRegistration?.let { donor ->
                        tvBloodType.text = "Blood Type: ${donor.bloodType}"
                        tvLocation.text = "Location: ${donor.city}, ${donor.country}"
                        tvAddress.text = "Address: ${donor.address}"
                        tvStatus.text = "Health Status: ${donor.healthStatus}"
                        tvDescription.text = "Description: ${donor.description}"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun loadRequesterDetails(userId: String) {
        FirebaseDatabase.getInstance().getReference("requesterRegistrations")
            .child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val requesterRegistration = snapshot.getValue(RequesterRegistration::class.java)
                    requesterRegistration?.let { requester ->
                        tvBloodType.text = "Blood Type: ${requester.bloodType}"
                        tvLocation.text = "Location: ${requester.city}, ${requester.country}"
                        tvAddress.text = "Address: ${requester.address}"
                        tvStatus.text = "Urgency: ${requester.urgency}"
                        tvDescription.text = "Description: ${requester.description}"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }
} 