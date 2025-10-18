package com.example.parkview

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Camaras : Fragment() {

    private lateinit var database: FirebaseDatabase

    // Declaramos todos los elementos de la UI que vamos a controlar
    private lateinit var btnVerCamaraPasillo: AppCompatButton
    private lateinit var btnVerCamaraSuperior: AppCompatButton
    private lateinit var tvStatusPasillo: TextView
    private lateinit var tvStatusSuperior: TextView


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_camaras, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase.getInstance()

        // Inicializamos las vistas con sus IDs del XML
        btnVerCamaraPasillo = view.findViewById(R.id.btn_ver_camara_pasillo)
        btnVerCamaraSuperior = view.findViewById(R.id.btn_ver_camara_superior)
        tvStatusPasillo = view.findViewById(R.id.tv_status_pasillo)
        tvStatusSuperior = view.findViewById(R.id.tv_status_superior)


        // Navegación
        val settingsIcon = view.findViewById<ImageView>(R.id.settings_icon)
        settingsIcon.setOnClickListener {
            findNavController().navigate(R.id.action_camaras_to_settings)
        }

        val btnRegresar = view.findViewById<AppCompatButton>(R.id.btn_regresar_camaras)
        btnRegresar.setOnClickListener {
            findNavController().popBackStack()
        }

        // Cargamos los datos de las cámaras desde Firebase
        loadCameraData()
    }

    private fun loadCameraData() {
        val dbRef = database.getReference("cameras")

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(context, "No se encontraron datos de cámaras en Firebase.", Toast.LENGTH_LONG).show()
                    return
                }

                // --- Procesar Cámara del Pasillo ---
                val camPasillo = snapshot.child("cam_pasillo")
                val pasilloName = camPasillo.child("name").getValue(String::class.java)
                val pasilloStatus = camPasillo.child("status").getValue(String::class.java)
                val pasilloUrl = camPasillo.child("streamUrl").getValue(String::class.java)

                if (pasilloStatus == "Online") {
                    tvStatusPasillo.text = "Estado: Online"
                    tvStatusPasillo.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
                    btnVerCamaraPasillo.isEnabled = true
                    btnVerCamaraPasillo.setOnClickListener {
                        val bundle = bundleOf(
                            "cameraName" to pasilloName,
                            "streamUrl" to pasilloUrl
                        )
                        findNavController().navigate(R.id.action_camaras_to_reproductorCamara, bundle)
                    }
                } else {
                    tvStatusPasillo.text = "Estado: Offline"
                    tvStatusPasillo.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                    btnVerCamaraPasillo.isEnabled = false
                }

                // --- Procesar Cámara Superior ---
                val camSuperior = snapshot.child("cam_superior")
                val superiorName = camSuperior.child("name").getValue(String::class.java)
                val superiorStatus = camSuperior.child("status").getValue(String::class.java)
                val superiorUrl = camSuperior.child("streamUrl").getValue(String::class.java)

                if (superiorStatus == "Online") {
                    tvStatusSuperior.text = "Estado: Online"
                    tvStatusSuperior.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
                    btnVerCamaraSuperior.isEnabled = true
                    btnVerCamaraSuperior.setOnClickListener {
                        val bundle = bundleOf(
                            "cameraName" to superiorName,
                            "streamUrl" to superiorUrl
                        )
                        findNavController().navigate(R.id.action_camaras_to_reproductorCamara, bundle)
                    }
                } else {
                    tvStatusSuperior.text = "Estado: Offline"
                    tvStatusSuperior.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                    btnVerCamaraSuperior.isEnabled = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error al cargar datos: ${error.message}", Toast.LENGTH_LONG).show()
            }
        })
    }
}