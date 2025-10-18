package com.example.parkview

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth

class settings : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val btnLogout = view.findViewById<AppCompatButton>(R.id.btn_logout)
        val btnBack = view.findViewById<AppCompatButton>(R.id.btn_back_from_settings)

        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(context, "Sesión cerrada.", Toast.LENGTH_SHORT).show()

            findNavController().navigate(R.id.action_settings_to_bienvenida)
        }

        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}