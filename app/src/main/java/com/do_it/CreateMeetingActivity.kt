package com.do_it

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.DatePicker
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.util.Calendar

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
    private var category = "운동"   // 선택한 카테고리 (XML에서 운동이 처음 선택된 상태)
    private var date: LocalDate = LocalDate.now()   // 선택한 날짜
    private val calendar = Calendar.getInstance()   // 선택한 시간 저장 (처음엔 현재 시간)
    private var dateTime = 0L   // 선택한 모임 일시 (0이면 아직 선택 안 함)

    // 지도에 핀 찍기 (지도 터치, 현재 위치 버튼에서 사용)
    private fun setPin(latLng: LatLng) {
        mMap.clear()   // 이전 핀 지우기

        val options = MarkerOptions()
        options.position(latLng)
        options.title("모임 장소")
        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        mMap.addMarker(options)

        selectedLatLng = latLng   // 찍은 위치 저장
    }


    // 날짜를 고른 뒤 시간 선택창 띄우기
    private fun showTimeDialog() {
        val dialog = TimePickerDialog(this, { view, hourOfDay, minute ->
            // 고른 날짜와 시간을 calendar에 넣고 밀리초로 저장
            calendar.set(date.year, date.monthValue - 1, date.dayOfMonth, hourOfDay, minute)
            dateTime = calendar.timeInMillis
            binding.tvMeetingDate.text = DateFormat.format("yyyy.MM.dd EEEE HH:mm", dateTime)
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true)
        dialog.show()
    }

    // 누른 카테고리만 선택 모양으로 바꾸고 나머지는 미선택 모양으로
    private fun selectCategory(selected: TextView, name: String) {
        val catList = listOf(binding.catSports, binding.catStudy, binding.catFood, binding.catHobby, binding.catEtc)
        for (cat in catList) {
            cat.setBackgroundResource(R.drawable.bg_filter_unselected)
            cat.setTextColor(Color.BLACK)
        }
        selected.setBackgroundResource(R.drawable.bg_filter_selected)
        selected.setTextColor(Color.WHITE)
        category = name
    }

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

        // 카테고리 선택
        binding.catSports.setOnClickListener {
            selectCategory(binding.catSports, "운동")
        }
        binding.catStudy.setOnClickListener {
            selectCategory(binding.catStudy, "스터디")
        }
        binding.catFood.setOnClickListener {
            selectCategory(binding.catFood, "맛집")
        }
        binding.catHobby.setOnClickListener {
            selectCategory(binding.catHobby, "취미")
        }
        binding.catEtc.setOnClickListener {
            selectCategory(binding.catEtc, "기타")
        }

        // 일시 설정 : 날짜 선택, 시간 선택
        binding.layoutMeetingDate.setOnClickListener {
            val dialog = DatePickerDialog(this, object : DatePickerDialog.OnDateSetListener {
                override fun onDateSet(
                    view: DatePicker?,
                    year: Int,
                    month: Int,
                    dayOfMonth: Int
                ) {
                    date = LocalDate.of(year, month + 1, dayOfMonth)
                    showTimeDialog()
                }
            }, date.year, date.monthValue - 1, date.dayOfMonth)
            dialog.show()
        }


        binding.mapTouchBlocker.setOnTouchListener { v, event ->
            binding.scrollView.requestDisallowInterceptTouchEvent(true)   // 지도 만지는 동안 스크롤 금지
            false   // 터치는 아래 지도로 넘기기
        }

        // 모집 정원 : 슬라이더를 움직이면 숫자 바꾸기
        binding.seekBarMember.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvMemberCount.text = "${progress}명"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        // 재설정 버튼
        binding.resetLocation.setOnClickListener {
            finish()
        }

        // 지도에 핀 찍기 버튼
        binding.btnPin.setOnClickListener {
            val title = binding.etMeetingTitle.text.toString()
            if (title.isEmpty()) {
                Toast.makeText(baseContext, "모임 이름을 입력해주세요.", Toast.LENGTH_LONG).show()
            } else {
                val uid = MyApplication.auth.currentUser?.uid ?:
                return@setOnClickListener
                if (dateTime == 0L) {
                    Toast.makeText(baseContext, "모임 일시를 선택해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
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
                    category = category,
                    description = binding.etDetail.text.toString(),
                    dateTime = dateTime,
                    place = "동양미래대학교 인근",   // 주소 기능을 만들기 전까지 기본값
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
                        name = MyApplication.auth.currentUser?.email ?: "",
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
            setPin(latLng)
        }
    }
}
