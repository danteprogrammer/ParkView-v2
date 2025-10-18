package com.example.parkview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.slider.Slider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ConfirmacionFragment : Fragment() {

    private val args: ConfirmacionFragmentArgs by navArgs()
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // Variables para guardar el tiempo y precio seleccionados
    private var currentMinutes: Int = 0
    private var currentPrice: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_confirmacion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Referencias a las vistas
        val tvEspacio: TextView = view.findViewById(R.id.tv_espacio_confirmacion)
        val etNombre: EditText = view.findViewById(R.id.et_nombre_completo)
        val btnPagar: Button = view.findViewById(R.id.btn_pagar_ahora)
        val tvMinutos: TextView = view.findViewById(R.id.tv_minutos_seleccionados)
        val sliderTiempo: Slider = view.findViewById(R.id.slider_tiempo_maximo)
        val tvPrecio: TextView = view.findViewById(R.id.tv_precio_confirmacion)

        // 1. Configurar el estado inicial desde los datos recibidos
        currentMinutes = args.time
        currentPrice = args.price

        tvEspacio.text = "Espacio: ${args.spot}"
        tvMinutos.text = "$currentMinutes minutos"
        sliderTiempo.value = currentMinutes.toFloat()
        tvPrecio.text = "Precio: S/ $currentPrice.00"

        // 2. Listener para cuando el usuario mueva el slider
        sliderTiempo.addOnChangeListener { _, value, _ ->
            currentMinutes = value.toInt()
            currentPrice = currentMinutes / 10 // Recalcula el precio

            tvMinutos.text = "$currentMinutes minutos"
            tvPrecio.text = "Precio: S/ $currentPrice.00"
        }

        // 3. Configurar el botón de pagar
        btnPagar.setOnClickListener {
            val nombre = etNombre.text.toString()
            if (nombre.isBlank()) {
                Toast.makeText(context, "Por favor, ingresa tu nombre.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveLocationToFirebase(nombre)
        }
    }

    private fun saveLocationToFirebase(nombreCliente: String) {
        val userId = auth.currentUser?.uid ?: return

        val timestamp = System.currentTimeMillis()
        val description = "Piso ${args.floor}, Espacio ${args.spot}"

        val locationData = hashMapOf(
            "description" to description,
            "floor" to args.floor,
            "spot" to args.spot,
            "timestamp" to timestamp,
            "maxStayMinutes" to currentMinutes, // Guarda el valor actualizado
            "price" to currentPrice,         // Guarda el valor actualizado
            "clientName" to nombreCliente
        )

        val dbRef = database.getReference("locations").child(userId).child("last_location")

        dbRef.setValue(locationData)
            .addOnSuccessListener {
                Toast.makeText(context, "Reserva confirmada.", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_confirmacionFragment_to_pagoExitosoFragment)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al guardar: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}