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
import com.google.firebase.database.FirebaseDatabase

class Register : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var etNameRegister: EditText
    private lateinit var etEmailRegister: EditText
    private lateinit var etPasswordRegister: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        etNameRegister = view.findViewById(R.id.et_name_register)
        etEmailRegister = view.findViewById(R.id.et_email_register)
        etPasswordRegister = view.findViewById(R.id.et_password_register)
        etConfirmPassword = view.findViewById(R.id.et_confirm_password)
        btnRegister = view.findViewById(R.id.btn_register)


        val tvHaveAccount = view.findViewById<TextView>(R.id.tv_have_account)
        tvHaveAccount.setOnClickListener {
            // Navega a la pantalla de Login
            findNavController().navigate(R.id.action_register_to_login)
        }

        btnRegister.setOnClickListener {
            val name = etNameRegister.text.toString().trim()
            val email = etEmailRegister.text.toString().trim()
            val password = etPasswordRegister.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (validateInput(name, email, password, confirmPassword)) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val userId = user?.uid

                            if (userId != null) {
                                val userRef = database.getReference("users").child(userId)
                                val userData = HashMap<String, String>()
                                userData["name"] = name
                                userRef.setValue(userData)
                                    .addOnCompleteListener { dbTask ->
                                        if (dbTask.isSuccessful) {
                                            Toast.makeText(context, "Registro exitoso.", Toast.LENGTH_SHORT).show()
                                            findNavController().navigate(R.id.action_register_to_login)
                                        } else {
                                            Toast.makeText(context, "Error al guardar nombre: ${dbTask.exception?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(context, "Error: UID de usuario nulo.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Fallo el registro: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }
    }

    private fun validateInput(name: String, email: String, pass: String, confirmPass: String): Boolean {
        if (name.isEmpty()) {
            Toast.makeText(context, "Por favor, ingresa tu nombre completo.", Toast.LENGTH_SHORT).show()
            etNameRegister.error = "Campo requerido"
            return false
        }
        if (email.isEmpty()) {
            Toast.makeText(context, "Por favor, ingresa tu correo electrónico.", Toast.LENGTH_SHORT).show()
            etEmailRegister.error = "Campo requerido"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(context, "Por favor, ingresa un correo electrónico válido.", Toast.LENGTH_SHORT).show()
            etEmailRegister.error = "Correo inválido"
            return false
        }
        if (pass.isEmpty()) {
            Toast.makeText(context, "Por favor, ingresa tu contraseña.", Toast.LENGTH_SHORT).show()
            etPasswordRegister.error = "Campo requerido"
            return false
        }
        if (pass.length < 6) {
            Toast.makeText(context, "La contraseña debe tener al menos 6 caracteres.", Toast.LENGTH_SHORT).show()
            etPasswordRegister.error = "Mínimo 6 caracteres"
            return false
        }
        if (confirmPass.isEmpty()) {
            Toast.makeText(context, "Por favor, confirma tu contraseña.", Toast.LENGTH_SHORT).show()
            etConfirmPassword.error = "Campo requerido"
            return false
        }
        if (pass != confirmPass) {
            Toast.makeText(context, "Las contraseñas no coinciden.", Toast.LENGTH_SHORT).show()
            etConfirmPassword.error = "Las contraseñas no coinciden"
            return false
        }
        return true
    }
}