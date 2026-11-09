package com.do_it

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

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

        val gochuckdom = LatLng(37.498230246814245, 126.8671058989656)
        val options = MarkerOptions()
        options.position(gochuckdom)
        options.title("고척 돔 구장")
        options.snippet("구일역에서 500미터")
        val bitmapDescriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
        options.icon(bitmapDescriptor)
        val marker = mMap.addMarker(options)
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(gochuckdom, 17.0f)
        mMap.moveCamera(cameraUpdate)
    }
}
