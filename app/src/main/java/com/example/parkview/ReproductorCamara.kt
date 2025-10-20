package com.example.parkview

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.activity.OnBackPressedCallback
import android.widget.LinearLayout
import androidx.navigation.fragment.navArgs

class ReproductorCamara : Fragment() {

    // Constantes para los nombres de las cámaras
    private val CAMARA_PASILLO = "Cámara - Pasillo"
    private val CAMARA_SUPERIOR = "Cámara - Superior"

    private val args: ReproductorCamaraArgs by navArgs()

    // Variables y constantes para la cámara
    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var previewView: PreviewView

    // VARIABLES DE CLASE
    private lateinit var tvReproductorTitle: TextView
    private lateinit var btnCambiarCamara: AppCompatButton
    private lateinit var controlsLayout: View
    private lateinit var reproductorCard: LinearLayout

    // Estado del reproductor
    private var isMuted = false
    // Usaremos un valor simple para rastrear el estado actual de la cámara
    private var isBackCameraSelected: Boolean = true
    private var currentCameraName: String = CAMARA_PASILLO
    private var imageCapture: ImageCapture? = null
    private var previewUseCase: Preview? = null

    private fun Int.dpToPx(context: Context): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

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

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)

        // ----------------------------------------------------------------
        // INICIALIZACIÓN DE VIEWS Y REFERENCIAS
        // ----------------------------------------------------------------
        previewView = view.findViewById(R.id.cameraPreview)
        tvReproductorTitle = view.findViewById(R.id.tv_reproductor_title)
        btnCambiarCamara = view.findViewById(R.id.btn_cambiar_camara)
        controlsLayout = view.findViewById(R.id.controls_layout)
        reproductorCard = view.findViewById(R.id.reproductor_card)

        // --- LÓGICA MODIFICADA ---
        // Lee los argumentos recibidos en lugar de usar valores fijos
        val streamUrl = args.streamUrl
        currentCameraName = args.cameraName
        // Decide qué cámara seleccionar basado en el argumento
        isBackCameraSelected = streamUrl == "simulated_back_camera"
        // --- FIN DE LÓGICA MODIFICADA ---

        tvReproductorTitle.text = currentCameraName

        // Manejo de Permisos
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // --- Lógica de botones ---
        view.findViewById<AppCompatButton>(R.id.btn_play).setOnClickListener {
            // Usamos el selector de cámara actual
            val selector = if (isBackCameraSelected) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
            previewUseCase?.let { cameraProvider?.bindToLifecycle(viewLifecycleOwner, selector, it) }
            imageCapture?.let { cameraProvider?.bindToLifecycle(viewLifecycleOwner, selector, it) }
            Toast.makeText(context, "Reproducción reanudada", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<AppCompatButton>(R.id.btn_pause).setOnClickListener {
            previewUseCase?.let { cameraProvider?.unbind(it) }
            Toast.makeText(context, "Video pausado", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<AppCompatButton>(R.id.btn_mute).setOnClickListener {
            toggleAudio()
        }

        // *** CÓDIGO CORREGIDO: NAVEGACIÓN A PANTALLA HORIZONTAL CON SAFE ARGS ***
        view.findViewById<AppCompatButton>(R.id.btn_fullscreen).setOnClickListener {
            // 1. Detenemos la cámara antes de navegar para liberar recursos
            cameraProvider?.unbindAll()

            // 2. Usamos la variable de estado para definir el argumento
            val cameraSelectorIdentifier = if (isBackCameraSelected) "BACK" else "FRONT"

            val action = ReproductorCamaraDirections.actionReproductorCamaraToReproductorHorizontal(
                cameraSelectorKey = cameraSelectorIdentifier
            )

            // 3. Navegamos al nuevo fragmento
            try {
                findNavController().navigate(action)
            } catch (e: Exception) {
                Toast.makeText(context, "Error de navegación a Fullscreen: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        // *************************************************

        // Lógica de Cambiar Cámara
        btnCambiarCamara.setOnClickListener {
            // Invertimos el estado
            isBackCameraSelected = !isBackCameraSelected

            if (isBackCameraSelected) {
                currentCameraName = CAMARA_PASILLO
            } else {
                currentCameraName = CAMARA_SUPERIOR
            }
            tvReproductorTitle.text = currentCameraName
            startCamera() // Reinicia la cámara con la nueva selección
            Toast.makeText(context, "Cambiando a $currentCameraName", Toast.LENGTH_SHORT).show()
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
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            previewUseCase = preview
            // El selector de cámara se determina por el estado booleano
            val currentSelector = if (isBackCameraSelected) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

            imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner, currentSelector, preview, imageCapture!! // Usamos !! porque ya verificamos que no sea nulo antes
                )
                if (isMuted) toggleAudio(forceMute = true)
            } catch(exc: Exception) {
                Toast.makeText(requireContext(), "Error al iniciar la cámara: ${exc.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun toggleAudio(forceMute: Boolean = false) {
        val newState = if (forceMute) true else !isMuted
        val capture = imageCapture ?: return // Salir si imageCapture es nulo

        // Usamos el selector de cámara actual para el re-binding
        val currentSelector = if (isBackCameraSelected) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

        if (newState) {
            // Desvincular imageCapture para simular silencio (sin audio)
            cameraProvider?.unbind(capture)
            isMuted = true
            if (!forceMute) Toast.makeText(context, "Audio silenciado", Toast.LENGTH_SHORT).show()
        } else {
            // Volver a vincular imageCapture para simular audio activado
            cameraProvider?.bindToLifecycle(viewLifecycleOwner, currentSelector, capture)
            isMuted = false
            Toast.makeText(context, "Audio activado", Toast.LENGTH_SHORT).show()
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
