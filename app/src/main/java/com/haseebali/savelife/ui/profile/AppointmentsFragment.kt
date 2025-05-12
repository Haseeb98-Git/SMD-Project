package com.haseebali.savelife.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.haseebali.savelife.R
import com.haseebali.savelife.models.Appointment
import java.text.SimpleDateFormat
import java.util.*

class AppointmentsFragment : Fragment() {
    private lateinit var rvAppointments: RecyclerView
    private lateinit var tvNoAppointments: TextView
    private lateinit var adapter: AppointmentAdapter
    private lateinit var currentUserId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_appointments, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        rvAppointments = view.findViewById(R.id.rvAppointments)
        tvNoAppointments = view.findViewById(R.id.tvNoAppointments)

        setupRecyclerView()
        loadAppointments()
    }

    private fun setupRecyclerView() {
        adapter = AppointmentAdapter(
            onAccept = { appointment ->
                updateAppointmentStatus(appointment.id, "accepted")
            },
            onReject = { appointment ->
                updateAppointmentStatus(appointment.id, "rejected")
            },
            onComplete = { appointment ->
                updateAppointmentStatus(appointment.id, "completed")
            }
        )
        rvAppointments.layoutManager = LinearLayoutManager(requireContext())
        rvAppointments.adapter = adapter
    }

    private fun loadAppointments() {
        val appointmentsRef = FirebaseDatabase.getInstance().getReference("appointments")
        
        appointmentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val appointments = mutableListOf<Appointment>()
                
                for (appointmentSnapshot in snapshot.children) {
                    val appointment = appointmentSnapshot.getValue(Appointment::class.java)
                    appointment?.let {
                        // Set the ID from Firebase key
                        it.id = appointmentSnapshot.key ?: ""
                        // Only show appointments where current user is donor or requester
                        if (it.donorId == currentUserId || it.requesterId == currentUserId) {
                            appointments.add(it)
                        }
                    }
                }
                
                val sortedAppointments = appointments.sortedByDescending { it.createdAt }
                adapter.submitList(sortedAppointments)
                tvNoAppointments.visibility = if (sortedAppointments.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun updateAppointmentStatus(appointmentId: String, newStatus: String) {
        val updates = hashMapOf<String, Any>(
            "status" to newStatus
        )
        
        if (newStatus == "completed") {
            updates["completedDate"] = System.currentTimeMillis().toString()
        }

        FirebaseDatabase.getInstance().getReference("appointments")
            .child(appointmentId)
            .updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Appointment $newStatus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update appointment", Toast.LENGTH_SHORT).show()
            }
    }
}

class AppointmentAdapter(
    private val onAccept: (Appointment) -> Unit,
    private val onReject: (Appointment) -> Unit,
    private val onComplete: (Appointment) -> Unit
) : RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {

    private var appointments: List<Appointment> = emptyList()

    fun submitList(newList: List<Appointment>) {
        appointments = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(appointments[position])
    }

    override fun getItemCount() = appointments.size

    inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvParticipant: TextView = itemView.findViewById(R.id.tvParticipant)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val tvVenue: TextView = itemView.findViewById(R.id.tvVenue)
        private val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        private val btnReject: Button = itemView.findViewById(R.id.btnReject)
        private val btnComplete: Button = itemView.findViewById(R.id.btnComplete)

        fun bind(appointment: Appointment) {
            // Set status with color
            tvStatus.text = appointment.status.capitalize()
            when (appointment.status) {
                "pending" -> tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                "accepted" -> tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                "rejected" -> tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                "completed" -> tvStatus.setTextColor(itemView.context.getColor(android.R.color.darker_gray))
            }

            // Set participant info
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val otherUserId = if (currentUserId == appointment.donorId) {
                appointment.requesterId
            } else {
                appointment.donorId
            }
            val otherUserRole = if (currentUserId == appointment.donorId) "Requester" else "Donor"
            
            // Get other user's name from Firebase
            FirebaseDatabase.getInstance().getReference("users")
                .child(otherUserId)
                .child("fullName")
                .get()
                .addOnSuccessListener { snapshot ->
                    val name = snapshot.value as? String ?: "Unknown User"
                    tvParticipant.text = "$otherUserRole: $name"
                }

            // Set date
            tvDate.text = "Date: ${appointment.date}"

            // Set location
            tvLocation.text = "Location: ${appointment.city}, ${appointment.country}"

            // Set venue
            tvVenue.text = "Venue: ${appointment.venue}"

            // Show/hide action buttons based on status and user role
            btnAccept.visibility = View.GONE
            btnReject.visibility = View.GONE
            btnComplete.visibility = View.GONE

            when (appointment.status) {
                "pending" -> {
                    if (currentUserId == appointment.donorId) {
                        btnAccept.visibility = View.VISIBLE
                        btnReject.visibility = View.VISIBLE
                    }
                }
                "accepted" -> {
                    if (currentUserId == appointment.createdBy) {
                        btnComplete.visibility = View.VISIBLE
                    }
                }
            }

            // Set click listeners
            btnAccept.setOnClickListener { onAccept(appointment) }
            btnReject.setOnClickListener { onReject(appointment) }
            btnComplete.setOnClickListener { onComplete(appointment) }
        }
    }
} 