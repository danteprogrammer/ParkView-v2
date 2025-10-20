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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class see_car : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private var lastLocationData: HashMap<String, Any>? = null

    private lateinit var locationRef: DatabaseReference
    private lateinit var locationListener: ValueEventListener

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


        val btnVerPlano = view.findViewById<AppCompatButton>(R.id.btn_ver_plano)

        val btnNavegarMaps = view.findViewById<AppCompatButton>(R.id.btn_navegar_maps)
        val btnRegresar = view.findViewById<AppCompatButton>(R.id.btn_regresar_see_car)

        loadCarLocation(view)



        btnVerPlano.setOnClickListener {
            if (lastLocationData != null) {

                val bundle = Bundle()
                val data = lastLocationData!!


                if (data.containsKey("floor") && data.containsKey("spot")) {
                    bundle.putInt("floor", (data["floor"] as Long).toInt())
                    bundle.putString("spot", data["spot"] as String)
                }

                else if (data.containsKey("normalizedX") && data.containsKey("normalizedY")) {
                    bundle.putFloat("normalizedX", (data["normalizedX"] as Double).toFloat())
                    bundle.putFloat("normalizedY", (data["normalizedY"] as Double).toFloat())
                }

                if (bundle.isEmpty) {
                    Toast.makeText(
                        context,
                        "Los datos de la ubicación están corruptos.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    findNavController().navigate(R.id.action_see_car_to_planoInterno, bundle)
                }


            } else {
                Toast.makeText(
                    context,
                    "No hay ubicación guardada para mostrar.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnNavegarMaps.setOnClickListener {

            val latitud = "-12.121852"
            val longitud = "-77.030367"
            val etiqueta = "Estacionamiento Los Portales (Parque Kennedy)"


            val gmmIntentUri = Uri.parse("geo:$latitud,$longitud?q=$latitud,$longitud($etiqueta)")


            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)


            if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(mapIntent)
            } else {

                Toast.makeText(
                    context,
                    "No se encontró una aplicación de mapas.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val settingsIcon = view.findViewById<ImageView>(R.id.settings_icon)
        settingsIcon.setOnClickListener {
            findNavController().navigate(R.id.action_see_car_to_settings)
        }

        btnRegresar.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun loadCarLocation(view: View) {
        val userId = auth.currentUser?.uid ?: return
        locationRef = database.getReference("locations").child(userId).child("last_location")
        val tvPiso = view.findViewById<TextView>(R.id.tv_piso)
        val tvLugar = view.findViewById<TextView>(R.id.tv_lugar)

        locationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (!isAdded) return

                if (snapshot.exists()) {
                    lastLocationData = snapshot.value as? HashMap<String, Any>
                    val description = lastLocationData?.get("description") as? String

                    if (!description.isNullOrEmpty()) {
                        tvPiso.text = description
                        tvLugar.visibility = View.GONE
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
                    tvLugar.visibility = View.GONE
                    lastLocationData = null
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return

                tvPiso.text = "Error"
                tvLugar.text = "Error al cargar"
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                lastLocationData = null
            }
        }
        locationRef.addValueEventListener(locationListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (::locationRef.isInitialized && ::locationListener.isInitialized) {
            locationRef.removeEventListener(locationListener)
        }
    }

}