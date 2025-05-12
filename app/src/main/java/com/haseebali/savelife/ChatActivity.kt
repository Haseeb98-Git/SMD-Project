package com.haseebali.savelife

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.haseebali.savelife.models.Message
import com.haseebali.savelife.models.User
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {
    private lateinit var tvChatTitle: TextView
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var adapter: MessageAdapter

    private lateinit var currentUserId: String
    private lateinit var otherUserId: String
    private lateinit var otherUserName: String
    private lateinit var conversationId: String
    private var currentUserName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Initialize views
        tvChatTitle = findViewById(R.id.tvChatTitle)
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        // Get data from intent
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        otherUserId = intent.getStringExtra("userId") ?: return
        otherUserName = intent.getStringExtra("userName") ?: return

        // Set chat title
        tvChatTitle.text = "Chat with $otherUserName"

        // Create conversation ID (sorted to ensure consistency)
        conversationId = if (currentUserId < otherUserId) {
            "conversation_${currentUserId}_${otherUserId}"
        } else {
            "conversation_${otherUserId}_${currentUserId}"
        }

        // Get current user's name for notifications
        getCurrentUserName()

        // Setup RecyclerView
        rvMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        adapter = MessageAdapter(currentUserId)
        rvMessages.adapter = adapter

        // Load messages
        loadMessages()

        // Setup send button
        btnSend.setOnClickListener {
            val messageText = etMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                etMessage.text.clear()
            }
        }
    }

    private fun getCurrentUserName() {
        FirebaseDatabase.getInstance().getReference("users")
            .child(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    currentUserName = user?.fullName ?: "User"
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun loadMessages() {
        FirebaseDatabase.getInstance().getReference("messages")
            .child(conversationId)
            .child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = mutableListOf<Message>()
                    for (messageSnapshot in snapshot.children) {
                        val message = messageSnapshot.getValue(Message::class.java)
                        message?.let { messages.add(it) }
                    }
                    adapter.submitList(messages)
                    rvMessages.scrollToPosition(messages.size - 1)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun sendMessage(text: String) {
        val message = Message(
            sender = currentUserId,
            text = text,
            timestamp = System.currentTimeMillis()
        )

        // Add message to conversation
        FirebaseDatabase.getInstance().getReference("messages")
            .child(conversationId)
            .child("messages")
            .push()
            .setValue(message)

        // Update participants
        FirebaseDatabase.getInstance().getReference("messages")
            .child(conversationId)
            .child("participants")
            .child(currentUserId)
            .setValue(true)

        FirebaseDatabase.getInstance().getReference("messages")
            .child(conversationId)
            .child("participants")
            .child(otherUserId)
            .setValue(true)
            
        // Send push notification to other user
        sendNotificationToUser(otherUserId, text)
    }
    
    private fun sendNotificationToUser(recipientUserId: String, messageText: String) {
        // Get the application instance
        val app = application as SaveLifeApplication
        
        // Send notification
        app.notificationService.sendNotificationToUser(
            recipientUserId = recipientUserId,
            title = "New message from $currentUserName",
            message = messageText,
            data = mapOf(
                "conversationId" to conversationId,
                "senderId" to currentUserId,
                "senderName" to currentUserName,
                "type" to "message"
            )
        )
    }
}

class MessageAdapter(private val currentUserId: String) : 
    ListAdapter<Message, MessageAdapter.MessageViewHolder>(MessageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MessageViewHolder(itemView: View) : 
        RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        private val messageContainer: LinearLayout = itemView.findViewById(R.id.messageContainer)

        fun bind(message: Message) {
            tvMessage.text = message.text
            
            // Format timestamp
            val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
            tvTimestamp.text = dateFormat.format(Date(message.timestamp))

            // Align message based on sender
            val params = messageContainer.layoutParams as LinearLayout.LayoutParams
            if (message.sender == currentUserId) {
                params.gravity = android.view.Gravity.END
                tvMessage.setBackgroundResource(R.drawable.bg_message_sent)
            } else {
                params.gravity = android.view.Gravity.START
                tvMessage.setBackgroundResource(R.drawable.bg_message)
            }
            messageContainer.layoutParams = params
        }
    }

    private class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
} 