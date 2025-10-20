package com.example.parkview

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        // Inflar el layout para este fragmento
        return inflater.inflate(R.layout.fragment_camaras, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Inicialización de Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // 2. Inicialización de Vistas
        tvCurrentLocationCamaras = view.findViewById(R.id.tv_current_location_camaras)
        tvStatusPasillo = view.findViewById(R.id.tv_status_pasillo)
        tvStatusSuperior = view.findViewById(R.id.tv_status_superior)
        btnVerCamaraPasillo = view.findViewById(R.id.btn_ver_camara_pasillo)
        btnVerCamaraSuperior = view.findViewById(R.id.btn_ver_camara_superior)

        // Configurar el botón de regreso
        view.findViewById<AppCompatButton>(R.id.btn_regresar_camaras).setOnClickListener {
            findNavController().navigateUp()
        }

        // 3. Cargar datos
        loadLastLocation()
        setupCameraStatus()
    }

    /**
     * Carga la última ubicación guardada por el usuario desde Firebase
     * y la muestra en el TextView del encabezado.
     */
    private fun loadLastLocation() {
        val userId = auth.currentUser?.uid ?: return
        val dbRef = database.getReference("locations").child(userId).child("last_location")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val description = snapshot.child("description").getValue(String::class.java)

                    if (description != null) {
                        // Limpiamos el texto para dejar solo las coordenadas o la ubicación clave
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

    /**
     * Simula la carga del estado de las cámaras, habilita el botón "Ver" de las cámaras
     * y configura la navegación correcta, enviando el argumento para el selector de cámara.
     */
    private fun setupCameraStatus() {
        // --- Cámara Pasillo (Cámara Trasera) ---
        tvStatusPasillo.text = "Estado: Online"
        tvStatusPasillo.setTextColor(Color.GREEN) // Color verde para Online
        btnVerCamaraPasillo.isEnabled = true

        // 1. Navegación para CÁMARA TRASERA
        btnVerCamaraPasillo.setOnClickListener {
            // Usamos Safe Args: cameraName = "Pasillo", streamUrl = "simulated_back_camera"
            val action = CamarasDirections.actionCamarasToReproductorCamara(
                cameraName = "Pasillo",
                streamUrl = "simulated_back_camera" // Indica a ReproductorCamara que use la cámara trasera
            )
            findNavController().navigate(action)
        }

        // --- Cámara Vista superior (Cámara Frontal) ---
        // **CORRECCIÓN:** Se fuerza a Online y se habilita el botón
        tvStatusSuperior.text = "Estado: Online"
        tvStatusSuperior.setTextColor(Color.GREEN) // Color verde para Online
        btnVerCamaraSuperior.isEnabled = true // HABILITADO

        // 2. Navegación para CÁMARA FRONTAL
        btnVerCamaraSuperior.setOnClickListener {
            // Usamos Safe Args: cameraName = "Vista Superior", streamUrl = "simulated_front_camera"
            val action = CamarasDirections.actionCamarasToReproductorCamara(
                cameraName = "Vista Superior",
                streamUrl = "simulated_front_camera" // Indica a ReproductorCamara que use la cámara frontal
            )
            findNavController().navigate(action)
        }
    }
}