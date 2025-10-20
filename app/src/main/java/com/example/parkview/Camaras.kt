package com.example.parkview

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Camaras : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var tvCurrentLocationCamaras: TextView
    private lateinit var tvStatusPasillo: TextView
    private lateinit var tvStatusSuperior: TextView
    private lateinit var btnVerCamaraPasillo: AppCompatButton
    private lateinit var btnVerCamaraSuperior: AppCompatButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_camaras, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()


        tvCurrentLocationCamaras = view.findViewById(R.id.tv_current_location_camaras)
        tvStatusPasillo = view.findViewById(R.id.tv_status_pasillo)
        tvStatusSuperior = view.findViewById(R.id.tv_status_superior)
        btnVerCamaraPasillo = view.findViewById(R.id.btn_ver_camara_pasillo)
        btnVerCamaraSuperior = view.findViewById(R.id.btn_ver_camara_superior)


        view.findViewById<AppCompatButton>(R.id.btn_regresar_camaras).setOnClickListener {
            findNavController().navigateUp()
        }


        loadLastLocation()
        setupCameraStatus()

        val settingsIcon = view.findViewById<ImageView>(R.id.settings_icon)
        settingsIcon.setOnClickListener {
            findNavController().navigate(R.id.action_camaras_to_settings)
        }

    }


    private fun loadLastLocation() {
        val userId = auth.currentUser?.uid ?: return
        val dbRef = database.getReference("locations").child(userId).child("last_location")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val description = snapshot.child("description").getValue(String::class.java)

                    if (description != null) {

                        val cleanDescription = description.replace("Plano ", "").trim()
                        tvCurrentLocationCamaras.text = "Ubicación Guardada: $cleanDescription"
                    } else {
                        tvCurrentLocationCamaras.text = "No hay ubicación guardada"
                    }
                } else {
                    tvCurrentLocationCamaras.text = "No hay ubicación guardada"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                tvCurrentLocationCamaras.text = "Error al cargar ubicación"
            }
        })
    }

    private fun setupCameraStatus() {

        tvStatusPasillo.text = "Estado: Online"
        tvStatusPasillo.setTextColor(Color.GREEN)
        btnVerCamaraPasillo.isEnabled = true


        btnVerCamaraPasillo.setOnClickListener {

            val action = CamarasDirections.actionCamarasToReproductorCamara(
                cameraName = "Pasillo",
                streamUrl = "simulated_back_camera"
            )
            findNavController().navigate(action)
        }


        tvStatusSuperior.text = "Estado: Online"
        tvStatusSuperior.setTextColor(Color.GREEN)
        btnVerCamaraSuperior.isEnabled = true

        btnVerCamaraSuperior.setOnClickListener {

            val action = CamarasDirections.actionCamarasToReproductorCamara(
                cameraName = "Vista Superior",
                streamUrl = "simulated_front_camera"
            )
            findNavController().navigate(action)
        }
    }
}