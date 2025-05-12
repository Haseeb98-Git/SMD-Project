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
import com.haseebali.savelife.databinding.FragmentDonorRegistrationBinding
import java.util.Date

class DonorRegistrationFragment : Fragment() {
    private var _binding: FragmentDonorRegistrationBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

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

        // Check if user has donor role
        database.reference.child("users").child(userId).child("roles")
            .get()
            .addOnSuccessListener { snapshot ->
                val roles = snapshot.getValue(Roles::class.java) ?: Roles()
                if (!roles.donor) {
                    Toast.makeText(context, "You must select donor role in your profile first", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val bloodType = binding.bloodTypeDropdown.text.toString()
                val country = binding.countryEditText.text.toString()
                val city = binding.cityEditText.text.toString()
                val address = binding.addressEditText.text.toString()
                val healthStatus = binding.healthStatusDropdown.text.toString()
                val description = binding.descriptionEditText.text.toString()

                if (bloodType.isEmpty() || country.isEmpty() || city.isEmpty() || 
                    address.isEmpty() || healthStatus.isEmpty() || description.isEmpty()) {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val donorData = hashMapOf<String, Any>(
                    "bloodType" to bloodType,
                    "country" to country,
                    "city" to city,
                    "address" to address,
                    "healthStatus" to healthStatus,
                    "description" to description,
                    "updatedAt" to Date().time.toString()
                )

                database.reference.child("donorRegistrations").child(userId)
                    .setValue(donorData)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Donor registration saved successfully", Toast.LENGTH_SHORT).show()
                        requireActivity().onBackPressed()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error saving donor registration", Toast.LENGTH_SHORT).show()
                    }
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
                    .addOnSuccessListener {
                        // Navigate back
                        requireActivity().onBackPressed()
                    }
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