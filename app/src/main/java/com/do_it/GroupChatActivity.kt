package com.do_it

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.do_it.MyApplication.Companion.auth
import com.do_it.MyApplication.Companion.db
import com.do_it.databinding.ActivityGroupChatBinding
import com.do_it.databinding.ActivityGroupDetailBinding

class GroupChatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        val binding = ActivityGroupChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gIntent = getIntent()
        val groupId = gIntent.getStringExtra("groupId")   // 넘겨받은 모임 id
        val myId = auth.currentUser?.uid
        if (groupId == null || myId == null) {
            finish()
            return
        }

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

        val chatList = mutableListOf<chatData>()
        val adapter = GroupChatAdapter(chatList, myId)

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

        // 새 메시지가 올라올 때마다 목록 다시 보여주기 (보낸 시간 순서)
        chatRef.orderBy("createdAt").addSnapshotListener(this) { value, error ->
            if (value == null) return@addSnapshotListener

            chatList.clear()
            for (document in value) {
                chatList.add(document.toObject(chatData::class.java))
            }
            adapter.notifyDataSetChanged()
            binding.rvChat.scrollToPosition(chatList.size - 1)   // 맨 아래로
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}