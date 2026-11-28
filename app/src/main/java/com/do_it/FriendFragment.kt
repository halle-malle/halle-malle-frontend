package com.do_it

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.do_it.databinding.FragmentFriendBinding

class FriendFragment : Fragment() {

    private lateinit var binding: FragmentFriendBinding
    private lateinit var friendAdapter: friendListAdapter
    private lateinit var invitationAdapter: friendInvitationListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFriendBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // 더미 데이터 (친구 데이터가 생기기 전 화면 확인용)
        val friendList = mutableListOf(
            userData(
                id = "u1",
                email = "daeun@test.com",
                nickname = "정다은"
            ),
            userData(
                id = "u2",
                email = "minsu@test.com",
                nickname = "김민수"
            )
        )

        friendAdapter = friendListAdapter(
            { item ->
                val intent: Intent = Intent(requireContext(), PersonalChatActivity::class.java)
                intent.putExtra("friendId", item.id)
                startActivity(intent)
            },
            { item ->
                // 친구 상세 화면 (아직 없음)
            },
            { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
        )

        binding.rvMyFriends.layoutManager =
            LinearLayoutManager(requireContext())
        binding.rvMyFriends.adapter = friendAdapter

//        friendAdapter.setItems(friendList)
/////////////////////////////////////////////////////////////////////////////////
        // 더미 데이터 (친구 요청이 생기기 전 화면 확인용)
        val friendInvitationList = mutableListOf(
            userData(
                id = "u3",
                email = "jiho@test.com",
                nickname = "이지호"
            ),
            userData(
                id = "u4",
                email = "seoyeon@test.com",
                nickname = "박서연"
            )
        )

        invitationAdapter = friendInvitationListAdapter(
            { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
        )

        binding.rvFriendInvitations.layoutManager =
            LinearLayoutManager(requireContext())
        binding.rvFriendInvitations.adapter = invitationAdapter

//        invitationAdapter.setItems(friendInvitationList)
    }

     //실제 Firestore 데이터를 쓸 때 주석 해제 (켜 두면 빈 목록이 더미 데이터를 덮어씀)
     override fun onResume() {
         super.onResume()
         friendAdapter.loadItems()
         invitationAdapter.loadItems()
     }
}
