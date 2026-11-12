package com.do_it

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.do_it.MyApplication.Companion.db

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.do_it.databinding.FragmentMapBinding

class mapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    lateinit private var binding: FragmentMapBinding

    private val groupList = mutableListOf<GroupData>()   // 불러온 모임 목록
    private var filter = "전체"   // 선택한 필터

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 모임 만들기 화면으로 이동
        binding.createMeeting.setOnClickListener {
            val intent = Intent(requireContext(), CreateMeetingActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // 처음 보여줄 위치 (동양미래대 8호관)
        val location = LatLng(37.5000, 126.8663)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15.0f))

        // 모집중(0), 확정(1)인 모임 불러와서 목록에 저장한 뒤 핀 찍기
        loadGroups()

        // 새로고침 버튼 (지도가 준비된 뒤에 눌러야 핀을 찍을 수 있어서 여기서 등록)
        binding.refresh.setOnClickListener {
            loadGroups()
            Toast.makeText(context, "새로고침", Toast.LENGTH_SHORT).show()
        }

        // 카테고리 필터 (지도가 준비된 뒤에 눌러야 핀을 다시 찍을 수 있어서 여기서 등록)
        binding.filterSports.setOnClickListener {
            selectFilter(binding.filterSports, "운동")
        }
        binding.filterStudy.setOnClickListener {
            selectFilter(binding.filterStudy, "스터디")
        }
        binding.filterFood.setOnClickListener {
            selectFilter(binding.filterFood, "맛집")
        }
        binding.filterHobby.setOnClickListener {
            selectFilter(binding.filterHobby, "취미")
        }
        binding.filterEtc.setOnClickListener {
            selectFilter(binding.filterEtc, "기타")
        }

        // 핀을 누르면 바텀시트로 모임 정보 보여주기
        mMap.setOnMarkerClickListener { marker ->
            val groupId = marker.tag as String?
            if (groupId != null) {
                val bundle = bundleOf("groupId" to groupId)

                val sheet = PinBottomSheetFragment()
                sheet.arguments = bundle
                sheet.setStyle(DialogFragment.STYLE_NORMAL, R.style.AppBottomSheetDialogBorder20WhiteTheme)
                sheet.show(parentFragmentManager, "pin")
            }
            true
        }
    }

    // 필터 선택 : 같은 필터를 다시 누르면 전체 보기
    private fun selectFilter(selected: TextView, name: String) {
        val filterList = listOf(binding.filterSports, binding.filterStudy, binding.filterFood, binding.filterHobby, binding.filterEtc)
        for (f in filterList) {
            f.setBackgroundResource(R.drawable.bg_filter_unselected)
            f.setTextColor(Color.BLACK)
        }

        if (filter == name) {
            filter = "전체"
        } else {
            filter = name
            selected.setBackgroundResource(R.drawable.bg_filter_selected)
            selected.setTextColor(Color.WHITE)
        }
        showMarkers()
    }

    // Firestore에서 모집중(0), 확정(1)인 모임을 불러와서 핀 다시 찍기
    private fun loadGroups() {
        val task = db.collection("groups").whereIn("status", listOf(0, 1)).get()
        task.addOnSuccessListener {
            groupList.clear()
            for (document in it) {
                val item = document.toObject(GroupData::class.java)
                groupList.add(item)
            }
            showMarkers()
        }
        task.addOnFailureListener {
            Toast.makeText(context, "모임 불러오기 실패", Toast.LENGTH_SHORT).show()
        }
    }

    // 목록에 있는 모임 중 필터에 맞는 것만 핀 찍기
    private fun showMarkers() {
        mMap.clear()   // 이전 핀 지우기

        for (item in groupList) {
            // 위치가 없는 모임은 건너뛰기
            if (item.latitude == 0.0 && item.longitude == 0.0) continue
            // 전체가 아니면 선택한 카테고리만
            if (filter != "전체" && item.category != filter) continue

            val options = MarkerOptions()
            options.position(LatLng(item.latitude, item.longitude))
            options.title(item.title)
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            val marker = mMap.addMarker(options)
            marker?.tag = item.id   // 핀에 모임 id 저장
        }
    }
}
