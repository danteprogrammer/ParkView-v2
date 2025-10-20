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


    private val CAMARA_PASILLO = "Cámara - Pasillo"
    private val CAMARA_SUPERIOR = "Cámara - Superior"

    private val args: ReproductorCamaraArgs by navArgs()


    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var previewView: PreviewView


    private lateinit var tvReproductorTitle: TextView
    private lateinit var btnCambiarCamara: AppCompatButton
    private lateinit var controlsLayout: View
    private lateinit var reproductorCard: LinearLayout


    private var isMuted = false

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


        previewView = view.findViewById(R.id.cameraPreview)
        tvReproductorTitle = view.findViewById(R.id.tv_reproductor_title)
        btnCambiarCamara = view.findViewById(R.id.btn_cambiar_camara)
        controlsLayout = view.findViewById(R.id.controls_layout)
        reproductorCard = view.findViewById(R.id.reproductor_card)


        val streamUrl = args.streamUrl
        currentCameraName = args.cameraName
        isBackCameraSelected = streamUrl == "simulated_back_camera"

        tvReproductorTitle.text = currentCameraName


        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        view.findViewById<AppCompatButton>(R.id.btn_play).setOnClickListener {

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


        view.findViewById<AppCompatButton>(R.id.btn_fullscreen).setOnClickListener {

            cameraProvider?.unbindAll()


            val cameraSelectorIdentifier = if (isBackCameraSelected) "BACK" else "FRONT"

            val action = ReproductorCamaraDirections.actionReproductorCamaraToReproductorHorizontal(
                cameraSelectorKey = cameraSelectorIdentifier
            )


            try {
                findNavController().navigate(action)
            } catch (e: Exception) {
                Toast.makeText(context, "Error de navegación a Fullscreen: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }



        btnCambiarCamara.setOnClickListener {

            isBackCameraSelected = !isBackCameraSelected

            if (isBackCameraSelected) {
                currentCameraName = CAMARA_PASILLO
            } else {
                currentCameraName = CAMARA_SUPERIOR
            }
            tvReproductorTitle.text = currentCameraName
            startCamera()
            Toast.makeText(context, "Cambiando a $currentCameraName", Toast.LENGTH_SHORT).show()
        }

        val settingsIcon = view.findViewById<ImageView>(R.id.settings_icon)
        settingsIcon.setOnClickListener {
            findNavController().navigate(R.id.action_reproductorCamara_to_settings)
        }

    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            previewUseCase = preview

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
        val capture = imageCapture ?: return


        val currentSelector = if (isBackCameraSelected) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

        if (newState) {

            cameraProvider?.unbind(capture)
            isMuted = true
            if (!forceMute) Toast.makeText(context, "Audio silenciado", Toast.LENGTH_SHORT).show()
        } else {

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
