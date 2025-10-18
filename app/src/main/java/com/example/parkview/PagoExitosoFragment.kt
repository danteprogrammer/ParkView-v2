package com.example.parkview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class PagoExitosoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pago_exitoso, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnVolver: Button = view.findViewById(R.id.btn_volver_inicio)
        btnVolver.setOnClickListener {
            findNavController().navigate(R.id.action_pagoExitosoFragment_to_dashboard)
        }
    }
}