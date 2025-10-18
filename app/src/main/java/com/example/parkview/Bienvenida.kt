package com.example.parkview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth

class Bienvenida : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_bienvenida, container, false)
    }

    override fun onStart() {
        super.onStart()

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            findNavController().navigate(R.id.action_bienvenida_to_dashboard)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnIniciarSesion = view.findViewById<Button>(R.id.btn_iniciar_sesion)

        btnIniciarSesion.setOnClickListener {
            findNavController().navigate(R.id.action_bienvenida_to_login)
        }

        val tvRegistrate = view.findViewById<TextView>(R.id.tv_registrate)

        tvRegistrate.setOnClickListener {
            findNavController().navigate(R.id.action_bienvenida_to_register)
        }
    }
}