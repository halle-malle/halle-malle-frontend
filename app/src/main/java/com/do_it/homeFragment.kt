package com.do_it

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.do_it.MyApplication.Companion.auth
import com.do_it.MyApplication.Companion.db
import com.do_it.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class homeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var upcomingAdapter: upcomingListAdapter
    private lateinit var joinedAdapter: listAdapter
    private lateinit var createdAdapter: listAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val upcomingList = mutableListOf<GroupData>(

        )

        upcomingAdapter = upcomingListAdapter(
            { item ->
                val intent: Intent = Intent(requireContext(), GroupChatActivity::class.java)
                intent.putExtra("groupId", item.id)
                startActivity(intent)
            },
            { item ->
                val intent: Intent = Intent(requireContext(), GroupDetailActivity::class.java)
                intent.putExtra("groupId", item.id)
                startActivity(intent)
            },
            { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show()}
        )
        binding.rvUpcoming.adapter = upcomingAdapter
        PagerSnapHelper().attachToRecyclerView(binding.rvUpcoming)

        val joinedList = listOf(
            GroupData(
                id = "g2",
                title = "코딩 스터디 할래? 💻",
                dateTime = 1794736800000L,   // 2026.11.15 19:00
                place = "동양미래대학교 도서관"
            )
        )

        createdAdapter = listAdapter(
            {item ->
                // 누르면 모임 상세 화면으로
                val intent: Intent = Intent(requireContext(), GroupDetailActivity::class.java)
                intent.putExtra("groupId", item.id)
                startActivity(intent)
            },
            { message->
                Toast.makeText(context,message,Toast.LENGTH_LONG).show()
            },
            { count ->
                binding.textView7.text = "내가 연 모임(${count})"
            }
        )

        joinedAdapter = listAdapter(
            {item ->
                // 누르면 모임 상세 화면으로
                val intent: Intent = Intent(requireContext(), GroupDetailActivity::class.java)
                intent.putExtra("groupId", item.id)
                startActivity(intent)
            },
            { message->
                Toast.makeText(context,message,Toast.LENGTH_LONG).show()
            },
            { count ->
                binding.textView5.text = "참여 중인 모임(${count})"
            }
        )

        binding.rvUpcoming.layoutManager =
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false)


        binding.rvJoined.layoutManager =
            LinearLayoutManager(requireContext())
        binding.rvJoined.adapter = joinedAdapter
        binding.rvCreated.layoutManager =
            LinearLayoutManager(requireContext())
        binding.rvCreated.adapter = createdAdapter

    }

    // 화면에 다시 돌아올 때마다 새로 불러오기 (모임 삭제, 참여 후에도 바로 반영)
    override fun onResume() {
        super.onResume()
        upcomingAdapter.loadItems()
        joinedAdapter.loadJoinedItems()
        createdAdapter.loadItems()
    }

}
