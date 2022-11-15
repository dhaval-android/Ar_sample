package com.deblead.arsample.ui

import android.R.attr.data
import android.app.Activity
import android.app.ActivityManager
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.ExternalTexture
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.xplora.arsample.R
import com.xplora.arsample.databinding.ActivityChromeVideoBinding


// The color to filter out of the video.
private val CHROMA_KEY_COLOR = Color(0.1843f, 1.0f, 0.098f)

// Controls the height of the video in world space.
private const val VIDEO_HEIGHT_METERS = 0.85f
const val MIN_OPENGL_VERSION = 3.0

class ChromeVideoActivity : AppCompatActivity() {

    private lateinit var externalTextureView: ExternalTexture
    private var videoRenderable: ModelRenderable? = null
    private lateinit var url: Uri
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var arFragment: ArFragment
    private lateinit var binding: ActivityChromeVideoBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChromeVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        arFragment = supportFragmentManager.findFragmentById(R.id.fragment) as ArFragment
        pickVideo()

    }

    private fun pickVideo() {
        val intent = Intent()
        intent.type = "video/*"
        intent.action = Intent.ACTION_GET_CONTENT
        videoActivityResultLauncher.launch(intent)
    }

    private val videoActivityResultLauncher = registerForActivityResult(
        StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK && it.data != null && it.data!!.data != null) {
            val selectedImageUri: Uri? = it.data!!.data
            if (selectedImageUri != null) {
                url = selectedImageUri
                initAllObj()
                initDoubleTap()
            } else {
                val toast =
                    Toast.makeText(this, "Unable to load video renderable", Toast.LENGTH_LONG)
            }
        }
    }

    private fun initDoubleTap() {
        arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            if (videoRenderable == null) {
                return@setOnTapArPlaneListener
            }
            val anchor = hitResult.createAnchor()
            val anchorNode = AnchorNode(anchor).apply {
                setParent(arFragment.arSceneView.scene)
            }

            val videoNode = TransformableNode(arFragment.transformationSystem)
                .apply {
                    setParent(anchorNode)
                }
            val videoWidth = mediaPlayer?.videoWidth
            val videoHeight = mediaPlayer?.videoHeight
            if (videoWidth != null && videoHeight != null) {
                videoNode.localScale = Vector3(
                    VIDEO_HEIGHT_METERS * (videoWidth / videoHeight),
                    VIDEO_HEIGHT_METERS, 1.0f
                )
            }
            if (!mediaPlayer!!.isPlaying) {
                mediaPlayer!!.start()
                externalTextureView
                    .surfaceTexture
                    .setOnFrameAvailableListener {
                        videoNode.renderable = videoRenderable
                        externalTextureView.surfaceTexture.setOnFrameAvailableListener(null)
                    }
            } else {
                videoNode.renderable = videoRenderable
            }
        }
    }

    private fun initAllObj() {
        externalTextureView = ExternalTexture()
        mediaPlayer = MediaPlayer.create(this, url)
            .apply {
                setSurface(externalTextureView.surface)
                isLooping = true
            }
        ModelRenderable.builder()
            .setSource(this, R.raw.chroma_key_video)
            .build()
            .thenAccept {
                videoRenderable = it
                it.material.setExternalTexture("videoTexture", externalTextureView)
                it.material.setFloat4("keyColor", CHROMA_KEY_COLOR)
            }
            .exceptionally {
                val toast =
                    Toast.makeText(this, "Unable to load video renderable", Toast.LENGTH_LONG)
                toast.setGravity(Gravity.CENTER, 0, 0)
                toast.show()
                null
            }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }



    private fun scaleCenterInside(
        videoWidth: Float,
        videoHeight: Float,
        imageWidth: Float,
        imageHeight: Float
    ): Vector3 {
        val isVideoVertical = videoHeight > videoWidth

        val videoAspectRatio =
            if (isVideoVertical) videoHeight / videoWidth else videoWidth / videoHeight
        val imageAspectRatio =
            if (isVideoVertical) imageHeight / imageWidth else imageWidth / imageHeight


        return if (isVideoVertical) {
            if (videoHeight > imageHeight) {
                Vector3(videoHeight / imageHeight, 1.0f, videoAspectRatio)
            } else {
                Vector3(videoHeight / imageHeight, 1.0f, imageAspectRatio)
            }

        } else {
            if (videoWidth > imageWidth) {
                Vector3(videoAspectRatio, 1.0f, videoWidth / imageWidth)
            } else {
                Vector3(imageAspectRatio, 1.0f, imageWidth / videoWidth)
            }
        }
    }

    private fun scaleCenterCropNew(
        videoWidth: Float,
        videoHeight: Float,
        imageWidth: Float,
        imageHeight: Float
    ): Vector3 {
        val isVideoVertical = videoHeight > videoWidth
        val videoAspectRatio =
            if (isVideoVertical) videoHeight / videoWidth else videoWidth / videoHeight
        val imageAspectRatio =
            if (isVideoVertical) imageHeight / imageWidth else imageWidth / imageHeight


        return if (isVideoVertical) {
            if (videoWidth > imageWidth) {
                Vector3(videoAspectRatio, videoWidth / imageWidth, 1.0f)
            } else {
                Vector3(imageAspectRatio, imageWidth / videoWidth, 1.0f)
            }
        } else {
            if (videoHeight > imageHeight) {
                Vector3(videoHeight / imageHeight, videoAspectRatio, 1.0f)
            } else {
                Vector3(imageHeight / videoHeight, imageAspectRatio, 1.0f)
            }
        }
    }

    private fun scaleCenterCrop(
        videoWidth: Float,
        videoHeight: Float,
        imageWidth: Float,
        imageHeight: Float
    ): Vector3 {
        val isVideoVertical = videoHeight > videoWidth
        val videoAspectRatio =
            if (isVideoVertical) videoHeight / videoWidth else videoWidth / videoHeight
        val imageAspectRatio =
            if (isVideoVertical) imageHeight / imageWidth else imageWidth / imageHeight

        return if (isVideoVertical) {
            if (videoAspectRatio > imageAspectRatio) {
                Vector3(imageWidth, 1.0f, imageWidth * videoAspectRatio)
            } else {
                Vector3(imageHeight / videoAspectRatio, 1.0f, imageHeight)
            }
        } else {
            if (videoAspectRatio > imageAspectRatio) {
                Vector3(imageHeight * videoAspectRatio, 1.0f, imageHeight)
            } else {
                Vector3(imageWidth, 1.0f, imageWidth / videoAspectRatio)
            }
        }
    }

    private fun scaleFitXY(imageWidth: Float, imageHeight: Float): Vector3 {
        return Vector3(imageWidth, 1.0f, imageHeight)
    }

}