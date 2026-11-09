package com.do_it

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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

        //친구탭 클릭 이벤트
        binding.menuFriends.setOnClickListener{
            findNavController().navigate(R.id.action_my_to_friendFragment)
        }
        //내가 연 모임 관리 탭 클릭 이벤트
        binding.menuCreatedMeetups.setOnClickListener {
            findNavController().navigate(R.id.action_my_to_group_ManagementFragment)
        }
        //지난 모임탭 클릭 이벤트
        binding.menuPastMeetups.setOnClickListener {
            findNavController().navigate(R.id.action_my_to_previous_meetingFragment)
        }
        // 알림 설정 클릭 이벤트
        binding.menuNotificationSettings.setOnClickListener {
            findNavController().navigate(R.id.action_my_to_notification_SettingsFragment)
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