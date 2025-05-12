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
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.haseebali.savelife.Constants
import com.haseebali.savelife.R
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

        loadUserProfile()
        setupClickListeners()
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        database.reference.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    user?.let {
                        if (_binding != null) {
                            binding.userNameTextView.text = it.fullName
                            
                            // Load profile picture
                            if (!it.profilePicture.isNullOrEmpty()) {
                                val imageUrl = Constants.SERVER_IMAGES_URL + it.profilePicture
                                Toast.makeText(activity, "image url: $imageUrl", Toast.LENGTH_LONG).show()
                                Glide.with(requireActivity())
                                    .load(imageUrl)
                                    .skipMemoryCache(true)
                                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                                    .into(binding.profileImage)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(activity, "Error loading profile", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setupClickListeners() {
        binding.editProfileCard.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_profile_to_editProfileFragment)
        }

        binding.donationAppointmentsCard.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_profile_to_appointmentsFragment)
        }

        binding.donationHistoryCard.setOnClickListener {
            findNavController().navigate(R.id.action_navigation_profile_to_donationHistoryFragment)
        }

        binding.logoutCard.setOnClickListener {
            auth.signOut()
            // TODO: Navigate to login screen
            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
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
    var uid: String = "",
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