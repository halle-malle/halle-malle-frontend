package com.do_it

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.do_it.databinding.FragmentHomeBinding

class homeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val joinedList = listOf(
            CardData(
                "코딩 스터디 할래? 💻",
                "10.25 (일) 19:00 | 동양미래대학교 도서관",
                "참여 중"
            ),
            CardData(
                "고척 돈까스 뿌실래?",
                "10.26 (월) 12:00 | 고척동 돈까스집",
                "참여 중"
            ),
            CardData(
                "보드게임 할래?",
                "10.28 (수) 18:00 | 학생회관 2층",
                "참여 중"
        )
        )

        val createdList = listOf(
            CardData(
                "러닝 할래?",
                "10.24 (토) 07:00 | 안양천 산책로",
                "멤버 3/8명"
            )
        )



        binding.rvJoined.layoutManager =
            LinearLayoutManager(requireContext())
        binding.rvJoined.adapter = CardAdapter(joinedList)

        binding.rvCreated.layoutManager =
            LinearLayoutManager(requireContext())
        binding.rvCreated.adapter = CardAdapter(createdList)
    }

    // Fragment 화면 제거 시 바인딩 해제
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
