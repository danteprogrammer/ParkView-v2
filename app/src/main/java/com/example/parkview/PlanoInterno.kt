package com.example.parkview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PlanoInterno : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var gridLayoutPlano: GridLayout
    private lateinit var rgPisosPlano: RadioGroup

    private var selectedFloor: Int = 1
    private var occupiedSpots = listOf<String>()
    private var mySpot: String? = null
    private lateinit var locationsRef: DatabaseReference
    private lateinit var locationsListener: ValueEventListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_plano_interno, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        gridLayoutPlano = view.findViewById(R.id.grid_layout_plano)
        rgPisosPlano = view.findViewById(R.id.rg_pisos_plano)

        setupFloorSelection()
        setupButtons(view)
        fetchOccupiedSpots()

        val settingsIcon = view.findViewById<ImageView>(R.id.settings_icon)
        settingsIcon.setOnClickListener {
            findNavController().navigate(R.id.action_save_location_to_settings)
        }

    }

    private fun setupFloorSelection() {
        rgPisosPlano.setOnCheckedChangeListener { _, checkedId ->
            selectedFloor = when (checkedId) {
                R.id.rb_piso2_plano -> 2
                R.id.rb_piso3_plano -> 3
                else -> 1
            }
            updateParkingGrid()
        }
    }

    private fun setupButtons(view: View) {
        val btnActualizar: AppCompatButton = view.findViewById(R.id.btn_actualizar_ubicacion)
        val btnBorrar: AppCompatButton = view.findViewById(R.id.btn_borrar_ubicacion_plano)
        val btnCamaras: AppCompatButton = view.findViewById(R.id.btn_ver_camaras_plano)
        val btnRegresar: AppCompatButton = view.findViewById(R.id.btn_regresar_plano)

        btnActualizar.setOnClickListener {
            findNavController().navigate(R.id.action_planoInterno_to_save_location)
        }

        btnBorrar.setOnClickListener {
            deleteCurrentUserLocation()
        }

        btnCamaras.setOnClickListener {
            findNavController().navigate(R.id.action_planoInterno_to_camaras)
        }

        btnRegresar.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun deleteCurrentUserLocation() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "No se pudo identificar al usuario.", Toast.LENGTH_SHORT).show()
            return
        }

        val userLocationRef =
            database.getReference("locations").child(userId).child("last_location")

        userLocationRef.removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Ubicación borrada exitosamente.", Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al borrar la ubicación.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchOccupiedSpots() {
        locationsRef = database.getReference("locations")
        locationsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                val spots = mutableListOf<String>()
                mySpot = null
                val currentUserId = auth.currentUser?.uid

                for (userSnapshot in snapshot.children) {
                    val lastLocation = userSnapshot.child("last_location")
                    if (lastLocation.exists()) {
                        val spotId = lastLocation.child("spot").getValue(String::class.java)
                        spotId?.let {
                            spots.add(it)
                            if (userSnapshot.key == currentUserId) {
                                mySpot = it
                            }
                        }
                    }
                }
                occupiedSpots = spots
                updateParkingGrid()
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded) {
                    Toast.makeText(context, "Error al cargar el plano.", Toast.LENGTH_SHORT).show()
                }
            }
        }
        locationsRef.addValueEventListener(locationsListener)
    }

    private fun updateParkingGrid() {
        if (!isAdded) return

        gridLayoutPlano.removeAllViews()
        val inflater = LayoutInflater.from(context)
        val spots = when (selectedFloor) {
            1 -> (1..10).map { "A$it" }
            2 -> (1..10).map { "B$it" }
            3 -> (1..10).map { "C$it" }
            else -> emptyList()
        }

        for (spot in spots) {
            val spotView = inflater.inflate(R.layout.parking_spot_item, gridLayoutPlano, false)

            val params = GridLayout.LayoutParams().apply {
                height = GridLayout.LayoutParams.WRAP_CONTENT
                width = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            spotView.layoutParams = params

            val spotIdTextView = spotView.findViewById<TextView>(R.id.spot_id_text)
            val statusTextView = spotView.findViewById<TextView>(R.id.price_text)

            spotIdTextView.text = spot

            if (spot == mySpot) {
                spotView.setBackgroundResource(R.drawable.borde_discontinuo_ocupado)
                statusTextView.text = "Mi Auto"
                statusTextView.background = ContextCompat.getDrawable(requireContext(), R.drawable.button_background_blue)
            } else if (occupiedSpots.contains(spot)) {
                spotView.setBackgroundResource(R.drawable.borde_discontinuo_ocupado)
                statusTextView.text = "Ocupado"
                statusTextView.background = ContextCompat.getDrawable(requireContext(), R.drawable.button_background_gray)
            } else {
                spotView.setBackgroundResource(R.drawable.borde_discontinuo)
                statusTextView.text = "Libre"
                statusTextView.background = ContextCompat.getDrawable(requireContext(), R.drawable.button_background_blue)
            }
            gridLayoutPlano.addView(spotView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::locationsRef.isInitialized) {
            locationsRef.removeEventListener(locationsListener)
        }
    }
}