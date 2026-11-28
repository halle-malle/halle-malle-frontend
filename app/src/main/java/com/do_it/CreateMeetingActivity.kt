package com.do_it

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.text.format.DateFormat
import android.view.View
import android.widget.DatePicker
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.util.Calendar

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.do_it.databinding.ActivityCreateMeetingBinding
import com.do_it.databinding.MarkerLayoutBinding

class CreateMeetingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityCreateMeetingBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var callback: LocationCallback? = null
    private var selectedLatLng: LatLng? = null   // 지도에서 찍은 위치
    private var category = "운동"   // 선택한 카테고리 (XML에서 운동이 처음 선택된 상태)
    private var date: LocalDate = LocalDate.now()   // 선택한 날짜
    private val calendar = Calendar.getInstance()   // 선택한 시간 저장 (처음엔 현재 시간)
    private var dateTime = 0L   // 선택한 모임 일시 (0이면 아직 선택 안 함)

    // 위치 선택 화면에서 찍은 위치, 주소 받기
    private val locationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            val rIntent = it.data!!
            val latitude = rIntent.getDoubleExtra("latitude", 0.0)
            val longitude = rIntent.getDoubleExtra("longitude", 0.0)
            selectedLatLng = LatLng(latitude, longitude)
            binding.tvAddress.text = rIntent.getStringExtra("address") ?: ""
            binding.etPlaceName.setText(rIntent.getStringExtra("placeName") ?: "")

            // 미리보기 지도에 고른 위치 핀 찍고 그 위치 보여주기
            mMap.clear()
            val options = MarkerOptions()
            options.position(selectedLatLng!!)
            options.icon(BitmapDescriptorFactory.fromBitmap(createMarker()))
            mMap.addMarker(options)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLng!!, 16.0f))
        }
    }

    // 위치 선택 화면 열기 (핀에 보여줄 모임 이름, 카테고리도 같이 보내기)
    private fun openSelectLocation() {
        val intent = Intent(this, SelectLocationActivity::class.java)
        intent.putExtra("title", binding.etMeetingTitle.text.toString())
        intent.putExtra("category", category)
        locationLauncher.launch(intent)
    }

    // 입력한 모임 이름 들어간 핀 만들기 (이름을 안 썼으면 "모임 핀")
    private fun createMarker(): Bitmap {
        val markerBinding = MarkerLayoutBinding.inflate(layoutInflater)

        var title = binding.etMeetingTitle.text.toString().trim()
        if (title.isEmpty()) title = "모임 핀"

        // 카테고리 이모지 모임 이름 앞에 붙이기
        val emoji = when (category) {
            "운동" -> "🏸"
            "스터디" -> "💻"
            "맛집" -> "🍕"
            "취미" -> "🎸"
            else -> "💬"
        }
        markerBinding.tvMarker.text = emoji + " " + title

        // 핀 아이콘은 Bitmap만 넣을 수 있어서 XML을 Bitmap으로 바꾸기
        val view = markerBinding.root
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
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

        // 미리보기 지도 연결
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.acmMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 위치 지정 : 주소 줄(지정 버튼 포함)을 누르면 위치 선택 화면으로 이동
        binding.layoutLocation.setOnClickListener {
            openSelectLocation()
        }

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

        // 모임 만들기 버튼
        binding.btnCreate.setOnClickListener {
            val title = binding.etMeetingTitle.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(baseContext, "모임 이름을 입력해주세요.", Toast.LENGTH_LONG).show()
            } else {
                val uid = MyApplication.auth.currentUser?.uid ?: return@setOnClickListener
                if (dateTime == 0L) {
                    Toast.makeText(baseContext, "모임 일시를 선택해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (dateTime < System.currentTimeMillis()) {
                    Toast.makeText(baseContext, "지난 시간은 선택할 수 없어요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val latLng = selectedLatLng
                if (latLng == null) {
                    Toast.makeText(baseContext, "모임 장소를 지정해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val placeName = binding.etPlaceName.text.toString().trim()
                if (placeName.isEmpty()) {
                    Toast.makeText(baseContext, "장소 이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
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
                    place = placeName,
                    address = binding.tvAddress.text.toString(),
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
                    setResult(RESULT_OK)   // 탐색 화면에서 새 핀 다시 불러오게
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
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 16.0f))

        // 지도를 누르면 위치 선택 화면으로 이동
        mMap.setOnMapClickListener {
            openSelectLocation()
        }

        showMyLocation()
    }

    // 현재 위치 보여주기 (위치 권한은 탐색 화면에서 이미 받아서 여기선 확인만)
    private fun showMyLocation() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return

        // 내 위치 파란 원 표시 (내 위치로 이동 버튼은 숨기기)
        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = false

        callback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                val location = p0.lastLocation
                val current = LatLng(location?.latitude!!, location?.longitude!!)

                // 아직 장소를 안 골랐으면 현재 위치로 카메라 이동하고 위치 받기 중지
                if (selectedLatLng == null)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 16.0f))
                fusedLocationClient.removeLocationUpdates(this)
            }
        }

        val builder = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
        builder.setMinUpdateDistanceMeters(1.0f)
        val request = builder.build()
        fusedLocationClient.requestLocationUpdates(request, callback!!, Looper.getMainLooper())
    }
}
