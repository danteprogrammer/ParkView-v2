package com.example.parkview

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EditProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var etNameEdit: EditText
    private lateinit var btnSaveName: Button
    private lateinit var btnCancelEdit: Button
    private var currentUserId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        currentUserId = auth.currentUser?.uid

        etNameEdit = view.findViewById(R.id.et_name_edit)
        btnSaveName = view.findViewById(R.id.btn_save_name)
        btnCancelEdit = view.findViewById(R.id.btn_cancel_edit)

        loadCurrentName()

        btnSaveName.setOnClickListener {
            val newName = etNameEdit.text.toString().trim()
            if (newName.isNotEmpty()) {
                updateUserName(newName)
            } else {
                etNameEdit.error = "El nombre no puede estar vacío"
                Toast.makeText(context, "Por favor, ingresa un nombre.", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancelEdit.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun loadCurrentName() {
        if (currentUserId != null) {
            database.getReference("users").child(currentUserId!!).child("name")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val name = snapshot.getValue(String::class.java)
                        etNameEdit.setText(name ?: "")
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "Error al cargar nombre actual: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun updateUserName(newName: String) {
        if (currentUserId != null) {
            database.getReference("users").child(currentUserId!!).child("name")
                .setValue(newName)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "Nombre actualizado con éxito.", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    } else {
                        Toast.makeText(context, "Error al actualizar nombre: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        } else {
            Toast.makeText(context, "Error: Usuario no encontrado.", Toast.LENGTH_SHORT).show()
        }
    }
}