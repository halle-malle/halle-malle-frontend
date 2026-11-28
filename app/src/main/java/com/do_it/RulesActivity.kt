package com.do_it

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.do_it.databinding.ActivityRulesBinding

// 사용 규칙 화면
class RulesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRulesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRulesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 툴바 뒤로가기
        setSupportActionBar(binding.rulesToolbar)
        binding.rulesToolbar.setNavigationOnClickListener {
            finish()
        }
    }
}
