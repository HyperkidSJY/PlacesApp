package com.hyper.place.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.hyper.place.R
import com.hyper.place.databinding.ActivityMapsBinding
import com.hyper.place.models.HappyPlaceModel

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var binding: ActivityMapsBinding? = null
    private var mHappyPlaceDetails: HappyPlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        if(intent.hasExtra(MainActivity.EXTRA_PLACE_DETAILS)){
            mHappyPlaceDetails = intent.getParcelableExtra(MainActivity.EXTRA_PLACE_DETAILS) as? HappyPlaceModel
        }
        if(mHappyPlaceDetails != null){
            setSupportActionBar(binding?.toolbarMap)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title =  mHappyPlaceDetails!!.title

            binding?.toolbarMap?.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            val supportMapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            supportMapFragment.getMapAsync(this)
        }
    }


    override fun onMapReady(p0: GoogleMap?) {
        val position = LatLng(mHappyPlaceDetails!!.latitude,mHappyPlaceDetails!!.longitude)
        p0!!.addMarker(MarkerOptions().position(position).title(mHappyPlaceDetails!!.location))
        val newLatLngZoom = CameraUpdateFactory.newLatLngZoom(position,10f)
        p0.animateCamera(newLatLngZoom)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

}