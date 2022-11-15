package com.deblead.arsample.ui

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.deblead.arsample.model.ArModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.collision.Box
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.FootprintSelectionVisualizer
import com.google.ar.sceneform.ux.TransformableNode
import com.google.ar.sceneform.ux.TransformationSystem
import com.xplora.arsample.R
import com.xplora.arsample.databinding.ActivityArSampleBinding
import java.util.concurrent.CompletableFuture
import com.google.ar.sceneform.rendering.PlaneRenderer


private const val BOTTOM_SHEET_PEEK_HEIGHT = 50f
private const val DOUBLE_TAP_DURATION = 1000L

class ArSampleActivity : AppCompatActivity() {

    private lateinit var transNode: TransformableNode
    lateinit var arFragment: ArFragment

    var viewNodes = mutableListOf<Node>()

    private lateinit var bindingView: ActivityArSampleBinding
    //List of Model array for load 3d Objects
    private val models = mutableListOf(
        ArModel(R.drawable.ico_horse, "Horse", R.raw.horse),
        ArModel(R.drawable.ic_tshirt, "T-Shirt", R.raw.tshirt_glb),
        ArModel(R.drawable.chroma_vid, "Card", R.raw.card),
    )

    private lateinit var selectedModel: ArModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingView = ActivityArSampleBinding.inflate(layoutInflater)
        setContentView(bindingView.root)
        arFragment = supportFragmentManager.findFragmentById(R.id.fragment) as ArFragment
        setBottomSheet()
        setupRecyclerView()
        setDoubleTapArPlanListener()
        /*getCurrentScene().addOnUpdateListener {
            rotateViewNodesTowordsUser()
        }*/
    }

    private fun setDoubleTapArPlanListener() {
        var firstTapTime = 0L
        arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            Log.d("setDoubleTapArPlanListener 2", "firstTapTime -> ${firstTapTime}")
            when {
                firstTapTime == 0L -> {
                    firstTapTime = System.currentTimeMillis()
                }
                System.currentTimeMillis() - firstTapTime < DOUBLE_TAP_DURATION -> {
                    loadModel { modelRenderable, viewRenderable ->
                        addNodeToScen(
                            hitResult.createAnchor(),
                            modelRenderable,
                            viewRenderable
                        )
                    }
                }
                else -> {
                    firstTapTime = System.currentTimeMillis()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        bindingView.rvModels.run {
            layoutManager =
                LinearLayoutManager(this@ArSampleActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = ArModelAdapter(models).apply {
                selectedModel.observe(this@ArSampleActivity, Observer {
                    this@ArSampleActivity.selectedModel = it
                    val newTitle = "Models (${it.title})"
                    bindingView.tvModel.text = newTitle
                })
            }
        }
    }

    private fun setBottomSheet() {
        val bottomSheetBehavior = BottomSheetBehavior.from(bindingView.bottomSheet)
        bottomSheetBehavior.peekHeight =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                BOTTOM_SHEET_PEEK_HEIGHT,
                resources.displayMetrics
            ).toInt()

        bottomSheetBehavior.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                bottomSheet.bringToFront()
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }
        })
    }


    private fun loadModel(callBack: (ModelRenderable, ViewRenderable) -> Unit) {

        //Load model from resources folder
        val modelRender = ModelRenderable.builder()
            .setSource(
                this, selectedModel.modelResourceId
            )
            .build()

        val viewRender = ViewRenderable.builder()
            .setView(this, getCardView())
            .build()

        CompletableFuture.allOf(modelRender, viewRender)
            .thenAccept {
                callBack(modelRender.get(), viewRender.get())
            }
            .exceptionally {
                Toast.makeText(this, "Error Loading Model $it", Toast.LENGTH_SHORT).show()
                null
            }
    }

    private fun addNodeToScen(
        anchor: Anchor,
        modelRender: ModelRenderable,
        viewRender: ViewRenderable
    ) {

        val transformationSystem =
            makeTransformationSystem()

        //create anchor node from anchor position
        val anchorNode = AnchorNode(anchor)
        val modelNode = TransformableNode(transformationSystem)
            .apply {
                renderable = modelRender
                setParent(anchorNode)
                select()
                rotationController.isEnabled = true
                scaleController.isEnabled = true
                translationController.isEnabled = false
                scaleController.maxScale = 5f
                scaleController.minScale = 0.1f
                getCurrentScene().addChild(anchorNode)
            }
        val nodeSub = Node()
        nodeSub.apply {
            setParent(modelNode)
        }
        arFragment.arSceneView.planeRenderer.material.thenAccept {
            it.setFloat3(PlaneRenderer.MATERIAL_COLOR, Color(0.0f, 0.0f, 1.0f, 1.0f));
        }

        //create view node
        val viewNode = Node().apply {
            renderable = null
            setParent(modelNode)
            val box = modelNode.renderable?.collisionShape as Box
            worldPosition = modelNode.worldPosition
            renderable?.collisionShape = box
            (viewRender.view as View).setOnClickListener {
                getCurrentScene().removeChild(anchorNode)
                viewNodes.remove(this)
            }
        }
        viewNodes.add(viewNode)

        getCurrentScene().addOnPeekTouchListener { hitTestResult, motionEvent ->
            transformationSystem.onTouch(
                hitTestResult,
                motionEvent
            )
        }

    }

    private fun makeTransformationSystem(): TransformationSystem {
        val selectionVisualizer = FootprintSelectionVisualizer()
        val transformationSystem = TransformationSystem(
            this.resources.displayMetrics,
            selectionVisualizer
        )
        return transformationSystem
    }

    //Method to rotate view object as front of user
    private fun rotateViewNodesTowordsUser() {
        for (node in viewNodes) {
            node.renderable?.let {
                val camPos = getCurrentScene().camera.worldPosition
                val viewNodePos = node.worldPosition
                val dir = Vector3.subtract(camPos, viewNodePos)
                node.worldRotation = Quaternion.lookRotation(dir, Vector3.up())
            }
        }
    }

    private fun getCurrentScene() = arFragment.arSceneView.scene

    private fun getCardView(): View {
        var view = LayoutInflater.from(this).inflate(R.layout.card_lay, null, false)
        view.findViewById<AppCompatImageView>(R.id.mImgLoad)
        return view
    }
}