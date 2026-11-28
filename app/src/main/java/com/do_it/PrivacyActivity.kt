package com.do_it

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.do_it.databinding.ActivityPrivacyBinding

// 개인정보 처리방침 화면
class PrivacyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrivacyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 툴바 뒤로가기
        setSupportActionBar(binding.privacyToolbar)
        binding.privacyToolbar.setNavigationOnClickListener {
            finish()
        }
    }
}
