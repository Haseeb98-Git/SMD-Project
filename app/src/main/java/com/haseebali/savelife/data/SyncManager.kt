package com.haseebali.savelife.data

import android.content.Context
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SyncManager(private val context: Context) {
    private val TAG = "SyncManager"
    private val dbHelper = DatabaseHelper(context)
    private val connectivityManager = ConnectivityManager(context)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    init {
        // Monitor network connectivity and sync when online
        coroutineScope.launch {
            connectivityManager.isNetworkAvailable.collect { isAvailable ->
                if (isAvailable) {
                    Log.d(TAG, "Network available, starting sync")
                    syncPendingData()
                }
            }
        }
    }

    // Sync all pending data when online
    private fun syncPendingData() {
        coroutineScope.launch(Dispatchers.IO) {
            syncPendingAppointments()
            syncPendingDonorRegistrations()
            syncPendingRequesterRegistrations()
        }
    }

    // Sync pending appointments
    private fun syncPendingAppointments() {
        val pendingAppointments = dbHelper.getPendingAppointments()
        if (pendingAppointments.isEmpty()) return

        val database = FirebaseDatabase.getInstance()
        val appointmentsRef = database.getReference("appointments")

        for ((id, appointmentData) in pendingAppointments) {
            appointmentsRef.push().setValue(appointmentData)
                .addOnSuccessListener {
                    dbHelper.markAppointmentSynced(id)
                    Log.d(TAG, "Successfully synced appointment $id")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to sync appointment $id: ${e.message}")
                }
        }
    }

    // Sync pending donor registrations
    private fun syncPendingDonorRegistrations() {
        val pendingRegistrations = dbHelper.getPendingDonorRegistrations()
        if (pendingRegistrations.isEmpty()) return

        val database = FirebaseDatabase.getInstance()
        val registrationsRef = database.getReference("donorRegistrations")

        for ((id, data) in pendingRegistrations) {
            val (userId, registration) = data
            registrationsRef.child(userId).setValue(registration)
                .addOnSuccessListener {
                    dbHelper.markDonorRegistrationSynced(id)
                    Log.d(TAG, "Successfully synced donor registration $id")
                    
                    // Also update user roles if needed
                    updateUserRole(userId, true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to sync donor registration $id: ${e.message}")
                }
        }
    }

    // Sync pending requester registrations
    private fun syncPendingRequesterRegistrations() {
        val pendingRegistrations = dbHelper.getPendingRequesterRegistrations()
        if (pendingRegistrations.isEmpty()) return

        val database = FirebaseDatabase.getInstance()
        val registrationsRef = database.getReference("requesterRegistrations")

        for ((id, data) in pendingRegistrations) {
            val (userId, registration) = data
            registrationsRef.child(userId).setValue(registration)
                .addOnSuccessListener {
                    dbHelper.markRequesterRegistrationSynced(id)
                    Log.d(TAG, "Successfully synced requester registration $id")
                    
                    // Also update user roles if needed
                    updateUserRole(userId, false)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to sync requester registration $id: ${e.message}")
                }
        }
    }

    // Update user role in Firebase
    private fun updateUserRole(userId: String, isDonor: Boolean) {
        val database = FirebaseDatabase.getInstance()
        val userRef = database.getReference("users").child(userId)
        
        userRef.child("roles").get().addOnSuccessListener { snapshot ->
            val roles = snapshot.getValue(com.haseebali.savelife.models.Roles::class.java) ?: com.haseebali.savelife.models.Roles()
            val updatedRoles = if (isDonor) {
                roles.copy(donor = true)
            } else {
                roles.copy(requester = true)
            }
            userRef.child("roles").setValue(updatedRoles)
        }
    }

    // Clean up resources
    fun onDestroy() {
        connectivityManager.unregisterNetworkCallback()
    }
} 