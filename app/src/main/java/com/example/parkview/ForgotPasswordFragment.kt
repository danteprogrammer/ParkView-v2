package com.example.parkview

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val etEmail = view.findViewById<EditText>(R.id.et_email_forgot)
        val btnSend = view.findViewById<Button>(R.id.btn_send_recovery)
        val tvBackToLogin = view.findViewById<TextView>(R.id.tv_back_to_login)

        btnSend.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                sendPasswordResetEmail(email)
            } else {
                Toast.makeText(context, "Por favor, ingresa un correo válido.", Toast.LENGTH_SHORT).show()
                etEmail.error = "Correo inválido"
            }
        }

        tvBackToLogin.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Correo de recuperación enviado.", Toast.LENGTH_LONG).show()
                    findNavController().popBackStack() // Regresa a Login después de enviar
                } else {
                    Toast.makeText(context, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}