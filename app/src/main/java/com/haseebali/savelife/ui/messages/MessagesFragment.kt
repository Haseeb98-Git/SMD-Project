package com.haseebali.savelife.ui.messages

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.haseebali.savelife.ChatActivity
import com.haseebali.savelife.R
import com.haseebali.savelife.models.Conversation
import com.haseebali.savelife.models.Message
import com.haseebali.savelife.models.User
import com.haseebali.savelife.Constants
import java.text.SimpleDateFormat
import java.util.*

class MessagesFragment : Fragment() {
    private lateinit var rvConversations: RecyclerView
    private lateinit var tvNoMessages: TextView
    private lateinit var adapter: ConversationAdapter
    private lateinit var currentUserId: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_messages, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        rvConversations = view.findViewById(R.id.rvConversations)
        tvNoMessages = view.findViewById(R.id.tvNoMessages)
        
        setupRecyclerView()
        loadConversations()
    }

    private fun setupRecyclerView() {
        adapter = ConversationAdapter { conversation ->
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                // Extract the other user's ID from the conversation ID
                val userIds = conversation.id.replace("conversation_", "").split("_")
                putExtra("userId", userIds.first { it != currentUserId })
                putExtra("userName", conversation.otherUserName)
            }
            startActivity(intent)
        }
        
        rvConversations.layoutManager = LinearLayoutManager(requireContext())
        rvConversations.adapter = adapter
    }

    private fun loadConversations() {
        val conversationsRef = FirebaseDatabase.getInstance().getReference("messages")
        
        conversationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conversations = mutableListOf<Conversation>()
                
                for (conversationSnapshot in snapshot.children) {
                    val conversationId = conversationSnapshot.key ?: continue
                    
                    // Check if current user is a participant
                    val participants = conversationSnapshot.child("participants")
                        .getValue(object : GenericTypeIndicator<Map<String, Boolean>>() {})
                        ?: continue
                        
                    if (!participants.containsKey(currentUserId)) continue
                    
                    // Get the other user's ID
                    val otherUserId = participants.keys.first { it != currentUserId }
                    
                    // Get the last message
                    val lastMessageSnapshot = conversationSnapshot.child("messages")
                        .children
                        .lastOrNull()
                    val lastMessage = lastMessageSnapshot?.getValue(Message::class.java)
                    
                    // Only add conversations that have messages
                    if (lastMessage == null) continue
                    
                    // Get other user's details
                    FirebaseDatabase.getInstance().getReference("users")
                        .child(otherUserId)
                        .get()
                        .addOnSuccessListener { userSnapshot ->
                            val user = userSnapshot.getValue(User::class.java)
                            user?.let {
                                val conversation = Conversation(
                                    id = conversationId,
                                    participants = participants,
                                    lastMessage = lastMessage,
                                    otherUserName = it.fullName,
                                    otherUserProfilePicture = it.profilePicture
                                )
                                conversations.add(conversation)
                                val sortedConversations = conversations.sortedByDescending { it.lastMessage?.timestamp ?: 0 }
                                adapter.submitList(sortedConversations)
                                
                                // Show/hide no messages text
                                tvNoMessages.visibility = if (sortedConversations.isEmpty()) View.VISIBLE else View.GONE
                            }
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }
}

class ConversationAdapter(
    private val onConversationClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    private var conversations: List<Conversation> = emptyList()

    fun submitList(newList: List<Conversation>) {
        conversations = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount() = conversations.size

    inner class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivProfilePicture = itemView.findViewById<ImageView>(R.id.ivProfilePicture)
        private val tvName = itemView.findViewById<TextView>(R.id.tvName)
        private val tvLastMessage = itemView.findViewById<TextView>(R.id.tvLastMessage)
        private val tvTimestamp = itemView.findViewById<TextView>(R.id.tvTimestamp)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onConversationClick(conversations[position])
                }
            }
        }

        fun bind(conversation: Conversation) {
            tvName.text = conversation.otherUserName
            tvLastMessage.text = conversation.lastMessage?.text ?: "No messages yet"
            
            // Format timestamp
            conversation.lastMessage?.timestamp?.let { timestamp ->
                val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                tvTimestamp.text = dateFormat.format(Date(timestamp))
            } ?: run {
                tvTimestamp.text = ""
            }

            // Load profile picture
            if (conversation.otherUserProfilePicture.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(Constants.SERVER_IMAGES_URL + conversation.otherUserProfilePicture)
                    .into(ivProfilePicture)
            }
        }
    }
} 