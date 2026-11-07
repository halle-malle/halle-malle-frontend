package com.do_it

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.do_it.MyApplication.Companion.auth
import com.do_it.databinding.ActivityLoginBinding
import com.do_it.databinding.ActivityMainBinding
import com.do_it.databinding.FragmentHomeBinding
import com.do_it.databinding.FragmentMypageBinding

class mypageFragment : Fragment() {

    private lateinit var binding: FragmentMypageBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMypageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.menuFriends.setOnClickListener{ //친구탭 클릭시 '친구 클릭' 이라는 토스트창이 나옴
            toast("친구 클릭")
        }
        //내가 연 모임 관리 탭 클릭 이벤트
        binding.menuCreatedMeetups.setOnClickListener {
            toast("내가 연 모임 관리")
        }
        //지난 모임탭 클릭 이벤트
        binding.menuPastMeetups.setOnClickListener {
            toast("지난 모임")
        }
        // 알림 설정 클릭 이벤트
        binding.menuNotificationSettings.setOnClickListener {
            toast("알림 설정")
        }

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
    //토스트 함수 제작
    fun toast(str : String){
        Toast.makeText(requireContext(),str,Toast.LENGTH_SHORT).show()
    }
}