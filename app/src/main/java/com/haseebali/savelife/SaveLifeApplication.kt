package com.haseebali.savelife

import android.app.Application
import android.view.View
import android.widget.TextView
import com.google.firebase.FirebaseApp
import com.haseebali.savelife.data.ConnectivityManager
import com.haseebali.savelife.data.DatabaseHelper
import com.haseebali.savelife.data.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SaveLifeApplication : Application() {
    
    // Managers
    lateinit var databaseHelper: DatabaseHelper
        private set
    
    lateinit var connectivityManager: ConnectivityManager
        private set
    
    lateinit var syncManager: SyncManager
        private set
    
    // Application-level coroutine scope
    private val applicationScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize managers
        databaseHelper = DatabaseHelper(this)
        connectivityManager = ConnectivityManager(this)
        syncManager = SyncManager(this)
        
        // Initialize singleton instance
        instance = this
    }
    
    // Show or hide offline mode indicator
    fun setupOfflineModeIndicator(offlineBanner: TextView) {
        applicationScope.launch {
            connectivityManager.isNetworkAvailable.collect { isAvailable ->
                offlineBanner.visibility = if (isAvailable) View.GONE else View.VISIBLE
            }
        }
    }
    
    companion object {
        lateinit var instance: SaveLifeApplication
            private set
    }
} 