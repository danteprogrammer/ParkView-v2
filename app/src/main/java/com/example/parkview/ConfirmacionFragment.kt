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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class ConfirmacionFragment : Fragment() {

    private val args: ConfirmacionFragmentArgs by navArgs()
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private var currentMinutes: Int = 0
    private var currentPrice: Int = 0

    private var clientName: String = "Usuario"
    private lateinit var tvNombreUsuario: TextView

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

        val tvEspacio: TextView = view.findViewById(R.id.tv_espacio_confirmacion)
        tvNombreUsuario = view.findViewById(R.id.tv_nombre_usuario)
        val btnPagar: Button = view.findViewById(R.id.btn_pagar_ahora)
        val tvMinutos: TextView = view.findViewById(R.id.tv_minutos_seleccionados)
        val sliderTiempo: Slider = view.findViewById(R.id.slider_tiempo_maximo)
        val tvPrecio: TextView = view.findViewById(R.id.tv_precio_confirmacion)

        currentMinutes = args.time
        currentPrice = args.price


        tvEspacio.text = "Espacio: ${args.spot}"
        tvMinutos.text = "$currentMinutes minutos"
        sliderTiempo.value = currentMinutes.toFloat()
        tvPrecio.text = "Precio: S/ $currentPrice.00"

        loadUserName()

        sliderTiempo.addOnChangeListener { _, value, _ ->
            currentMinutes = value.toInt()
            currentPrice = currentMinutes / 10

            tvMinutos.text = "$currentMinutes minutos"
            tvPrecio.text = "Precio: S/ $currentPrice.00"
        }

        btnPagar.setOnClickListener {
            saveLocationToFirebase(clientName)
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
            "maxStayMinutes" to currentMinutes,
            "price" to currentPrice,
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

    private fun loadUserName() {
        val userId = auth.currentUser?.uid ?: return
        database.getReference("users").child(userId).child("name")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded) return
                    val name = snapshot.getValue(String::class.java)
                    clientName = name ?: "Usuario"
                    tvNombreUsuario.text = "Cliente: $clientName"
                }

                override fun onCancelled(error: DatabaseError) {
                    if (!isAdded) return
                    clientName = "Usuario"
                    tvNombreUsuario.text = "Cliente: Usuario"
                }
            })
    }
}