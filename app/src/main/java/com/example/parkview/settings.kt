package com.example.parkview

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.database.FirebaseDatabase
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class settings : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val btnEditProfile = view.findViewById<AppCompatButton>(R.id.btn_edit_profile)
        val btnDeleteAccount = view.findViewById<AppCompatButton>(R.id.btn_delete_account)
        val btnLogout = view.findViewById<AppCompatButton>(R.id.btn_logout)
        val btnBack = view.findViewById<AppCompatButton>(R.id.btn_back_from_settings)

        btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_editProfileFragment)
        }

        btnDeleteAccount.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(context, "Sesión cerrada.", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_settings_to_bienvenida)
        }

        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar Cuenta")
            .setMessage("¿Estás seguro? Esta acción borrará tu nombre, ubicación guardada y acceso a la app de forma permanente.")
            .setPositiveButton("Eliminar") { dialog, which ->
                deleteUserAccountAndData()
            }
            .setNegativeButton("Cancelar", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deleteUserAccountAndData() {
        val user = auth.currentUser
        val userId = user?.uid

        if (user == null || userId == null) {
            Toast.makeText(context, "Error: No se pudo encontrar el usuario.", Toast.LENGTH_SHORT).show()
            return
        }


        database.getReference("locations").child(userId).removeValue().addOnCompleteListener { locationTask ->
            if (!locationTask.isSuccessful) {
                Toast.makeText(context, "Aviso: No se pudo borrar la ubicación guardada.", Toast.LENGTH_SHORT).show()
            }


            database.getReference("users").child(userId).removeValue().addOnCompleteListener { nameTask ->
                if (!nameTask.isSuccessful) {
                    Toast.makeText(context, "Aviso: No se pudo borrar el nombre de usuario.", Toast.LENGTH_SHORT).show()
                }

                user.delete().addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        Toast.makeText(context, "Cuenta eliminada con éxito.", Toast.LENGTH_LONG).show()
                        findNavController().navigate(R.id.action_settings_to_bienvenida) // Ir a bienvenida
                    } else {
                        if (authTask.exception is FirebaseAuthRecentLoginRequiredException) {
                            Toast.makeText(context, "Por seguridad, cierra sesión y vuelve a iniciarla antes de eliminar tu cuenta.", Toast.LENGTH_LONG).show()
                            auth.signOut()
                            findNavController().navigate(R.id.action_settings_to_bienvenida)
                        } else {
                            Toast.makeText(context, "Error al eliminar cuenta: ${authTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }
}