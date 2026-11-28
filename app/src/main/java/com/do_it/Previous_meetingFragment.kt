package com.do_it

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.do_it.MyApplication.Companion.auth
import com.do_it.MyApplication.Companion.db
import com.do_it.databinding.FragmentPreviousMeetingBinding

class Previous_meetingFragment : Fragment() {
    private var _binding: FragmentPreviousMeetingBinding? = null
    private val binding get() = _binding!!
    private lateinit var meetingAdapter: listAdapter
    private var requestNumber = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPreviousMeetingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        meetingAdapter = listAdapter(
            { item ->
                val intent = Intent(requireContext(), GroupDetailActivity::class.java)
                intent.putExtra("groupId", item.id)
                startActivity(intent)
            },
            { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() },
            { count -> _binding?.tvMeetingCount?.text = "${count}개의 모임" }
        )
        binding.rvPastMeetings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPastMeetings.adapter = meetingAdapter
        binding.buttonRetry.setOnClickListener { loadPastMeetings() }
    }

    override fun onResume() {
        super.onResume()
        loadPastMeetings()
    }

    private fun loadPastMeetings() {
        val currentBinding = _binding ?: return
        val request = ++requestNumber
        val uid = auth.currentUser?.uid
        if (uid == null) {
            meetingAdapter.setItems(emptyList())
            currentBinding.progressLoading.visibility = View.GONE
            currentBinding.tvMeetingCount.text = "0개의 모임"
            currentBinding.tvListStatus.text = "로그인 후 참여했던 모임을 볼 수 있어요."
            currentBinding.tvListStatus.visibility = View.VISIBLE
            currentBinding.buttonRetry.visibility = View.GONE
            return
        }
        currentBinding.progressLoading.visibility = View.VISIBLE
        currentBinding.tvListStatus.visibility = View.GONE
        currentBinding.buttonRetry.visibility = View.GONE
        currentBinding.tvMeetingCount.text = "불러오는 중"
        db.collection("groups").whereArrayContains("memberIds", uid).get()
            .addOnCompleteListener { task ->
                if (_binding !== currentBinding || request != requestNumber || auth.currentUser?.uid != uid)
                    return@addOnCompleteListener
                currentBinding.progressLoading.visibility = View.GONE
                if (!task.isSuccessful) {
                    meetingAdapter.setItems(emptyList())
                    currentBinding.tvMeetingCount.text = "조회 실패"
                    currentBinding.tvListStatus.text = "목록을 불러오지 못했어요. 네트워크를 확인해주세요."
                    currentBinding.tvListStatus.visibility = View.VISIBLE
                    currentBinding.buttonRetry.visibility = View.VISIBLE
                    return@addOnCompleteListener
                }
                val meetings = mutableListOf<GroupData>()
                for (document in task.result) {
                    val item = document.toObject(GroupData::class.java).copy(id = document.id)
                    if (item.status == GroupData.STATUS_CANCELED) continue
                    if (item.status == GroupData.STATUS_FINISHED || (item.dateTime > 0L && item.dateTime + GroupData.END_TIME < System.currentTimeMillis())) {
                        meetings.add(item)
                    }
                }
                meetings.sortByDescending { it.dateTime }
                meetingAdapter.setItems(meetings)
                currentBinding.tvMeetingCount.text = "${meetings.size}개의 모임"
                if (meetings.isEmpty()) {
                    currentBinding.tvListStatus.text = "아직 지난 모임이 없어요."
                    currentBinding.tvListStatus.visibility = View.VISIBLE
                }
            }
    }

    override fun onDestroyView() {
        requestNumber++
        _binding?.rvPastMeetings?.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
