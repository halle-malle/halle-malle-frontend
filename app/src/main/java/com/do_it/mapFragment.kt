package com.do_it

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.do_it.MyApplication.Companion.db
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
import com.do_it.databinding.FragmentMapBinding
import com.do_it.databinding.MarkerLayoutBinding

class mapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    lateinit private var binding: FragmentMapBinding

    private val groupList = mutableListOf<GroupData>()   // 불러온 모임 목록
    private var filter = "전체"   // 선택한 필터

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    var callback: LocationCallback? = null

    private var current: LatLng? = null    // 내 위치

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 모임 만들기 화면으로 이동
        binding.createMeeting.setOnClickListener {
            val intent = Intent(requireContext(), CreateMeetingActivity::class.java)
            createLauncher.launch(intent)
        }
    }

    // 모임을 만들고 돌아오면 모임 다시 불러와서 새 핀 보이게
    private val createLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == AppCompatActivity.RESULT_OK) {
            loadGroups()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // 처음 보여줄 위치 (동양미래대 8호관)
        val location = LatLng(37.5000, 126.8663)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15.0f))

        // 모집중(0), 확정(1)인 모임 불러와서 목록에 저장한 뒤 핀 찍기
        loadGroups()

        // 위치 권한 확인 후 내 위치 표시
        checkLocationPermission()

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
            options.icon(BitmapDescriptorFactory.fromBitmap(createMarker(item)))
            val marker = mMap.addMarker(options)
            marker?.tag = item.id   // 핀에 모임 id 저장
        }
    }

    // 모임 이름 들어간 핀 만들기
    private fun createMarker(item: GroupData): Bitmap {
        val markerBinding = MarkerLayoutBinding.inflate(layoutInflater)

        // 카테고리 이모지 모임 이름 앞에 붙이기
        val emoji = when (item.category) {
            "운동" -> "🏸"
            "스터디" -> "💻"
            "맛집" -> "🍕"
            "취미" -> "🎸"
            else -> "💬"
        }
        markerBinding.tvMarker.text = emoji + " " + item.title

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

    private val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions(),
        object : ActivityResultCallback<Map<String, Boolean>> {
            override fun onActivityResult(result: Map<String, Boolean>) {
                var granted = true
                for (p in result.values)
                    granted = granted && p
                if (granted == true)
                    showMyLocation()
                else
                    Toast.makeText(context, "위치 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            }
        })

    private fun checkLocationPermission() {
        var array = arrayOf<String>()

        if (requireContext().checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            array = array.plus(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        if (requireContext().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            array = array.plus(android.Manifest.permission.ACCESS_FINE_LOCATION)

        if (array.size == 0) // 모든 권한이 이미 있는 경우
            showMyLocation()
        else // 권한 요청
            launcher.launch(array)
    }

    private fun showMyLocation() {
        if (requireContext().checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            requireContext().checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return

        // 내 위치 파란 원 표시
        mMap.isMyLocationEnabled = true

        // 구글맵 기본 ui가 가려져서 padding 주기
        // setPadding은 px 단위이고 그럴 경우 폰 기종 마다 위치가 다르게 표시됨,
        // resources.displayMetrics.density를 이용 px, dp 변환
        val top = (112 * resources.displayMetrics.density).toInt()
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
