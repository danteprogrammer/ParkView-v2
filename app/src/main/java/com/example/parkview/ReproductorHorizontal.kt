package com.example.parkview

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.navigation.fragment.navArgs
import com.example.parkview.databinding.ActivityReproductorHorizontalBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.activity.OnBackPressedCallback

/**
 * Fragmento diseñado para mostrar la reproducción de la cámara en modo horizontal/fullscreen.
 */
class ReproductorHorizontal : Fragment() {

    // Tag para registro de errores
    private val TAG = "ReproductorHorizontal"

    // Uso de Safe Args para obtener los argumentos pasados por NavGraph
    private val args: ReproductorHorizontalArgs by navArgs()

    // View Binding
    private var _binding: ActivityReproductorHorizontalBinding? = null
    private val binding get() = _binding!!

    // Ejecutor para las tareas de la cámara
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var previewView: PreviewView

    private var imageCapture: ImageCapture? = null
    private var previewUseCase: Preview? = null
    private var isMuted = false

    // El CameraSelector real que usaremos para bindToLifecycle
    private lateinit var cameraSelector: CameraSelector

    // Variable de clase para manejar el callback de retroceso
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    // ----------------------------------------------------------------
    // CICLO DE VIDA
    // ----------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // OBTENER Y CONVERTIR EL ARGUMENTO DE LA CÁMARA USANDO SAFE ARGS
        val selectorString = args.cameraSelectorKey

        // Convertir la cadena de CameraSelector a la instancia real (Back por defecto)
        cameraSelector = when (selectorString) {
            "FRONT" -> CameraSelector.DEFAULT_FRONT_CAMERA
            "BACK" -> CameraSelector.DEFAULT_BACK_CAMERA
            else -> CameraSelector.DEFAULT_BACK_CAMERA // Fallback
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflar el layout usando View Binding
        _binding = ActivityReproductorHorizontalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        // Forzar la orientación horizontal (pantalla completa) al entrar o reanudar
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar la PreviewView usando el binding
        previewView = binding.cameraPreviewHorizontal

        // Manejo del Botón Atrás del dispositivo
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Navegar hacia atrás, lo que nos lleva a ReproductorCamara
                val success = findNavController().popBackStack()

                if (!success) {
                    // Si falla, registramos el error y forzamos la navegación a un destino seguro.
                    Log.e(TAG, "Error: popBackStack() falló. Volviendo a Dashboard como fallback.")
                    Toast.makeText(requireContext(), "Error de navegación, volviendo al Dashboard.", Toast.LENGTH_LONG).show()
                    // Reemplaza R.id.dashboard con tu ID de destino de inicio real si es diferente
                    findNavController().navigate(R.id.dashboard)
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)

        // Iniciar la cámara con el selector recuperado
        startCamera()

        // --- LÓGICA DE BOTONES ---

        // Botón de regresar (Salir de Fullscreen).
        binding.btnBack.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }

        // Botón Play
        binding.btnPlayHorizontal.setOnClickListener {
            previewUseCase?.let { cameraProvider?.bindToLifecycle(viewLifecycleOwner, cameraSelector, it) }
            imageCapture?.let { cameraProvider?.bindToLifecycle(viewLifecycleOwner, cameraSelector, it) }
            Toast.makeText(context, "Reproducción reanudada", Toast.LENGTH_SHORT).show()
        }

        // Botón Pause
        binding.btnPauseHorizontal.setOnClickListener {
            previewUseCase?.let { cameraProvider?.unbind(it) }
            Toast.makeText(context, "Video pausado", Toast.LENGTH_SHORT).show()
        }

        // Botón Mute
        binding.btnMuteHorizontal.setOnClickListener {
            toggleAudio()
        }
    }

    // ----------------------------------------------------------------
    // LÓGICA DE CÁMARA
    // ----------------------------------------------------------------

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            // Configuración del caso de uso de vista previa
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            previewUseCase = preview
            // Configuración del caso de uso de captura de imagen (para simular audio/captura)
            imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
            try {
                // Desvincular cualquier uso anterior y vincular los nuevos casos de uso
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture!!
                )
                if (isMuted) toggleAudio(true) // Mantener el estado de mute
            } catch(exc: Exception) {
                Toast.makeText(requireContext(), "Error al iniciar la cámara: ${exc.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // Adaptación del método toggleAudio para aceptar un booleano (para mantener el estado)
    private fun toggleAudio(forceMute: Boolean = false) {
        val newState = if (forceMute) true else !isMuted

        val capture = imageCapture ?: return

        if (newState) {
            // Desvincular imageCapture para simular silencio
            cameraProvider?.unbind(capture)
            isMuted = true
            if (!forceMute) {
                Toast.makeText(context, "Audio silenciado", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Volver a vincular imageCapture para activar audio
            cameraProvider?.bindToLifecycle(viewLifecycleOwner, cameraSelector, capture)
            isMuted = false
            Toast.makeText(context, "Audio activado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Desactivar explícitamente el callback de retroceso al destruir la vista.
        if (::onBackPressedCallback.isInitialized) {
            onBackPressedCallback.isEnabled = false
        }

        // Liberar recursos de la cámara
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()

        // **IMPORTANTE: Restaurar la orientación a PORTRAIT o UNSPECIFIED al salir**
        // Esto asegura que el Fragmento ReproductorCamara se muestre verticalmente al regresar.
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        _binding = null
    }
}