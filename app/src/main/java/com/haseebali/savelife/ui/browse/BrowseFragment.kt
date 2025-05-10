package com.haseebali.savelife.ui.browse

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.haseebali.savelife.R

class BrowseFragment : Fragment() {
    private lateinit var rootView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_browse, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        rootView.findViewById<MaterialCardView>(R.id.donorsCard).setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("is_donor_list", true)
            }
            findNavController().navigate(R.id.action_browseFragment_to_browseListFragment, bundle)
        }

        rootView.findViewById<MaterialCardView>(R.id.requestersCard).setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("is_donor_list", false)
            }
            findNavController().navigate(R.id.action_browseFragment_to_browseListFragment, bundle)
        }
    }
} 