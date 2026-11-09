package com.do_it

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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


        setContentView(R.layout.activity_group_chat)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}