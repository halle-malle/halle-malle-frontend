package com.do_it

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.do_it.databinding.FragmentSearchBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

// 탐색 화면 검색 결과, 겹친 핀 모임을 목록으로 보여주는 바텀시트
class SearchBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentSearchBottomSheetBinding

    var title = ""   // 맨 위 제목
    var results = listOf<GroupData>()   // 검색 결과
    var onItemClick: (GroupData) -> Unit = {}   // 결과를 눌렀을 때 할 일 (탐색 화면에서 정함)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvSearchTitle.text = title

        // 홈 화면 모임 목록 어댑터 그대로 사용 (누르면 바텀시트 닫고 탐색 화면에 알려주기)
        val adapter = listAdapter({ item ->
            dismiss()
            onItemClick(item)
        }, {}, {})
        binding.rvSearch.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSearch.adapter = adapter
        adapter.setItems(results)
    }
}
