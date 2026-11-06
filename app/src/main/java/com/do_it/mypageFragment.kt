package com.do_it

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.do_it.MyApplication.Companion.auth
import com.do_it.databinding.ActivityMainBinding
import com.do_it.databinding.FragmentMypageBinding

class mypageFragment : Fragment() {

    private var _binding: FragmentMypageBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMypageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 로그아웃 버튼
        binding.buttonLogout.setOnClickListener {

            // Firebase 로그아웃
            auth.signOut()

            // 로그인 액티비티 이동
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)

            // 메인 액티비티 종료
            requireActivity().finish()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}