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


    private val TAG = "ReproductorHorizontal"


    private val args: ReproductorHorizontalArgs by navArgs()


    private var _binding: ActivityReproductorHorizontalBinding? = null
    private val binding get() = _binding!!


    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var previewView: PreviewView

    private var imageCapture: ImageCapture? = null
    private var previewUseCase: Preview? = null
    private var isMuted = false


    private lateinit var cameraSelector: CameraSelector


    private lateinit var onBackPressedCallback: OnBackPressedCallback


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()


        val selectorString = args.cameraSelectorKey


        cameraSelector = when (selectorString) {
            "FRONT" -> CameraSelector.DEFAULT_FRONT_CAMERA
            "BACK" -> CameraSelector.DEFAULT_BACK_CAMERA
            else -> CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = ActivityReproductorHorizontalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        previewView = binding.cameraPreviewHorizontal


        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

                val success = findNavController().popBackStack()

                if (!success) {

                    Log.e(TAG, "Error: popBackStack() falló. Volviendo a Dashboard como fallback.")
                    Toast.makeText(requireContext(), "Error de navegación, volviendo al Dashboard.", Toast.LENGTH_LONG).show()

                    findNavController().navigate(R.id.dashboard)
                }
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)


        startCamera()




        binding.btnBack.setOnClickListener {
            onBackPressedCallback.handleOnBackPressed()
        }


        binding.btnPlayHorizontal.setOnClickListener {
            previewUseCase?.let { cameraProvider?.bindToLifecycle(viewLifecycleOwner, cameraSelector, it) }
            imageCapture?.let { cameraProvider?.bindToLifecycle(viewLifecycleOwner, cameraSelector, it) }
            Toast.makeText(context, "Reproducción reanudada", Toast.LENGTH_SHORT).show()
        }


        binding.btnPauseHorizontal.setOnClickListener {
            previewUseCase?.let { cameraProvider?.unbind(it) }
            Toast.makeText(context, "Video pausado", Toast.LENGTH_SHORT).show()
        }


        binding.btnMuteHorizontal.setOnClickListener {
            toggleAudio()
        }
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            previewUseCase = preview

            imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
            try {

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner, cameraSelector, preview, imageCapture!!
                )
                if (isMuted) toggleAudio(true)
            } catch(exc: Exception) {
                Toast.makeText(requireContext(), "Error al iniciar la cámara: ${exc.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }


    private fun toggleAudio(forceMute: Boolean = false) {
        val newState = if (forceMute) true else !isMuted

        val capture = imageCapture ?: return

        if (newState) {

            cameraProvider?.unbind(capture)
            isMuted = true
            if (!forceMute) {
                Toast.makeText(context, "Audio silenciado", Toast.LENGTH_SHORT).show()
            }
        } else {

            cameraProvider?.bindToLifecycle(viewLifecycleOwner, cameraSelector, capture)
            isMuted = false
            Toast.makeText(context, "Audio activado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        if (::onBackPressedCallback.isInitialized) {
            onBackPressedCallback.isEnabled = false
        }


        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()


        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        _binding = null
    }
}