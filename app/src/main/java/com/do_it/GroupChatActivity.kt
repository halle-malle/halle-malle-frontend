package com.do_it

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.do_it.MyApplication.Companion.auth
import com.do_it.MyApplication.Companion.db
import com.do_it.databinding.ActivityGroupChatBinding
import com.google.firebase.firestore.Query

class GroupChatActivity : AppCompatActivity() {
    companion object {
        private var visibleChat: GroupChatActivity? = null

        fun isViewing(groupId: String?, uid: String?): Boolean {
            val chat = visibleChat ?: return false
            return groupId != null && uid != null && chat.listening &&
                chat.roomId == groupId && chat.userId == uid
        }
    }

    private lateinit var binding: ActivityGroupChatBinding
    private lateinit var adapter: GroupChatAdapter
    private lateinit var chatQuery: Query
    private val chatList = mutableListOf<chatData>()
    private var roomId: String? = null
    private var userId: String? = null
    private var listening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        binding = ActivityGroupChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gIntent = getIntent()
        val groupId = gIntent.getStringExtra("groupId")   // 넘겨받은 모임 id
        val myId = auth.currentUser?.uid
        if (groupId == null || myId == null) {
            finish()
            return
        }

        roomId = groupId
        userId = myId

        binding.bckBtnFrmGroupChat.setOnClickListener{
            finish()
        }

        // 채팅방 제목 : 모임 이름과 인원
        db.collection("groups").document(groupId).get().addOnSuccessListener {
            val group = it.toObject(GroupData::class.java)
            if (group != null) {
                binding.textView2.text = "${group.title} ${group.memberCount}/${group.maxMember}"
            }
        }

        // 메시지에 같이 저장할 내 이름 (닉네임이 없으면 이메일 @ 앞부분)
        var myName = auth.currentUser?.email?.substringBefore("@") ?: ""   // users에 정보가 없을 때를 대비한 기본값
        db.collection("users").document(myId).get().addOnSuccessListener {
            val user = it.toObject(userData::class.java)
            if (user != null) {
                myName = if (user.nickname.isEmpty()) user.email.substringBefore("@") else user.nickname
            }
        }

        adapter = GroupChatAdapter(chatList, myId)

        binding.rvChat.layoutManager = LinearLayoutManager(this)
        binding.rvChat.adapter = adapter

        // 메시지는 groups/모임id/chats 에 저장
        val chatRef = db.collection("groups").document(groupId).collection("chats")

        binding.btnSend.setOnClickListener(object:View.OnClickListener {
            override fun onClick(v: View?) {
                if(binding.editTextText.text.toString().trim().isEmpty()) return

                chatRef.add(chatData(
                    groupId = groupId,
                    senderId = myId,
                    createdAt = System.currentTimeMillis(),
                    name = myName,
                    content = binding.editTextText.text.toString().trim()
                ))
                binding.editTextText.setText("")
            }
        })

        chatQuery = chatRef.orderBy("createdAt")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onStart() {
        super.onStart()
        if (!::chatQuery.isInitialized) return

        // Firestore removes this Activity-scoped listener when onStop runs.
        chatQuery.addSnapshotListener(this) { value, error ->
            if (error != null) {
                listening = false
                Log.w("GroupChat", "채팅 목록 구독 실패", error)
                Toast.makeText(this, "채팅을 불러오지 못했어요.", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            if (value == null) return@addSnapshotListener

            listening = true
            chatList.clear()
            for (document in value) {
                chatList.add(document.toObject(chatData::class.java))
            }
            adapter.notifyDataSetChanged()
            if (chatList.isNotEmpty()) {
                binding.rvChat.scrollToPosition(chatList.lastIndex)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::chatQuery.isInitialized) visibleChat = this
    }

    override fun onPause() {
        if (visibleChat === this) visibleChat = null
        super.onPause()
    }

    override fun onStop() {
        listening = false
        super.onStop()
    }
}