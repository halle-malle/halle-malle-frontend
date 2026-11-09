package com.do_it

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.do_it.databinding.ActivityGroupDetailBinding
import com.do_it.databinding.ActivityMainBinding
import com.do_it.databinding.FragmentMypageBinding

class GroupDetailActivity : AppCompatActivity() {

    private lateinit var adapter: detailMemberAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val binding = ActivityGroupDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val rIntent = getIntent()

        binding.bckBtnFrmGroupDetail.setOnClickListener{
            finish()
        }

        binding.btnGroupChat.setOnClickListener {
            val intent: Intent = Intent(this, ::class.java)
            startActivity(intent)
        }
        var item = listOf(
            memberData (
                name = "홍길동",
                isHost = "방장",
                intro = "안녕하세요 잘 부탁드릴게요~"
            ),
            memberData (
                name = "김수한",
                isHost = "방장 아님",
                intro = "안녕하세요 잘 부탁드릴게요~"
            )
        )

        adapter = detailMemberAdapter(item)
        binding.rvMember.layoutManager = LinearLayoutManager(this)
        binding.rvMember.adapter = adapter
        binding.rvMember.addItemDecoration(DividerItemDecoration(this,
            LinearLayoutManager.VERTICAL))


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}