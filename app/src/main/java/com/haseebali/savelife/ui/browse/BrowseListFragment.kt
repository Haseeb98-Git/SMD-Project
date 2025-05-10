package com.haseebali.savelife.ui.browse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.haseebali.savelife.R
import com.haseebali.savelife.models.User

class BrowseListFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BrowseAdapter
    private var isDonorList: Boolean = false

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
        
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = BrowseAdapter(isDonorList) { user ->
            // Handle user click - navigate to profile
            // TODO: Implement navigation to user profile
        }
        recyclerView.adapter = adapter

        loadData()
    }

    private fun loadData() {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")
        val registrationsRef = if (isDonorList) {
            database.getReference("donorRegistrations")
        } else {
            database.getReference("requesterRegistrations")
        }

        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = mutableListOf<User>()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    user?.let {
                        if ((isDonorList && it.roles?.donor == true) ||
                            (!isDonorList && it.roles?.requester == true)) {
                            users.add(it)
                        }
                    }
                }
                adapter.submitList(users)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
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