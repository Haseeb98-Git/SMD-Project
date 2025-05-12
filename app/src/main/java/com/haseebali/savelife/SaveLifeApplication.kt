package com.haseebali.savelife

import android.app.Application
import android.view.View
import android.widget.TextView
import com.google.firebase.FirebaseApp
import com.haseebali.savelife.data.ConnectivityManager
import com.haseebali.savelife.data.DatabaseHelper
import com.haseebali.savelife.data.NotificationService
import com.haseebali.savelife.data.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel

class SaveLifeApplication : Application() {
    
    // Managers
    lateinit var databaseHelper: DatabaseHelper
        private set
    
    lateinit var connectivityManager: ConnectivityManager
        private set
    
    lateinit var syncManager: SyncManager
        private set
        
    // Notification service
    lateinit var notificationService: NotificationService
        private set
    
    // Application-level coroutine scope
    private val applicationScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // OneSignal initialization
        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        
        // Initialize with your OneSignal App ID
        OneSignal.initWithContext(this, Constants.ONESIGNAL_APP_ID)

        // Initialize managers
        databaseHelper = DatabaseHelper(this)
        connectivityManager = ConnectivityManager(this)
        syncManager = SyncManager(this)
        notificationService = NotificationService()
        
        // Initialize singleton instance
        instance = this
    }
    
    // Set external user ID for OneSignal when a user logs in
    fun setCurrentUser(userId: String) {
        // Set OneSignal External User ID
        OneSignal.login(userId)
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