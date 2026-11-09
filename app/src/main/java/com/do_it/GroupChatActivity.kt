package com.do_it

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.do_it.databinding.ActivityGroupChatBinding
import com.do_it.databinding.ActivityGroupDetailBinding

class GroupChatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        val binding = ActivityGroupChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val gIntent = getIntent()

        binding.bckBtnFrmGroupChat.setOnClickListener{
            finish()
        }

        val chatList = mutableListOf(
            chatData(
                name = "홍길동",
                content = "안녕하세요 홍길동입니다~~",
                time = "오후 2:32",
                type = chatData.TYPE_OTHER
            ),
            chatData(
                name = "이영희",
                content = "반가워요!",
                time = "오후 2:32",
                type = chatData.TYPE_OTHER
            ),
            chatData(
                content = "안녕하세요 저는 김철수입니다.",
                time = "오후 2:33",
                type = chatData.TYPE_MINE
            )
        )
        val adapter = GroupChatAdapter(chatList)

        binding.rvChat.scrollToPosition(chatList.size - 1)

        binding.btnSend.setOnClickListener(object:View.OnClickListener {
            override fun onClick(v: View?) {
                if(binding.editTextText.text.toString().trim().isEmpty()) return

                chatList.add(chatData(
                    content = binding.editTextText.text.toString().trim(),
                    time = "오후 2:33",
                    type = chatData.TYPE_MINE
                ))
                adapter.notifyDataSetChanged()
                binding.editTextText.setText("")
            }
        })

        binding.rvChat.layoutManager = LinearLayoutManager(this)
        binding.rvChat.adapter = adapter

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}