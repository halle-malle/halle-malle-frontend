package com.do_it

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import com.do_it.databinding.FragmentHomeBinding

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

        val upcomingList = listOf(
            cardData(
                id = 3,
                title = "배드민턴 할래?",
                date = "10.23 (금) 17:00",
                place = "동양미래대학교 8호관 1층 대강당",
                memberCount = 4,
                maxMember = 10,
                newMessage = 2,
                status = 4
            ),
            cardData(
                id = 4,
                title = "배드민턴 할래?",
                date = "10.23 (금) 17:00",
                place = "동양미래대학교 8호관 1층 대강당",
                memberCount = 4,
                maxMember = 10,
                newMessage = 2,
                status = 4
            )

        )

        val joinedList = listOf(
            cardData(
                id= 1,
                title = "코딩 스터디 할래? 💻",
                date = "10.25 (일) 19:00",
                place = "동양미래대학교 도서관",
                status = 0,
            ),
            cardData(
                id= 1,
                title = "코딩 스터디 할래? 💻",
                date = "10.25 (일) 19:00",
                place = "동양미래대학교 도서관",
                status = 0,
            )

        )

        val createdList = listOf(
            cardData(
                id = 1,
                title = "러닝 할래?",
                date ="10.24 (토) 07:00",
                place = "안양천 산책로",
                status = 2,
                memberCount = 3,
                maxMember = 8
            )
        )


        binding.rvUpcoming.layoutManager =
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false)
        binding.rvUpcoming.adapter = upcomingListAdapter(upcomingList)
        PagerSnapHelper().attachToRecyclerView(binding.rvUpcoming)

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
