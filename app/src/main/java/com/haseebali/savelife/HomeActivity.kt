package com.haseebali.savelife

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        val navView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        navView.setupWithNavController(navController)

        // Handle navigation item selection manually
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_register -> {
                    navController.navigate(R.id.action_global_registerFragment)
                    true
                }
                R.id.navigation_browse -> {
                    navController.navigate(R.id.action_global_browseFragment)
                    true
                }
                R.id.navigation_profile -> {
                    navController.navigate(R.id.action_global_profileFragment)
                    true
                }
                R.id.navigation_messages -> {
                    navController.navigate(R.id.action_global_messagesFragment)
                    true
                }
                else -> false
            }
        }
    }
} 