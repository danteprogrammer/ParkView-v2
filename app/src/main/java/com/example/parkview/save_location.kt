package com.example.parkview

import android.os.Bundle
import androidx.fragment.app.Fragment
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
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class save_location : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase


    private lateinit var locationsRef: DatabaseReference
    private lateinit var locationsListener: ValueEventListener
    private var occupiedSpots = listOf<String>()


    private var selectedFloor: Int = 1
    private lateinit var gridLayoutParking: GridLayout
    private lateinit var rgPisos: RadioGroup

    private var defaultMaxTimeMinutes: Int = 10

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_save_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        gridLayoutParking = view.findViewById(R.id.grid_layout_parking)
        rgPisos = view.findViewById(R.id.rg_pisos)


        attachOccupiedSpotsListener()

        rgPisos.setOnCheckedChangeListener { _, checkedId ->
            selectedFloor = when (checkedId) {
                R.id.rb_piso2 -> 2
                R.id.rb_piso3 -> 3
                else -> 1
            }

            updateParkingGrid()
        }

        val settingsIcon = view.findViewById<ImageView>(R.id.settings_icon)
        settingsIcon.setOnClickListener {
            findNavController().navigate(R.id.action_save_location_to_settings)
        }

        val btnCancel = view.findViewById<AppCompatButton>(R.id.btn_cancel)
        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun attachOccupiedSpotsListener() {
        locationsRef = database.getReference("locations")
        locationsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                val spots = mutableListOf<String>()
                val currentUserId = auth.currentUser?.uid

                var currentUserSpot: String? = null

                for (userSnapshot in snapshot.children) {
                    val lastLocation = userSnapshot.child("last_location")
                    if (lastLocation.exists()) {
                        val spotId = lastLocation.child("spot").getValue(String::class.java)
                        spotId?.let {
                            spots.add(it)
                            if (userSnapshot.key == currentUserId) {
                                currentUserSpot = it
                            }
                        }
                    }
                }
                occupiedSpots = spots
                updateParkingGrid()
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded) {
                    Toast.makeText(context, "Error al cargar disponibilidad.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
        locationsRef.addValueEventListener(locationsListener)
    }

    private fun updateParkingGrid() {
        if (!isAdded) return

        gridLayoutParking.removeAllViews()
        val inflater = LayoutInflater.from(context)

        val spots = when (selectedFloor) {
            1 -> (1..10).map { "A$it" }
            2 -> (1..10).map { "B$it" }
            3 -> (1..10).map { "C$it" }
            else -> emptyList()
        }

        for (spot in spots) {
            val spotView = inflater.inflate(R.layout.parking_spot_item, gridLayoutParking, false)

            val params = GridLayout.LayoutParams().apply {
                height = GridLayout.LayoutParams.WRAP_CONTENT
                width = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            spotView.layoutParams = params

            val spotIdTextView = spotView.findViewById<TextView>(R.id.spot_id_text)
            val priceTextView =
                spotView.findViewById<TextView>(R.id.price_text) // Reusado como botón/etiqueta de estado

            spotIdTextView.text = spot


            if (occupiedSpots.contains(spot)) {

                spotView.setBackgroundResource(R.drawable.borde_discontinuo_ocupado)
                priceTextView.text = "Ocupado"
                priceTextView.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.button_background_gray)
                spotView.isClickable = false
            } else {

                spotView.setBackgroundResource(R.drawable.borde_discontinuo)
                val initialPrice = defaultMaxTimeMinutes / 10
                priceTextView.text = "Desde S/ ${initialPrice}.00"
                priceTextView.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.button_background_blue)


                spotView.setOnClickListener {
                    val action = save_locationDirections.actionSaveLocationToConfirmacionFragment(
                        spot = spot,
                        floor = selectedFloor,
                        time = defaultMaxTimeMinutes,
                        price = initialPrice
                    )
                    findNavController().navigate(action)
                }
            }
            gridLayoutParking.addView(spotView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::locationsRef.isInitialized && ::locationsListener.isInitialized) {
            locationsRef.removeEventListener(locationsListener)
        }
    }
}