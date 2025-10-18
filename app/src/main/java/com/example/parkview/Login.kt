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

class Login : Fragment() {

    private lateinit var auth: FirebaseAuth

    private lateinit var etEmailLogin: EditText
    private lateinit var etPasswordLogin: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnGoToRegister: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        etEmailLogin = view.findViewById(R.id.et_email_login)
        etPasswordLogin = view.findViewById(R.id.et_password_login)
        btnLogin = view.findViewById(R.id.btn_login)
        btnGoToRegister = view.findViewById(R.id.btn_register_card)

        val tvForgotPassword = view.findViewById<TextView>(R.id.tv_forgot_password)

        btnLogin.setOnClickListener {
            val email = etEmailLogin.text.toString().trim()
            val password = etPasswordLogin.text.toString().trim()

            if (validateInput(email, password)) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(context, "Inicio de sesión exitoso.", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.action_login_to_dashboard)
                        } else {
                            Toast.makeText(context, "Error en el inicio de sesión: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        btnGoToRegister.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }

        tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgotPasswordFragment)
        }
    }

    private fun validateInput(email: String, pass: String): Boolean {
        if (email.isEmpty()) {
            Toast.makeText(context, "Por favor, ingresa tu correo electrónico.", Toast.LENGTH_SHORT).show()
            etEmailLogin.error = "Campo requerido"
            return false
        }
        if (pass.isEmpty()) {
            Toast.makeText(context, "Por favor, ingresa tu contraseña.", Toast.LENGTH_SHORT).show()
            etPasswordLogin.error = "Campo requerido"
            return false
        }
        return true
    }
}