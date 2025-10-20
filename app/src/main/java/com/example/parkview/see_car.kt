package com.example.parkview

import android.content.Intent // <-- IMPORTACIÓN AÑADIDA
import android.net.Uri // <-- IMPORTACIÓN AÑADIDA
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class see_car : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var lastLocationData: HashMap<String, Any>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_see_car, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // --- Referencias a los botones ---
        val btnVerPlano = view.findViewById<AppCompatButton>(R.id.btn_ver_plano)
        // El ImageView fue reemplazado por un Button:
        val btnNavegarMaps = view.findViewById<AppCompatButton>(R.id.btn_navegar_maps)
        val btnRegresar = view.findViewById<AppCompatButton>(R.id.btn_regresar_see_car)

        loadCarLocation(view) // Carga los datos de la ubicación

        // --- Lógica de OnClick para CADA botón ---

        btnVerPlano.setOnClickListener {
            if (lastLocationData != null) {
                // ### LÓGICA CORREGIDA ###
                // Preparamos los datos para enviar a PlanoInterno de forma segura
                val bundle = Bundle()
                val data = lastLocationData!!

                // Verificamos si es el NUEVO sistema (con piso y lugar)
                if (data.containsKey("floor") && data.containsKey("spot")) {
                    bundle.putInt("floor", (data["floor"] as Long).toInt())
                    bundle.putString("spot", data["spot"] as String)
                }
                // Si no, verificamos si es el sistema ANTIGUO (con coordenadas)
                else if (data.containsKey("normalizedX") && data.containsKey("normalizedY")) {
                    bundle.putFloat("normalizedX", (data["normalizedX"] as Double).toFloat())
                    bundle.putFloat("normalizedY", (data["normalizedY"] as Double).toFloat())
                }

                if (bundle.isEmpty) {
                    Toast.makeText(context, "Los datos de la ubicación están corruptos.", Toast.LENGTH_SHORT).show()
                } else {
                    findNavController().navigate(R.id.action_see_car_to_planoInterno, bundle)
                }
                // ### FIN DE LA LÓGICA CORREGIDA ###

            } else {
                Toast.makeText(context, "No hay ubicación guardada para mostrar.", Toast.LENGTH_SHORT).show()
            }
        }

        // --- ¡AQUÍ ESTÁ LA NUEVA LÓGICA! ---
        btnNavegarMaps.setOnClickListener {
            // Coordenadas de ejemplo: Estacionamiento Los Portales, Parque Kennedy, Miraflores
            val latitud = "-12.121852"
            val longitud = "-77.030367"
            val etiqueta = "Estacionamiento Los Portales (Parque Kennedy)"

            // Creamos el URI para el Intent de Geo-localización
            // El 'q=' define el marcador y la etiqueta
            val gmmIntentUri = Uri.parse("geo:$latitud,$longitud?q=$latitud,$longitud($etiqueta)")

            // Creamos el Intent para ver el mapa
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)

            // Verificamos si hay alguna app que pueda manejar este Intent (como Google Maps)
            if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(mapIntent)
            } else {
                // Mensaje de error por si el usuario no tiene ninguna app de mapas
                Toast.makeText(context, "No se encontró una aplicación de mapas.", Toast.LENGTH_SHORT).show()
            }
        }

        btnRegresar.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun loadCarLocation(view: View) {
        val userId = auth.currentUser?.uid ?: return
        val dbRef = database.getReference("locations").child(userId).child("last_location")
        val tvPiso = view.findViewById<TextView>(R.id.tv_piso)
        val tvLugar = view.findViewById<TextView>(R.id.tv_lugar)

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    lastLocationData = snapshot.value as? HashMap<String, Any>
                    val description = lastLocationData?.get("description") as? String

                    // Mostramos la descripción si existe, si no, los datos individuales
                    if (!description.isNullOrEmpty()) {
                        tvPiso.text = description // "Piso 1, Espacio A2"
                        tvLugar.visibility = View.GONE // Ocultamos el segundo TextView si no es necesario
                    } else {
                        val floor = lastLocationData?.get("floor")
                        val spot = lastLocationData?.get("spot")
                        tvPiso.text = "Piso: ${floor ?: "N/A"}"
                        tvLugar.text = "Lugar: ${spot ?: "N/A"}"
                        tvLugar.visibility = View.VISIBLE
                    }
                } else {
                    tvPiso.text = "No hay ubicación guardada"
                    tvLugar.text = ""
                    lastLocationData = null
                }
            }

            override fun onCancelled(error: DatabaseError) {
                tvPiso.text = "Error"
                tvLugar.text = "Error al cargar"
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                lastLocationData = null
            }
        })
    }
}