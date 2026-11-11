package com.do_it

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

        // 모집중(0), 확정(1)인 모임 불러와서 핀 찍기
        val task = db.collection("groups").whereIn("status", listOf(0, 1)).get()
        task.addOnSuccessListener {
            for (document in it) {
                val item = document.toObject(GroupData::class.java)

                // 위치가 없는 모임은 건너뛰기
                if (item.latitude == 0.0 && item.longitude == 0.0) continue

                val options = MarkerOptions()
                options.position(LatLng(item.latitude, item.longitude))
                options.title(item.title)
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                mMap.addMarker(options)
            }
        }
        task.addOnFailureListener {
            Toast.makeText(context, "모임 불러오기 실패", Toast.LENGTH_SHORT).show()
        }
    }
}
