package com.example.parkview

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.CountDownTimer
import androidx.core.content.ContextCompat
import com.google.firebase.database.DatabaseReference

class dashboard : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var welcomeTextView: TextView
    private lateinit var lastLocationTextView: TextView

    private lateinit var tvCountdownTimer: TextView
    private lateinit var tvLimitTime: TextView
    private lateinit var tvLimitTimeLabel: TextView
    private var countdownTimer: CountDownTimer? = null

    // Variables para manejar el ciclo de vida del oyente de Firebase
    private lateinit var dbRef: DatabaseReference
    private lateinit var lastLocationListener: ValueEventListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onStart() {
        super.onStart()
        loadUserName()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancelamos el temporizador y el oyente para evitar fugas de memoria y crashes
        countdownTimer?.cancel()
        if (::dbRef.isInitialized) {
            dbRef.removeEventListener(lastLocationListener)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        welcomeTextView = view.findViewById(R.id.welcome_text)
        lastLocationTextView = view.findViewById(R.id.last_location_text)

        tvCountdownTimer = view.findViewById(R.id.tv_countdown_timer)
        tvLimitTime = view.findViewById(R.id.tv_limit_time)
        tvLimitTimeLabel = view.findViewById(R.id.tv_limit_time_label)

        // Adjuntamos el oyente de la última ubicación
        attachLastLocationListener()

        val btnSaveLocation = view.findViewById<AppCompatButton>(R.id.btn_save_location)
        val btnSeeCar = view.findViewById<AppCompatButton>(R.id.btn_see_car)
        val btnSeeCameras = view.findViewById<AppCompatButton>(R.id.btn_see_cameras)
        val settingsIcon = view.findViewById<ImageView>(R.id.settings_icon)

        val buttons = listOf(btnSaveLocation, btnSeeCar, btnSeeCameras)
        btnSaveLocation.isSelected = true

        buttons.forEach { button ->
            button.setOnClickListener {
                buttons.forEach { it.isSelected = false }
                it.isSelected = true

                when (it.id) {
                    R.id.btn_save_location -> findNavController().navigate(R.id.action_dashboard_to_save_location)
                    R.id.btn_see_car -> findNavController().navigate(R.id.action_dashboard_to_see_car)
                    R.id.btn_see_cameras -> findNavController().navigate(R.id.action_dashboard_to_camaras)
                }
            }
        }

        settingsIcon.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_settings)
        }
    }

    private fun attachLastLocationListener() {
        val userId = auth.currentUser?.uid ?: return
        dbRef = database.getReference("locations").child(userId).child("last_location")

        lastLocationListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return // Comprobación de seguridad extra

                countdownTimer?.cancel()

                if (snapshot.exists()) {
                    val description = snapshot.child("description").getValue(String::class.java)
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java)
                    val maxStayMinutes = snapshot.child("maxStayMinutes").getValue(Int::class.java)

                    if (description == null || timestamp == null || maxStayMinutes == null) {
                        lastLocationTextView.text = "Datos de ubicación incompletos"
                        tvCountdownTimer.visibility = View.GONE
                        tvLimitTime.visibility = View.GONE
                        tvLimitTimeLabel.visibility = View.GONE
                        return
                    }

                    val cleanDescription = description.replace("Plano ", "").trim()
                    val maxStayMillis = TimeUnit.MINUTES.toMillis(maxStayMinutes.toLong())
                    val elapsedTime = System.currentTimeMillis() - timestamp
                    val timeLeftMillis = maxStayMillis - elapsedTime
                    val horaExactaRegistro = formatTimestamp(timestamp, "h:mm a")

                    lastLocationTextView.text = "Última ubicación: $cleanDescription\nHora de registro: $horaExactaRegistro"

                    if (timeLeftMillis > 1000) {
                        startCountdown(timeLeftMillis, maxStayMinutes, timestamp)
                    } else {
                        tvCountdownTimer.text = "¡Tiempo Expirado!"
                        tvCountdownTimer.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                        tvCountdownTimer.visibility = View.VISIBLE
                        tvLimitTime.visibility = View.GONE
                        tvLimitTimeLabel.text = "¡La ubicación ha expirado!"
                        tvLimitTimeLabel.visibility = View.VISIBLE
                    }
                } else {
                    lastLocationTextView.text = "No hay ubicación guardada"
                    tvCountdownTimer.visibility = View.GONE
                    tvLimitTime.visibility = View.GONE
                    tvLimitTimeLabel.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return
                lastLocationTextView.text = "Error al cargar ubicación"
                tvCountdownTimer.visibility = View.GONE
                tvLimitTime.visibility = View.GONE
                tvLimitTimeLabel.visibility = View.GONE
            }
        }
        dbRef.addValueEventListener(lastLocationListener)
    }

    private fun formatTimestamp(timestamp: Long, format: String): String {
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun startCountdown(millisRemaining: Long, maxStayMinutes: Int, timestamp: Long) {
        countdownTimer?.cancel()

        val expirationTimeMillis = timestamp + TimeUnit.MINUTES.toMillis(maxStayMinutes.toLong())
        val horaLimiteStr = formatTimestamp(expirationTimeMillis, "h:mm a")

        tvLimitTime.text = "Vencimiento: $horaLimiteStr"
        tvLimitTime.visibility = View.VISIBLE
        tvCountdownTimer.visibility = View.VISIBLE
        tvLimitTimeLabel.visibility = View.VISIBLE
        tvLimitTimeLabel.text = "Tiempo restante:"

        countdownTimer = object : CountDownTimer(millisRemaining, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (!isAdded) return
                updateCountdownDisplay(millisUntilFinished)
                if (millisUntilFinished < TimeUnit.MINUTES.toMillis(5)) {
                    tvCountdownTimer.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                } else {
                    tvCountdownTimer.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                }
            }

            override fun onFinish() {
                if (!isAdded) return
                tvCountdownTimer.text = "¡Tiempo Expirado!"
                tvCountdownTimer.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
                tvLimitTime.visibility = View.GONE
                tvLimitTimeLabel.text = "¡La ubicación ha expirado!"
                dbRef.removeValue()
            }
        }.start()

        tvCountdownTimer.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
    }

    private fun updateCountdownDisplay(millisUntilFinished: Long) {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
        tvCountdownTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun loadUserName() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            database.getReference("users").child(userId).child("name")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!isAdded) return
                        val name = snapshot.getValue(String::class.java)
                        welcomeTextView.text = if (name != null) "Bienvenido\n$name" else "Bienvenido\nUsuario"
                    }

                    override fun onCancelled(error: DatabaseError) {
                        if (!isAdded) return
                        welcomeTextView.text = "Bienvenido\nUsuario"
                    }
                })
        } else {
            if (isAdded) {
                findNavController().navigate(R.id.bienvenida)
            }
        }
    }
}