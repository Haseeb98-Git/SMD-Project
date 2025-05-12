package com.haseebali.savelife.ui.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.haseebali.savelife.SaveLifeApplication
import com.haseebali.savelife.databinding.FragmentDonorRegistrationBinding
import com.haseebali.savelife.models.DonorRegistration
import com.haseebali.savelife.models.Roles
import java.util.Date

class DonorRegistrationFragment : Fragment() {
    private var _binding: FragmentDonorRegistrationBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var app: SaveLifeApplication

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDonorRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        app = requireActivity().application as SaveLifeApplication

        setupDropdowns()
        setupSaveButton()
    }

    private fun setupDropdowns() {
        // Blood Type dropdown
        val bloodTypes = arrayOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        val bloodTypeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, bloodTypes)
        binding.bloodTypeDropdown.setAdapter(bloodTypeAdapter)

        // Health Status dropdown
        val healthStatuses = arrayOf("Healthy", "Deferred")
        val healthStatusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, healthStatuses)
        binding.healthStatusDropdown.setAdapter(healthStatusAdapter)
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            saveDonorRegistration()
        }
    }

    private fun saveDonorRegistration() {
        val userId = auth.currentUser?.uid ?: return

        val bloodType = binding.bloodTypeDropdown.text.toString()
        val country = binding.countryEditText.text.toString()
        val city = binding.cityEditText.text.toString()
        val address = binding.addressEditText.text.toString()
        val healthStatus = binding.healthStatusDropdown.text.toString()
        val description = binding.descriptionEditText.text.toString()

        if (bloodType.isEmpty() || country.isEmpty() || city.isEmpty() || 
            address.isEmpty() || healthStatus.isEmpty() || description.isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Create registration data
        val donorRegistration = DonorRegistration(
            bloodType = bloodType,
            country = country,
            city = city,
            address = address,
            healthStatus = healthStatus,
            description = description,
            updatedAt = Date().time.toString()
        )
        
        // Check if offline or online
        if (!app.connectivityManager.isNetworkAvailable.value) {
            // Save locally for offline mode
            saveDonorRegistrationOffline(userId, donorRegistration)
        } else {
            // Save to Firebase if online
            saveDonorRegistrationOnline(userId, donorRegistration)
        }
    }
    
    private fun saveDonorRegistrationOffline(userId: String, registration: DonorRegistration) {
        // Add to pending registration
        app.databaseHelper.addPendingDonorRegistration(userId, registration)
        
        // Also save to local database for immediate use
        app.databaseHelper.saveDonorRegistration(userId, registration)
        
        Toast.makeText(context, "Donor registration saved offline. Will be synced when online.", Toast.LENGTH_SHORT).show()
        requireActivity().onBackPressed()
    }
    
    private fun saveDonorRegistrationOnline(userId: String, registration: DonorRegistration) {
        // First check if user has donor role
        database.reference.child("users").child(userId).child("roles")
            .get()
            .addOnSuccessListener { snapshot ->
                val roles = snapshot.getValue(Roles::class.java) ?: Roles()
                if (!roles.donor) {
                    updateUserRoles(true)
                }

                // Convert registration to map
                val donorData = hashMapOf<String, Any>(
                    "bloodType" to registration.bloodType,
                    "country" to registration.country,
                    "city" to registration.city,
                    "address" to registration.address,
                    "healthStatus" to registration.healthStatus,
                    "description" to registration.description,
                    "updatedAt" to registration.updatedAt.toString()
                )

                database.reference.child("donorRegistrations").child(userId)
                    .setValue(donorData)
                    .addOnSuccessListener {
                        // Also save locally for offline use
                        app.databaseHelper.saveDonorRegistration(userId, registration)
                        
                        Toast.makeText(context, "Donor registration saved successfully", Toast.LENGTH_SHORT).show()
                        requireActivity().onBackPressed()
                    }
                    .addOnFailureListener {
                        // On failure, save locally
                        saveDonorRegistrationOffline(userId, registration)
                    }
            }
            .addOnFailureListener {
                // On failure, save locally
                saveDonorRegistrationOffline(userId, registration)
            }
    }

    private fun updateUserRoles(isDonor: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        database.reference.child("users").child(userId).child("roles")
            .get()
            .addOnSuccessListener { snapshot ->
                val currentRoles = snapshot.getValue(Roles::class.java) ?: Roles()
                val updatedRoles = currentRoles.copy(donor = isDonor)
                
                database.reference.child("users").child(userId).child("roles")
                    .setValue(updatedRoles)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class Roles(
    val donor: Boolean = false,
    val requester: Boolean = false
) 