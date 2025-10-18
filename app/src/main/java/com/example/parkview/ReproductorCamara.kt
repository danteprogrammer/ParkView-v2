package com.example.parkview

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.activity.OnBackPressedCallback // <-- IMPORTACIÓN NECESARIA

class ReproductorCamara : Fragment() {

    // Variables y constantes para la cámara
    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var previewView: PreviewView

    // Estado del reproductor
    private var isMuted = false
    private var isFullscreen = false
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    // Caso de uso para simular audio/captura de datos (necesario para el Mute)
    private var imageCapture: ImageCapture? = null

    // Variable para guardar la referencia del caso de uso Preview (necesario para Pause)
    private var previewUseCase: Preview? = null

    // ----------------------------------------------------------------
    // CICLO DE VIDA
    // ----------------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        cameraExecutor = Executors.newSingleThreadExecutor()
        return inflater.inflate(R.layout.fragment_reproductor_camara, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Manejo del Botón Atrás
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFullscreen) {
                    // Si estamos en Fullscreen, salimos del modo Fullscreen
                    view.let { toggleFullscreen(it) }
                } else {
                    // Si NO estamos en Fullscreen, volvemos a la pantalla anterior
                    findNavController().popBackStack()
                }
            }
        }
        // Usamos viewLifecycleOwner para enlazar el callback
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        previewView = view.findViewById(R.id.cameraPreview)

        // Recibimos los datos de la cámara seleccionada
        val cameraName = arguments?.getString("cameraName")
        val tvCameraName = view.findViewById<TextView>(R.id.tv_reproductor_title)
        tvCameraName.text = cameraName ?: "Cámara - Pasillo"

        // Manejo de Permisos: Comprueba y solicita al inicio
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // --- Lógica de botones ---

        val btnPlay = view.findViewById<AppCompatButton>(R.id.btn_play)
        btnPlay.setOnClickListener {
            // Reanudar el Preview y el Audio
            previewUseCase?.let {
                cameraProvider?.bindToLifecycle(viewLifecycleOwner, cameraSelector, it)
            }
            imageCapture?.let {
                cameraProvider?.bindToLifecycle(viewLifecycleOwner, cameraSelector, it)
            }
            Toast.makeText(context, "Reproducción reanudada", Toast.LENGTH_SHORT).show()
        }

        val btnPause = view.findViewById<AppCompatButton>(R.id.btn_pause)
        btnPause.setOnClickListener {
            // Desenlazar la referencia guardada (previewUseCase) para congelar el feed
            previewUseCase?.let {
                cameraProvider?.unbind(it)
            }
            Toast.makeText(context, "Video pausado", Toast.LENGTH_SHORT).show()
        }

        val btnMute = view.findViewById<AppCompatButton>(R.id.btn_mute)
        btnMute.setOnClickListener {
            toggleAudio()
        }

        val btnFullscreen = view.findViewById<AppCompatButton>(R.id.btn_fullscreen)
        btnFullscreen.setOnClickListener {
            toggleFullscreen(view)
        }

        val btnCambiarCamara = view.findViewById<AppCompatButton>(R.id.btn_cambiar_camara)
        btnCambiarCamara.setOnClickListener {
            // Cambiar Cámara: Alterna y reinicia el feed
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }

        val settingsIcon = view.findViewById<ImageView>(R.id.settings_icon)
        settingsIcon.setOnClickListener {
            findNavController().navigate(R.id.action_reproductorCamara_to_settings)
        }
    }

    // ----------------------------------------------------------------
    // LÓGICA DE CÁMARA
    // ----------------------------------------------------------------

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Configura la vista previa
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Guarda la referencia del Preview en la variable de clase (para Pause)
            previewUseCase = preview

            // Inicializa ImageCapture (simula el canal de datos/audio)
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider?.unbindAll()

                // Enlaza la vista previa y el caso de uso de ImageCapture
                cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture
                )

                // Si el estado es Mute, desactiva el audio inmediatamente después de enlazar
                if (isMuted) toggleAudio(true)

            } catch(exc: Exception) {
                Toast.makeText(requireContext(), "Error al iniciar la cámara: ${exc.message}", Toast.LENGTH_LONG).show()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // ----------------------------------------------------------------
    // LÓGICA DE BOTONES AUXILIARES
    // ----------------------------------------------------------------

    /**
     * Alterna la captura de audio/datos (simulando Mute).
     */
    private fun toggleAudio(forceMute: Boolean = false) {
        val newState = if (forceMute) true else !isMuted

        if (newState) {
            // MUTE: desenlaza ImageCapture (simula detener el canal de audio/datos)
            cameraProvider?.unbind(imageCapture!!)
            isMuted = true
            Toast.makeText(context, "Audio silenciado", Toast.LENGTH_SHORT).show()
        } else {
            // UNMUTE: vuelve a enlazar ImageCapture para restaurar el canal
            cameraProvider?.bindToLifecycle(viewLifecycleOwner, cameraSelector, imageCapture)
            isMuted = false
            Toast.makeText(context, "Audio activado", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Alterna la vista de pantalla completa (soporte para API antigua y moderna).
     * 💡 CORREGIDO: Se eliminó la manipulación incorrecta del headerLayout al salir de Fullscreen.
     */
    private fun toggleFullscreen(view: View) {
        val headerLayout = view.findViewById<View>(R.id.header_layout_reproductor)
        val reproductorCard = view.findViewById<View>(R.id.reproductor_card)
        val tvTitle = view.findViewById<TextView>(R.id.tv_reproductor_title)
        val settingsIcon = view.findViewById<ImageView>(R.id.settings_icon)
        val window = activity?.window

        if (window == null) return // Evitar crashes

        if (!isFullscreen) {
            // ENTRAR EN FULLSCREEN INMERSIVO

            // 1. Ocultar barras del sistema:
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // API 30+ (Moderno): Ocultar StatusBar y NavigationBar
                window.insetsController?.hide(WindowInsets.Type.systemBars())
            } else {
                // API 29- (Clásico): Ocultar StatusBar y NavigationBar
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            }

            // 2. Ocultar Controles de la App
            headerLayout.visibility = View.GONE
            tvTitle.visibility = View.GONE
            settingsIcon.visibility = View.GONE

            // 3. Expandir la tarjeta de reproducción
            val cardParams = reproductorCard.layoutParams as ViewGroup.MarginLayoutParams
            cardParams.topMargin = 0 // Elimina el margen negativo
            cardParams.bottomMargin = 0 // Elimina el margen inferior
            reproductorCard.layoutParams = cardParams

            Toast.makeText(context, "Pantalla completa activada", Toast.LENGTH_SHORT).show()

        } else {
            // SALIR DE FULLSCREEN (LÓGICA CORREGIDA)

            // 1. Mostrar barras del sistema:
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // API 30+ (Moderno): Mostrar StatusBar y NavigationBar
                window.insetsController?.show(WindowInsets.Type.systemBars())
            } else {
                // API 29- (Clásico): Mostrar StatusBar y NavigationBar
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }

            // 2. Mostrar Controles de la App
            headerLayout.visibility = View.VISIBLE
            tvTitle.visibility = View.VISIBLE
            settingsIcon.visibility = View.VISIBLE

            // 3. Restaurar diseño original (SOLO MÁRGENES DE LA TARJETA)
            // *** Bloque que causaba el error ELIMINADO ***

            val cardParams = reproductorCard.layoutParams as ViewGroup.MarginLayoutParams
            cardParams.topMargin = resources.getDimensionPixelSize(R.dimen.negative_margin_80dp)
            cardParams.bottomMargin = resources.getDimensionPixelSize(R.dimen.margin_32dp)
            reproductorCard.layoutParams = cardParams

            Toast.makeText(context, "Pantalla normal", Toast.LENGTH_SHORT).show()
        }

        isFullscreen = !isFullscreen
        reproductorCard.requestLayout()
    }

    // ----------------------------------------------------------------
    // LÓGICA DE PERMISOS Y CICLO DE VIDA (Mantiene el modo inmersivo al regresar a la App)
    // ----------------------------------------------------------------

    override fun onResume() {
        super.onResume()
        if (isFullscreen) {
            val window = activity?.window
            if (window != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.insetsController?.hide(WindowInsets.Type.systemBars())
                } else {
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Cuando el Fragmento se pausa, restauramos el comportamiento normal del sistema UI si estamos en Fullscreen.
        if (isFullscreen) {
            val window = activity?.window
            if (window != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.insetsController?.show(WindowInsets.Type.systemBars())
                } else {
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Permiso de cámara denegado. La vista de seguridad no funcionará.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
    }
}