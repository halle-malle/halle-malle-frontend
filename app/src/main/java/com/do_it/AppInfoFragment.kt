package com.do_it

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.do_it.databinding.FragmentAppInfoBinding

class AppInfoFragment : Fragment(R.layout.fragment_app_info) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = FragmentAppInfoBinding.bind(view)
        val privacy = arguments?.getString("page") == "privacy"
        val title = if (privacy) "개인정보 처리방침" else "이용약관"
        binding.tvInfoTitle.text = title
        (activity as? AppCompatActivity)?.supportActionBar?.title = title
        binding.tvInfoBody.text = getString(if (privacy) R.string.mypage_privacy_body else R.string.mypage_terms_body)
    }
}
