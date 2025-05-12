package com.haseebali.savelife.ui.browse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.haseebali.savelife.R
import com.haseebali.savelife.models.User
import com.haseebali.savelife.models.DonorRegistration
import com.haseebali.savelife.models.RequesterRegistration
import com.haseebali.savelife.models.UserWithDetails
import com.google.firebase.database.FirebaseDatabase

class BrowseAdapter(
    private val isDonorList: Boolean,
    private val onUserClick: (UserWithDetails) -> Unit
) : ListAdapter<User, BrowseAdapter.UserViewHolder>(UserDiffCallback()) {

    // Map to store the registration details for each user
    private val userDetailsMap = mutableMapOf<String, Pair<String, String>>() // uid -> (bloodType, location)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user)
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.nameText)
        private val bloodTypeText: TextView = itemView.findViewById(R.id.bloodTypeText)
        private val locationText: TextView = itemView.findViewById(R.id.locationText)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val user = getItem(position)
                    val details = userDetailsMap[user.uid]
                    onUserClick(
                        UserWithDetails(
                            user = user,
                            bloodType = details?.first ?: "",
                            country = details?.second?.split(", ")?.getOrNull(1) ?: "",
                            city = details?.second?.split(", ")?.getOrNull(0) ?: ""
                        )
                    )
                }
            }
        }

        fun bind(user: User) {
            nameText.text = user.fullName
            
            // Get registration details
            val database = FirebaseDatabase.getInstance()
            val registrationsRef = if (isDonorList) {
                database.getReference("donorRegistrations")
            } else {
                database.getReference("requesterRegistrations")
            }

            registrationsRef.child(user.uid).get().addOnSuccessListener { snapshot ->
                if (isDonorList) {
                    val registration = snapshot.getValue(DonorRegistration::class.java)
                    registration?.let {
                        val bloodType = it.bloodType
                        val location = "${it.city}, ${it.country}"
                        
                        bloodTypeText.text = "Blood Type: $bloodType"
                        locationText.text = location
                        
                        // Store details for click handler
                        userDetailsMap[user.uid] = Pair(bloodType, location)
                    }
                } else {
                    val registration = snapshot.getValue(RequesterRegistration::class.java)
                    registration?.let {
                        val bloodType = it.bloodType
                        val location = "${it.city}, ${it.country}"
                        
                        bloodTypeText.text = "Blood Type: $bloodType"
                        locationText.text = location
                        
                        // Store details for click handler
                        userDetailsMap[user.uid] = Pair(bloodType, location)
                    }
                }
            }
        }
    }

    private class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
} 