package com.do_it

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.do_it.MyApplication.Companion.db
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import java.io.IOException

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.do_it.databinding.ActivitySelectLocationBinding
import com.do_it.databinding.MarkerLayoutBinding

// 모임 장소 고르는 화면 (탐색 화면 지도와 같은 구조)
class SelectLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivitySelectLocationBinding

    private val groupList = mutableListOf<GroupData>()   // 불러온 모임 목록

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var callback: LocationCallback? = null

    private var current: LatLng? = null    // 내 위치

    private var selectedLatLng: LatLng? = null   // 지도에서 찍은 위치
    private var address = ""   // 핀 찍은 곳 주소
    private var pinTitle = ""   // 내 핀에 보여줄 모임 이름
    private var pinCategory = ""   // 내 핀에 보여줄 카테고리

    // 장소 검색 화면에서 고른 장소로 지도 이동 (핀은 지도를 눌러서 찍기)
    private val placeLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            val place = Autocomplete.getPlaceFromIntent(it.data!!)
            val latLng = place.location
            if (latLng != null) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17.0f))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 툴바 뒤로가기
        setSupportActionBar(binding.slToolbar)
        binding.slToolbar.setNavigationOnClickListener {
            finish()
        }

        // 모임 만들기 화면에서 보낸 모임 이름, 카테고리 (이름을 안 썼으면 "모임 핀")
        pinTitle = intent.getStringExtra("title") ?: ""
        if (pinTitle.isEmpty()) pinTitle = "모임 핀"
        pinCategory = intent.getStringExtra("category") ?: ""

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.slMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 장소 검색 (키는 매니페스트에 있는 지도 키와 같은 키)
        Places.initialize(applicationContext, "AIzaSyDu4ZrWAQXuL7MbIaizNklNw5bmA7S8DCk")
        binding.layoutPlaceSearch.setOnClickListener {
            val fields = listOf(Place.Field.LOCATION)
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields).build(this)
            placeLauncher.launch(intent)
        }

        // 핀 찍기 버튼 : 찍은 위치, 주소를 모임 만들기 화면으로 보내기
        binding.btnSelect.setOnClickListener {
            val latLng = selectedLatLng
            if (latLng == null) {
                Toast.makeText(baseContext, "지도에 핀을 찍어주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val rIntent = Intent()
            rIntent.putExtra("latitude", latLng.latitude)
            rIntent.putExtra("longitude", latLng.longitude)
            rIntent.putExtra("address", address)
            setResult(RESULT_OK, rIntent)
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // 처음 보여줄 위치 (동양미래대 8호관)
        val location = LatLng(37.5000, 126.8663)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15.0f))

        // 모집중(0), 확정(1)인 모임 불러와서 목록에 저장한 뒤 핀 찍기
        loadGroups()

        // 내 위치 표시
        showMyLocation()

        // 새로고침 버튼 (지도가 준비된 뒤에 눌러야 핀을 찍을 수 있어서 여기서 등록)
        binding.refresh.setOnClickListener {
            loadGroups()
            Toast.makeText(baseContext, "새로고침", Toast.LENGTH_SHORT).show()
        }

        // 지도를 터치하면 그 위치에 핀 찍기
        mMap.setOnMapClickListener { latLng ->
            selectedLatLng = latLng   // 찍은 위치 저장
            showMarkers()
            showAddress(latLng)
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        }
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
            Toast.makeText(baseContext, "모임 불러오기 실패", Toast.LENGTH_SHORT).show()
        }
    }

    // 목록에 있는 모임 핀 + 내가 찍은 핀 찍기
    private fun showMarkers() {
        mMap.clear()   // 이전 핀 지우기

        for (item in groupList) {
            // 위치가 없는 모임은 건너뛰기
            if (item.latitude == 0.0 && item.longitude == 0.0) continue

            val options = MarkerOptions()
            options.position(LatLng(item.latitude, item.longitude))
            options.icon(BitmapDescriptorFactory.fromBitmap(createMarker(item.title, item.category)))
            mMap.addMarker(options)
        }

        // 내가 찍은 핀
        val latLng = selectedLatLng
        if (latLng != null) {
            val options = MarkerOptions()
            options.position(latLng)
            options.icon(BitmapDescriptorFactory.fromBitmap(createMarker(pinTitle, pinCategory)))
            mMap.addMarker(options)
        }
    }

    // 찍은 위치의 주소 보여주기 (위도, 경도 → 주소)
    private fun showAddress(latLng: LatLng) {
        val geocoder = Geocoder(this)
        try {
            val list = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (list != null && list.isNotEmpty()) {
                address = list[0].getAddressLine(0)
                binding.tvAddress.text = address
            }
        } catch (e: IOException) {
            address = ""
            binding.tvAddress.text = "주소를 불러올 수 없어요"
        }
    }

    // 모임 이름 들어간 핀 만들기
    private fun createMarker(title: String, category: String): Bitmap {
        val markerBinding = MarkerLayoutBinding.inflate(layoutInflater)

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
        // measure로 글자 길이에 맞는 크기 계산하고 그 크기 그대로 Bitmap 만들어서 박스 길이가 글자마다 달라짐
        val view = markerBinding.root
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    // 내 위치 보여주기 (위치 권한은 탐색 화면에서 이미 받아서 여기선 확인만)
    private fun showMyLocation() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return

        // 내 위치 파란 원 표시
        mMap.isMyLocationEnabled = true

        // 구글맵 기본 ui가 가려져서 padding 주기
        // setPadding은 px 단위이고 그럴 경우 폰 기종 마다 위치가 다르게 표시됨,
        // resources.displayMetrics.density를 이용 px, dp 변환
        val top = (130 * resources.displayMetrics.density).toInt()
        val bottom = (76 * resources.displayMetrics.density).toInt()
        mMap.setPadding(0, top, 0, bottom)

        callback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                val location = p0.lastLocation
                current = LatLng(location?.latitude!!, location?.longitude!!)

                // 처음 위치로 카메라 이동하고 위치 받기 중지
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current!!, 15.0f))
                fusedLocationClient.removeLocationUpdates(this)
            }
        }

        val builder = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
        builder.setMinUpdateDistanceMeters(1.0f)
        val request = builder.build()
        fusedLocationClient.requestLocationUpdates(request, callback!!, Looper.getMainLooper())
    }
}
