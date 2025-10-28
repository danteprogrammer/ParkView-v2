package com.example.parkview

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class GoogleMaps : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                enableMyLocation()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permiso de ubicación denegado.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
        return inflater.inflate(R.layout.fragment_google_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnRegresar = view.findViewById<Button>(R.id.btn_regresar_maps)
        btnRegresar.isSelected = true
        btnRegresar.setOnClickListener {
            findNavController().popBackStack()
        }

        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val btnNavegarExterno = view.findViewById<Button>(R.id.btn_iniciar_navegacion_externa)
        btnNavegarExterno.setOnClickListener {
            val latitud = -12.121852
            val longitud = -77.030367
            val etiquetaMapa = "Estacionamiento (Parque Kennedy)"

            val gmmIntentUri = Uri.parse("google.navigation:q=$latitud,$longitud")

            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            mapIntent.setPackage("com.google.android.apps.maps")

            if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(mapIntent)
            } else {
                Toast.makeText(requireContext(), "Google Maps no está instalado.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        val latitud = -12.121852
        val longitud = -77.030367
        val ubicacionEstacionamiento = LatLng(latitud, longitud)
        val etiqueta = "Estacionamiento (Parque Kennedy)"

        googleMap?.addMarker(
            MarkerOptions()
                .position(ubicacionEstacionamiento)
                .title(etiqueta)
        )
        googleMap?.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                ubicacionEstacionamiento,
                15f
            )
        )
        googleMap?.uiSettings?.isZoomControlsEnabled = true

    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableMyLocation()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(
                    requireContext(),
                    "Necesitamos tu ubicación para mostrarte en el mapa.",
                    Toast.LENGTH_LONG
                ).show()
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                }
            }
        } else {
            checkLocationPermission()
        }
    }

    override fun onResume() {
        super.onResume(); mapView.onResume()
    }

    override fun onStart() {
        super.onStart(); mapView.onStart()
    }

    override fun onStop() {
        super.onStop(); mapView.onStop()
    }

    override fun onPause() {
        super.onPause(); mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy(); mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory(); mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState)
    }
}