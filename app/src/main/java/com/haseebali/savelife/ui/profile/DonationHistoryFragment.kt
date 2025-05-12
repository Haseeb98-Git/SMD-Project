package com.haseebali.savelife.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.haseebali.savelife.R
import com.haseebali.savelife.models.Appointment
import java.text.SimpleDateFormat
import java.util.*

class DonationHistoryFragment : Fragment() {
    private lateinit var rvDonationHistory: RecyclerView
    private lateinit var tvNoDonationHistory: TextView
    private lateinit var adapter: DonationHistoryAdapter
    private lateinit var currentUserId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_donation_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        rvDonationHistory = view.findViewById(R.id.rvDonationHistory)
        tvNoDonationHistory = view.findViewById(R.id.tvNoDonationHistory)

        setupRecyclerView()
        loadDonationHistory()
    }

    private fun setupRecyclerView() {
        adapter = DonationHistoryAdapter()
        rvDonationHistory.layoutManager = LinearLayoutManager(requireContext())
        rvDonationHistory.adapter = adapter
    }

    private fun loadDonationHistory() {
        val appointmentsRef = FirebaseDatabase.getInstance().getReference("appointments")
        
        appointmentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val appointments = mutableListOf<Appointment>()
                
                for (appointmentSnapshot in snapshot.children) {
                    val appointment = appointmentSnapshot.getValue(Appointment::class.java)
                    appointment?.let {
                        // Only show completed appointments where current user is the donor
                        if (it.donorId == currentUserId && it.status == "completed") {
                            it.id = appointmentSnapshot.key ?: ""
                            appointments.add(it)
                        }
                    }
                }
                
                val sortedAppointments = appointments.sortedByDescending { it.completedDate }
                adapter.submitList(sortedAppointments)
                tvNoDonationHistory.visibility = 
                    if (sortedAppointments.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }
}

class DonationHistoryAdapter : RecyclerView.Adapter<DonationHistoryAdapter.DonationHistoryViewHolder>() {
    private var donations: List<Appointment> = emptyList()

    fun submitList(newList: List<Appointment>) {
        donations = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DonationHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_donation_history, parent, false)
        return DonationHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DonationHistoryViewHolder, position: Int) {
        holder.bind(donations[position])
    }

    override fun getItemCount() = donations.size

    inner class DonationHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvRequesterName: TextView = itemView.findViewById(R.id.tvRequesterName)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val tvVenue: TextView = itemView.findViewById(R.id.tvVenue)
        private val tvCompletedDate: TextView = itemView.findViewById(R.id.tvCompletedDate)

        fun bind(donation: Appointment) {
            // Get requester's name from Firebase
            FirebaseDatabase.getInstance().getReference("users")
                .child(donation.requesterId)
                .child("fullName")
                .get()
                .addOnSuccessListener { snapshot ->
                    val name = snapshot.value as? String ?: "Unknown User"
                    tvRequesterName.text = "Donated to: $name"
                }

            // Set date
            tvDate.text = "Appointment Date: ${donation.date}"

            // Set location
            tvLocation.text = "Location: ${donation.city}, ${donation.country}"

            // Set venue
            tvVenue.text = "Venue: ${donation.venue}"
            
            // Set completed date
            if (donation.completedDate.isNotEmpty()) {
                try {
                    val completedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(Date(donation.completedDate.toLong()))
                    tvCompletedDate.text = "Completed on: $completedDate"
                } catch (e: Exception) {
                    tvCompletedDate.text = "Completed"
                }
            } else {
                tvCompletedDate.text = "Completed"
            }
        }
    }
} 