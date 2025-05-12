package com.haseebali.savelife.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.haseebali.savelife.models.DonorRegistration
import com.haseebali.savelife.models.RequesterRegistration
import com.haseebali.savelife.models.User
import com.haseebali.savelife.models.Appointment
import com.haseebali.savelife.models.Roles
import org.json.JSONObject

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "savelife.db"
        private const val DATABASE_VERSION = 1

        // Table Names
        private const val TABLE_USERS = "users"
        private const val TABLE_DONOR_REGISTRATIONS = "donor_registrations"
        private const val TABLE_REQUESTER_REGISTRATIONS = "requester_registrations"
        private const val TABLE_PENDING_APPOINTMENTS = "pending_appointments"
        private const val TABLE_PENDING_DONOR_REGISTRATIONS = "pending_donor_registrations"
        private const val TABLE_PENDING_REQUESTER_REGISTRATIONS = "pending_requester_registrations"

        // Common Column Names
        private const val COLUMN_ID = "id"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_DATA = "data" // JSON serialized data
        private const val COLUMN_SYNCED = "synced"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create users table
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_DATA TEXT NOT NULL
            )
        """.trimIndent()

        // Create donor registrations table
        val createDonorRegistrationsTable = """
            CREATE TABLE $TABLE_DONOR_REGISTRATIONS (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_USER_ID TEXT NOT NULL,
                $COLUMN_DATA TEXT NOT NULL,
                FOREIGN KEY ($COLUMN_USER_ID) REFERENCES $TABLE_USERS($COLUMN_ID)
            )
        """.trimIndent()

        // Create requester registrations table
        val createRequesterRegistrationsTable = """
            CREATE TABLE $TABLE_REQUESTER_REGISTRATIONS (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_USER_ID TEXT NOT NULL,
                $COLUMN_DATA TEXT NOT NULL,
                FOREIGN KEY ($COLUMN_USER_ID) REFERENCES $TABLE_USERS($COLUMN_ID)
            )
        """.trimIndent()

        // Create pending appointments table
        val createPendingAppointmentsTable = """
            CREATE TABLE $TABLE_PENDING_APPOINTMENTS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID TEXT NOT NULL,
                $COLUMN_DATA TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_SYNCED INTEGER DEFAULT 0
            )
        """.trimIndent()

        // Create pending donor registrations table
        val createPendingDonorRegistrationsTable = """
            CREATE TABLE $TABLE_PENDING_DONOR_REGISTRATIONS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID TEXT NOT NULL,
                $COLUMN_DATA TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_SYNCED INTEGER DEFAULT 0
            )
        """.trimIndent()

        // Create pending requester registrations table
        val createPendingRequesterRegistrationsTable = """
            CREATE TABLE $TABLE_PENDING_REQUESTER_REGISTRATIONS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID TEXT NOT NULL,
                $COLUMN_DATA TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_SYNCED INTEGER DEFAULT 0
            )
        """.trimIndent()

        // Execute all table creation statements
        db.execSQL(createUsersTable)
        db.execSQL(createDonorRegistrationsTable)
        db.execSQL(createRequesterRegistrationsTable)
        db.execSQL(createPendingAppointmentsTable)
        db.execSQL(createPendingDonorRegistrationsTable)
        db.execSQL(createPendingRequesterRegistrationsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle database upgrades if needed
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PENDING_REQUESTER_REGISTRATIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PENDING_DONOR_REGISTRATIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PENDING_APPOINTMENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REQUESTER_REGISTRATIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_DONOR_REGISTRATIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // Save users to local database
    fun saveUser(user: User) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, user.uid)
            put(COLUMN_DATA, user.toJson())
        }
        db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    // Save donor registration to local database
    fun saveDonorRegistration(userId: String, registration: DonorRegistration) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, userId)
            put(COLUMN_USER_ID, userId)
            put(COLUMN_DATA, registration.toJson())
        }
        db.insertWithOnConflict(TABLE_DONOR_REGISTRATIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    // Save requester registration to local database
    fun saveRequesterRegistration(userId: String, registration: RequesterRegistration) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, userId)
            put(COLUMN_USER_ID, userId)
            put(COLUMN_DATA, registration.toJson())
        }
        db.insertWithOnConflict(TABLE_REQUESTER_REGISTRATIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
    }

    // Add a pending appointment that needs to be synced when online
    fun addPendingAppointment(userId: String, appointment: Map<String, Any>) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID, userId)
            put(COLUMN_DATA, JSONObject(appointment).toString())
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
            put(COLUMN_SYNCED, 0)
        }
        db.insert(TABLE_PENDING_APPOINTMENTS, null, values)
        db.close()
    }

    // Add a pending donor registration that needs to be synced when online
    fun addPendingDonorRegistration(userId: String, registration: DonorRegistration) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID, userId)
            put(COLUMN_DATA, registration.toJson())
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
            put(COLUMN_SYNCED, 0)
        }
        db.insert(TABLE_PENDING_DONOR_REGISTRATIONS, null, values)
        db.close()
    }

    // Add a pending requester registration that needs to be synced when online
    fun addPendingRequesterRegistration(userId: String, registration: RequesterRegistration) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID, userId)
            put(COLUMN_DATA, registration.toJson())
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
            put(COLUMN_SYNCED, 0)
        }
        db.insert(TABLE_PENDING_REQUESTER_REGISTRATIONS, null, values)
        db.close()
    }

    // Get all users from local database
    fun getAllUsers(): List<User> {
        val users = mutableListOf<User>()
        val db = readableDatabase
        val cursor = db.query(TABLE_USERS, arrayOf(COLUMN_DATA), null, null, null, null, null)

        cursor.use {
            while (it.moveToNext()) {
                val userJson = it.getString(it.getColumnIndexOrThrow(COLUMN_DATA))
                val user = User.fromJson(userJson)
                users.add(user)
            }
        }
        return users
    }

    // Get all donor registrations from local database
    fun getAllDonorRegistrations(): Map<String, DonorRegistration> {
        val registrations = mutableMapOf<String, DonorRegistration>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_DONOR_REGISTRATIONS,
            arrayOf(COLUMN_ID, COLUMN_DATA),
            null, null, null, null, null
        )

        cursor.use {
            while (it.moveToNext()) {
                val userId = it.getString(it.getColumnIndexOrThrow(COLUMN_ID))
                val registrationJson = it.getString(it.getColumnIndexOrThrow(COLUMN_DATA))
                val registration = DonorRegistration.fromJson(registrationJson)
                registrations[userId] = registration
            }
        }
        return registrations
    }

    // Get all requester registrations from local database
    fun getAllRequesterRegistrations(): Map<String, RequesterRegistration> {
        val registrations = mutableMapOf<String, RequesterRegistration>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_REQUESTER_REGISTRATIONS,
            arrayOf(COLUMN_ID, COLUMN_DATA),
            null, null, null, null, null
        )

        cursor.use {
            while (it.moveToNext()) {
                val userId = it.getString(it.getColumnIndexOrThrow(COLUMN_ID))
                val registrationJson = it.getString(it.getColumnIndexOrThrow(COLUMN_DATA))
                val registration = RequesterRegistration.fromJson(registrationJson)
                registrations[userId] = registration
            }
        }
        return registrations
    }

    // Get all pending appointments that need to be synced
    fun getPendingAppointments(): List<Pair<Int, Map<String, Any>>> {
        val appointments = mutableListOf<Pair<Int, Map<String, Any>>>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PENDING_APPOINTMENTS,
            arrayOf(COLUMN_ID, COLUMN_DATA),
            "$COLUMN_SYNCED = 0",
            null, null, null, null
        )

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow(COLUMN_ID))
                val dataJson = it.getString(it.getColumnIndexOrThrow(COLUMN_DATA))
                val data = convertJsonToMap(JSONObject(dataJson))
                appointments.add(Pair(id, data))
            }
        }
        return appointments
    }

    // Get all pending donor registrations that need to be synced
    fun getPendingDonorRegistrations(): List<Pair<Int, Pair<String, DonorRegistration>>> {
        val registrations = mutableListOf<Pair<Int, Pair<String, DonorRegistration>>>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PENDING_DONOR_REGISTRATIONS,
            arrayOf(COLUMN_ID, COLUMN_USER_ID, COLUMN_DATA),
            "$COLUMN_SYNCED = 0",
            null, null, null, null
        )

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow(COLUMN_ID))
                val userId = it.getString(it.getColumnIndexOrThrow(COLUMN_USER_ID))
                val dataJson = it.getString(it.getColumnIndexOrThrow(COLUMN_DATA))
                val registration = DonorRegistration.fromJson(dataJson)
                registrations.add(Pair(id, Pair(userId, registration)))
            }
        }
        return registrations
    }

    // Get all pending requester registrations that need to be synced
    fun getPendingRequesterRegistrations(): List<Pair<Int, Pair<String, RequesterRegistration>>> {
        val registrations = mutableListOf<Pair<Int, Pair<String, RequesterRegistration>>>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_PENDING_REQUESTER_REGISTRATIONS,
            arrayOf(COLUMN_ID, COLUMN_USER_ID, COLUMN_DATA),
            "$COLUMN_SYNCED = 0",
            null, null, null, null
        )

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndexOrThrow(COLUMN_ID))
                val userId = it.getString(it.getColumnIndexOrThrow(COLUMN_USER_ID))
                val dataJson = it.getString(it.getColumnIndexOrThrow(COLUMN_DATA))
                val registration = RequesterRegistration.fromJson(dataJson)
                registrations.add(Pair(id, Pair(userId, registration)))
            }
        }
        return registrations
    }

    // Mark a pending appointment as synced
    fun markAppointmentSynced(id: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SYNCED, 1)
        }
        db.update(TABLE_PENDING_APPOINTMENTS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()
    }

    // Mark a pending donor registration as synced
    fun markDonorRegistrationSynced(id: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SYNCED, 1)
        }
        db.update(TABLE_PENDING_DONOR_REGISTRATIONS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()
    }

    // Mark a pending requester registration as synced
    fun markRequesterRegistrationSynced(id: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_SYNCED, 1)
        }
        db.update(TABLE_PENDING_REQUESTER_REGISTRATIONS, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
        db.close()
    }

    // Helper function to convert JSONObject to Map
    private fun convertJsonToMap(jsonObject: JSONObject): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.get(key)
            map[key] = value
        }
        return map
    }
}

// Extension functions to convert models to/from JSON
fun User.toJson(): String {
    val json = JSONObject()
    json.put("uid", uid)
    json.put("fullName", fullName)
    json.put("username", username)
    json.put("email", email)
    json.put("profilePicture", profilePicture ?: "")
    
    val rolesJson = JSONObject()
    rolesJson.put("donor", roles?.donor ?: false)
    rolesJson.put("requester", roles?.requester ?: false)
    json.put("roles", rolesJson)
    
    json.put("donorAvailability", donorAvailability ?: "")
    return json.toString()
}

fun DonorRegistration.toJson(): String {
    val json = JSONObject()
    json.put("bloodType", bloodType)
    json.put("country", country)
    json.put("city", city)
    json.put("address", address)
    json.put("healthStatus", healthStatus)
    json.put("description", description)
    json.put("updatedAt", updatedAt.toString())
    return json.toString()
}

fun RequesterRegistration.toJson(): String {
    val json = JSONObject()
    json.put("bloodType", bloodType)
    json.put("country", country)
    json.put("city", city)
    json.put("address", address)
    json.put("urgency", urgency)
    json.put("description", description)
    json.put("updatedAt", updatedAt.toString())
    return json.toString()
}

fun User.Companion.fromJson(json: String): User {
    val jsonObject = JSONObject(json)
    val rolesJson = jsonObject.getJSONObject("roles")
    
    return User(
        uid = jsonObject.getString("uid"),
        fullName = jsonObject.getString("fullName"),
        username = jsonObject.getString("username"),
        email = jsonObject.getString("email"),
        profilePicture = jsonObject.getString("profilePicture"),
        roles = Roles(
            donor = rolesJson.getBoolean("donor"),
            requester = rolesJson.getBoolean("requester")
        ),
        donorAvailability = jsonObject.getString("donorAvailability")
    )
}

fun DonorRegistration.Companion.fromJson(json: String): DonorRegistration {
    val jsonObject = JSONObject(json)
    return DonorRegistration(
        bloodType = jsonObject.getString("bloodType"),
        country = jsonObject.getString("country"),
        city = jsonObject.getString("city"),
        address = jsonObject.getString("address"),
        healthStatus = jsonObject.getString("healthStatus"),
        description = jsonObject.getString("description"),
        updatedAt = jsonObject.getString("updatedAt")
    )
}

fun RequesterRegistration.Companion.fromJson(json: String): RequesterRegistration {
    val jsonObject = JSONObject(json)
    return RequesterRegistration(
        bloodType = jsonObject.getString("bloodType"),
        country = jsonObject.getString("country"),
        city = jsonObject.getString("city"),
        address = jsonObject.getString("address"),
        urgency = jsonObject.getString("urgency"),
        description = jsonObject.getString("description"),
        updatedAt = jsonObject.getString("updatedAt")
    )
} 