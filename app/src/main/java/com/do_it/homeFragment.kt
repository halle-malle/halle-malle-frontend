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

        val upcomingAdapter = upcomingListAdapter(
            { item -> Toast.makeText(context,"그룹 채팅 추가",Toast.LENGTH_SHORT).show()},
            { item ->
                val intent: Intent = Intent(requireContext(), GroupDetailActivity::class.java)
                startActivity(intent)
            },
            { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show()}
        )
        binding.rvUpcoming.adapter = upcomingAdapter
        PagerSnapHelper().attachToRecyclerView(binding.rvUpcoming)

        upcomingAdapter.loadItems()

        val joinedList = listOf(
            GroupData(
                id = "g2",
                title = "코딩 스터디 할래? 💻",
                dateTime = 1794736800000L,   // 2026.11.15 19:00
                place = "동양미래대학교 도서관"
            )
        )

        val createdList = listOf(
            GroupData(
                id = "g3",
                title = "러닝 할래?",
                dateTime = 1794607200000L,   // 2026.11.14 07:00
                place = "안양천 산책로",
                memberCount = 3,
                maxMember = 8
            )
        )

        binding.rvUpcoming.layoutManager =
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false)


        binding.rvJoined.layoutManager =
            LinearLayoutManager(requireContext())
        binding.rvJoined.adapter = listAdapter(joinedList){ item->
            Toast.makeText(context,"참여 중인 모임 : ${item.title}",Toast.LENGTH_LONG).show()
        }

        binding.rvCreated.layoutManager =
            LinearLayoutManager(requireContext())
        binding.rvCreated.adapter = listAdapter(createdList){item ->
            Toast.makeText(context,"내가 연 모임 : ${item.title}",Toast.LENGTH_LONG).show()
        }

    }

}
