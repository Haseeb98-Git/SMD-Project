package com.haseebali.savelife.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.haseebali.savelife.Constants
import com.haseebali.savelife.databinding.FragmentProfileBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var selectedImageUri: Uri? = null

    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                binding.profileImage.setImageURI(uri)
                uploadImage(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        setupProfileImageClick()
        loadUserProfile()
        setupSaveButton()
    }

    private fun setupProfileImageClick() {
        binding.profileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            getContent.launch(intent)
        }
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        database.reference.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    user?.let {
                        binding.fullNameEditText.setText(it.fullName)
                        binding.usernameEditText.setText(it.username)
                        binding.emailEditText.setText(it.email)
                        
                        // Load profile picture
                        if (!it.profilePicture.isNullOrEmpty()) {
                            Glide.with(this@ProfileFragment)
                                .load(Constants.SERVER_IMAGES_URL + it.profilePicture)
                                .into(binding.profileImage)
                        }

                        // Set roles
                        binding.donorCheckBox.isChecked = it.roles?.donor == true
                        binding.requesterCheckBox.isChecked = it.roles?.requester == true

                        // Set donor availability if donor
                        if (it.roles?.donor == true) {
                            binding.donorAvailabilityGroup.visibility = View.VISIBLE
                            when (it.donorAvailability) {
                                "available" -> binding.availableRadioButton.isChecked = true
                                "unavailable" -> binding.unavailableRadioButton.isChecked = true
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Error loading profile", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            saveProfile()
        }
    }

    private fun saveProfile() {
        val userId = auth.currentUser?.uid ?: return
        val fullName = binding.fullNameEditText.text.toString()
        val username = binding.usernameEditText.text.toString()
        val isDonor = binding.donorCheckBox.isChecked
        val isRequester = binding.requesterCheckBox.isChecked
        val donorAvailability = if (isDonor) {
            when (binding.donorAvailabilityGroup.checkedRadioButtonId) {
                binding.availableRadioButton.id -> "available"
                binding.unavailableRadioButton.id -> "unavailable"
                else -> null
            }
        } else null

        val userData = hashMapOf<String, Any>(
            "fullName" to fullName,
            "username" to username,
            "roles" to hashMapOf(
                "donor" to isDonor,
                "requester" to isRequester
            )
        )

        if (donorAvailability != null) {
            userData["donorAvailability"] = donorAvailability
        }

        database.reference.child("users").child(userId)
            .updateChildren(userData)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error updating profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadImage(uri: Uri) {
        val userId = auth.currentUser?.uid ?: return
        
        // Create multipart request body
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image",
                "profile_${userId}.jpg",
                requireContext().contentResolver.openInputStream(uri)?.let { inputStream ->
                    val bytes = inputStream.readBytes()
                    RequestBody.create("image/*".toMediaTypeOrNull(), bytes)
                } ?: return
            )
            .build()

        val request = Request.Builder()
            .url(Constants.SERVER_URL + "image_api/upload.php")
            .post(requestBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(context, "Error uploading image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    try {
                        val jsonResponse = org.json.JSONObject(responseBody)
                        val fileName = jsonResponse.getString("file")
                        
                        // Update user profile with new image name
                        database.reference.child("users").child(userId)
                            .child("profilePicture")
                            .setValue(fileName)
                            .addOnSuccessListener {
                                activity?.runOnUiThread {
                                    Toast.makeText(context, "Profile picture updated successfully", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                activity?.runOnUiThread {
                                    Toast.makeText(context, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } catch (e: Exception) {
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Error parsing server response: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Server error: ${responseBody}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class User(
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val profilePicture: String? = null,
    val roles: Roles? = null,
    val donorAvailability: String? = null
)

data class Roles(
    val donor: Boolean = false,
    val requester: Boolean = false
) 