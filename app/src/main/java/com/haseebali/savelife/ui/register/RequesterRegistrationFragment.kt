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
import com.haseebali.savelife.databinding.FragmentRequesterRegistrationBinding
import java.util.Date

class RequesterRegistrationFragment : Fragment() {
    private var _binding: FragmentRequesterRegistrationBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRequesterRegistrationBinding.inflate(inflater, container, false)
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

        // Urgency dropdown
        val urgencyLevels = arrayOf("immediate", "within 24 hours", "within 3 days", "other")
        val urgencyAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, urgencyLevels)
        binding.urgencyDropdown.setAdapter(urgencyAdapter)
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            saveRequesterRegistration()
        }
    }

    private fun saveRequesterRegistration() {
        val userId = auth.currentUser?.uid ?: return

        val bloodType = binding.bloodTypeDropdown.text.toString()
        val country = binding.countryEditText.text.toString()
        val city = binding.cityEditText.text.toString()
        val address = binding.addressEditText.text.toString()
        val urgency = binding.urgencyDropdown.text.toString()
        val description = binding.descriptionEditText.text.toString()

        if (bloodType.isEmpty() || country.isEmpty() || city.isEmpty() || 
            address.isEmpty() || urgency.isEmpty() || description.isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val requesterData = hashMapOf<String, Any>(
            "bloodType" to bloodType,
            "country" to country,
            "city" to city,
            "address" to address,
            "urgency" to urgency,
            "description" to description,
            "updatedAt" to Date().time.toString()
        )

        database.reference.child("requesterRegistrations").child(userId)
            .setValue(requesterData)
            .addOnSuccessListener {
                Toast.makeText(context, "Requester registration saved successfully", Toast.LENGTH_SHORT).show()
                // Update user roles to include requester
                updateUserRoles(true)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error saving requester registration", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserRoles(isRequester: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        database.reference.child("users").child(userId).child("roles")
            .get()
            .addOnSuccessListener { snapshot ->
                val currentRoles = snapshot.getValue(Roles::class.java) ?: Roles()
                val updatedRoles = currentRoles.copy(requester = isRequester)
                
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