package com.haseebali.savelife

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    private val activityScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Set up offline banner
        val offlineBanner = findViewById<View>(R.id.offlineBanner)
        
        // Monitor network status directly
        val app = application as SaveLifeApplication
        activityScope.launch {
            app.connectivityManager.isNetworkAvailable.collect { isAvailable ->
                offlineBanner.visibility = if (isAvailable) View.GONE else View.VISIBLE
            }
        }

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