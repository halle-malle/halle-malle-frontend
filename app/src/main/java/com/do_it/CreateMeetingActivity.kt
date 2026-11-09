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

        // 지도에 핀 찍기 버튼
        binding.btnPin.setOnClickListener {
            val title = binding.etMeetingTitle.text.toString()
            if (title.isEmpty()) {
                Toast.makeText(baseContext, "모임 이름을 입력해주세요.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(baseContext, "모임이 생성되었습니다!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val location = LatLng(37.5000, 126.8663)
        val options = MarkerOptions()
        options.position(location)
        options.title("동양미래대학교 8호관")
        val bitmapDescriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        options.icon(bitmapDescriptor)
        mMap.addMarker(options)

        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(location, 17.0f)
        mMap.moveCamera(cameraUpdate)
    }
}
