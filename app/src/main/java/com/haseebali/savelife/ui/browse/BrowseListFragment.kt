package com.haseebali.savelife.ui.browse

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.haseebali.savelife.DonationDetailsActivity
import com.haseebali.savelife.R
import com.haseebali.savelife.SaveLifeApplication
import com.haseebali.savelife.models.DonorRegistration
import com.haseebali.savelife.models.RequesterRegistration
import com.haseebali.savelife.models.User
import com.haseebali.savelife.models.UserWithDetails

class BrowseListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoResults: TextView
    private lateinit var bloodTypeFilter: AutoCompleteTextView
    private lateinit var countryFilter: TextInputEditText
    private lateinit var btnApplyFilters: Button
    private lateinit var btnClearFilters: Button
    private lateinit var adapter: BrowseAdapter
    private var isDonorList: Boolean = false

    private var allUsers = mutableListOf<UserWithDetails>()
    private var filteredUsers = mutableListOf<UserWithDetails>()
    
    // Standard blood types
    private val bloodTypes = listOf(
        "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            isDonorList = it.getBoolean(ARG_IS_DONOR_LIST)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_browse_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerView)
        tvNoResults = view.findViewById(R.id.tvNoResults)
        bloodTypeFilter = view.findViewById(R.id.bloodTypeFilter)
        countryFilter = view.findViewById(R.id.countryFilter)
        btnApplyFilters = view.findViewById(R.id.btnApplyFilters)
        btnClearFilters = view.findViewById(R.id.btnClearFilters)

        setupRecyclerView()
        setupFilterButtons()
        setupBloodTypeDropdown()
        loadData()
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = BrowseAdapter(isDonorList) { userWithDetails ->
            // Navigate to DonationDetailsActivity
            val intent = Intent(requireContext(), DonationDetailsActivity::class.java).apply {
                putExtra("userId", userWithDetails.user.uid)
                putExtra("isDonor", isDonorList)
                putExtra("currentUserId", FirebaseAuth.getInstance().currentUser?.uid)
            }
            startActivity(intent)
        }
        recyclerView.adapter = adapter
    }

    private fun setupFilterButtons() {
        btnApplyFilters.setOnClickListener {
            applyFilters()
        }

        btnClearFilters.setOnClickListener {
            clearFilters()
        }
    }
    
    private fun setupBloodTypeDropdown() {
        val bloodTypeAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            bloodTypes
        )
        bloodTypeFilter.setAdapter(bloodTypeAdapter)
    }

    private fun loadData() {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")
        val registrationsRef = if (isDonorList) {
            database.getReference("donorRegistrations")
        } else {
            database.getReference("requesterRegistrations")
        }

        // Reset lists
        allUsers.clear()
        filteredUsers.clear()

        // Check if app is offline
        val app = requireActivity().application as SaveLifeApplication
        if (!app.connectivityManager.isNetworkAvailable.value) {
            // Load from local database
            loadFromLocalDatabase()
            return
        }

        // Load from Firebase if online
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = mutableListOf<User>()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    user?.let {
                        // Create a new User object with the uid from snapshot key
                        val userWithUid = it.copy(uid = userSnapshot.key ?: "")
                        if ((isDonorList && userWithUid.roles?.donor == true) ||
                            (!isDonorList && userWithUid.roles?.requester == true)) {
                            users.add(userWithUid)
                            
                            // Also save to local database for offline mode
                            app.databaseHelper.saveUser(userWithUid)
                        }
                    }
                }
                
                // For each user, get their registration details
                loadUserRegistrations(users, registrationsRef)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error - try to load from local database as fallback
                loadFromLocalDatabase()
            }
        })
    }
    
    private fun loadFromLocalDatabase() {
        val app = requireActivity().application as SaveLifeApplication
        val dbHelper = app.databaseHelper
        
        // Get all users
        val allLocalUsers = dbHelper.getAllUsers()
        
        // Get registrations based on type
        val registrations = if (isDonorList) {
            dbHelper.getAllDonorRegistrations()
        } else {
            dbHelper.getAllRequesterRegistrations()
        }
        
        // Process users based on roles and available registrations
        val filteredLocalUsers = allLocalUsers.filter { user ->
            val hasRole = if (isDonorList) {
                user.roles?.donor == true
            } else {
                user.roles?.requester == true
            }
            
            // User needs to have both the right role and a registration
            hasRole && registrations.containsKey(user.uid)
        }
        
        // Create UserWithDetails objects
        for (user in filteredLocalUsers) {
            if (isDonorList) {
                val registration = dbHelper.getAllDonorRegistrations()[user.uid]
                registration?.let {
                    val userWithDetails = UserWithDetails(
                        user = user,
                        bloodType = it.bloodType,
                        country = it.country,
                        city = it.city
                    )
                    addUserWithDetails(userWithDetails)
                }
            } else {
                val registration = dbHelper.getAllRequesterRegistrations()[user.uid]
                registration?.let {
                    val userWithDetails = UserWithDetails(
                        user = user,
                        bloodType = it.bloodType,
                        country = it.country,
                        city = it.city
                    )
                    addUserWithDetails(userWithDetails)
                }
            }
        }
        
        // Update UI with local data
        updateUI()
    }
    
    private fun loadUserRegistrations(users: List<User>, registrationsRef: DatabaseReference) {
        val app = requireActivity().application as SaveLifeApplication
        
        for (user in users) {
            registrationsRef.child(user.uid).get().addOnSuccessListener { snapshot ->
                if (isDonorList) {
                    val registration = snapshot.getValue(DonorRegistration::class.java)
                    registration?.let {
                        // Save to local database for offline mode
                        app.databaseHelper.saveDonorRegistration(user.uid, it)
                        
                        val userWithDetails = UserWithDetails(
                            user = user,
                            bloodType = it.bloodType,
                            country = it.country,
                            city = it.city
                        )
                        addUserWithDetails(userWithDetails)
                    }
                } else {
                    val registration = snapshot.getValue(RequesterRegistration::class.java)
                    registration?.let {
                        // Save to local database for offline mode
                        app.databaseHelper.saveRequesterRegistration(user.uid, it)
                        
                        val userWithDetails = UserWithDetails(
                            user = user,
                            bloodType = it.bloodType,
                            country = it.country,
                            city = it.city
                        )
                        addUserWithDetails(userWithDetails)
                    }
                }
            }
        }
    }

    private fun addUserWithDetails(userWithDetails: UserWithDetails) {
        allUsers.add(userWithDetails)
        filteredUsers.add(userWithDetails)
        
        // Update UI
        updateUI()
    }

    private fun updateUI() {
        // Update RecyclerView
        adapter.submitList(filteredUsers.map { it.user })
        
        // Show/hide no results message
        tvNoResults.visibility = if (filteredUsers.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun applyFilters() {
        val selectedBloodType = bloodTypeFilter.text.toString()
        val selectedCountry = countryFilter.text.toString().trim()
        
        filteredUsers = allUsers.filter { user ->
            (selectedBloodType.isEmpty() || user.bloodType == selectedBloodType) &&
            (selectedCountry.isEmpty() || user.country.contains(selectedCountry, ignoreCase = true))
        }.toMutableList()
        
        adapter.submitList(filteredUsers.map { it.user })
        tvNoResults.visibility = if (filteredUsers.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun clearFilters() {
        bloodTypeFilter.setText("")
        countryFilter.setText("")
        filteredUsers = allUsers.toMutableList()
        adapter.submitList(filteredUsers.map { it.user })
        tvNoResults.visibility = if (filteredUsers.isEmpty()) View.VISIBLE else View.GONE
    }

    companion object {
        private const val ARG_IS_DONOR_LIST = "is_donor_list"

        fun newInstance(isDonorList: Boolean) = BrowseListFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_IS_DONOR_LIST, isDonorList)
            }
        }
    }
} 