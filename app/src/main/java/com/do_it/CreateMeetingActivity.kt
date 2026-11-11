package com.do_it

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.do_it.databinding.ActivityCreateMeetingBinding

class CreateMeetingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityCreateMeetingBinding
    private var selectedLatLng: LatLng? = null   // 지도에서 찍은 위치

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateMeetingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 툴바 뒤로가기
        setSupportActionBar(binding.acmToolbar)
        binding.acmToolbar.setNavigationOnClickListener {
            finish()
        }

        // 구글 지도 연결
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.acmMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 재설정 버튼
        binding.resetLocation.setOnClickListener {
            finish()
        }

        binding.mapTouchBlocker.setOnTouchListener { v, event ->
            binding.scrollView.requestDisallowInterceptTouchEvent(true)   // 지도 만지는 동안 스크롤 금지
            false   // 터치는 아래 지도로 넘기기
        }

        // 지도에 핀 찍기 버튼
        binding.btnPin.setOnClickListener {
            val title = binding.etMeetingTitle.text.toString()
            if (title.isEmpty()) {
                Toast.makeText(baseContext, "모임 이름을 입력해주세요.", Toast.LENGTH_LONG).show()
            } else {
                val uid = MyApplication.auth.currentUser?.uid ?:
                return@setOnClickListener
                val latLng = selectedLatLng
                if (latLng == null) {
                    Toast.makeText(baseContext, "지도에 핀을 찍어주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val ref = MyApplication.db.collection("groups").document()

                val group = GroupData(
                    id = ref.id,
                    memberIds = listOf(uid),
                    hostId = uid,
                    title = title,
                    description = binding.etDetail.text.toString(),
                    maxMember = binding.seekBarMember.progress,
                    memberCount = 1,
                    createdAt = System.currentTimeMillis(),
                    latitude = latLng.latitude,
                    longitude = latLng.longitude,
                )

                ref.set(group).addOnSuccessListener {
                    ref.collection("members").document(uid).set(memberData(
                        meetingId = ref.id,
                        userId = uid,
                        role = memberData.ROLE_HOST,
                        joinedAt = System.currentTimeMillis()
                        )
                    )
                    Toast.makeText(baseContext, "모임이 생성되었습니다!", Toast.LENGTH_SHORT).show()
                    finish()
                }.addOnFailureListener {
                    Toast.makeText(baseContext, "모임 생성 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap

        // 처음 보여줄 위치 (동양미래대 8호관)
        val location = LatLng(37.5000, 126.8663)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 17.0f))

        // 지도를 터치하면 그 위치에 핀 찍기
        mMap.setOnMapClickListener { latLng ->
            mMap.clear()   // 이전 핀 지우기

            val options = MarkerOptions()
            options.position(latLng)
            options.title("모임 장소")
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            mMap.addMarker(options)

            selectedLatLng = latLng   // 찍은 위치 저장
        }
    }
}
