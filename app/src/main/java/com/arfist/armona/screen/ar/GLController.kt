package com.arfist.armona.screen.ar

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import com.arfist.armona.Quaternion
import timber.log.Timber
import java.lang.Exception
import kotlin.math.PI

class GLController(viewModel: ArViewModel, lifecycleOwner: LifecycleOwner, glView: GLView) {
    init {
        viewModel.arrowModel.roadLine.observe(lifecycleOwner, {
            Timber.i("updateRoadLine: $it")
            with(glView){
                queueEvent{
                    glRenderer.navView.roadVertexCoords = it.fold(ArrayList<Float>(), {
                            floatArr, pt -> floatArr.add(pt.x.toFloat());floatArr.add(pt.y.toFloat());floatArr
                    }).toFloatArray()
                    requestRender()
                }
            }
        })

        viewModel.cameraModel.liveViewBitmap.observe(lifecycleOwner, {
            Timber.i("updateBitmap: $it")
            with(glView) {
                queueEvent {
                    glRenderer.liveViewProgram.cameraViewBitmap = it
                    requestRender()
                }
            }
        })

        viewModel.arrowRotation.observe(lifecycleOwner, {
            Timber.i("updateRotation ${it.joinToString()} on $glView")
            try {
                with(glView!!) {
                    queueEvent {
                        glRenderer.arrowProgram.rotation = it[0] * 180.0f / PI.toFloat() *-1
                        requestRender()
                    }
                }
            } catch (e: Exception){
                Timber.e(e)
            }

        })

//        viewModel.rotationVector.observe(lifecycleOwner, {
//            try {
//                with(glView!!) {
//                    queueEvent {
////                        glRenderer.arrowProgram.quaternion = floatArrayOf(it.values[0], it.values[1], it.values[2], it.values[3])
//                        glRenderer.arrowProgram.quaternion = floatArrayOf(1f, 0f, 0f, 0f)
//                        requestRender()
//                    }
//                }
//            } catch (e: Exception){
//                Timber.e(e)
//            }
//
//        })
//        viewModel.testAndroidRotationMatrix.observe(lifecycleOwner, {
//            try {
//                with(glView!!) {
//                    queueEvent {
//                        glRenderer.arrowProgram.rotationMatrix = it
//                        requestRender()
//                    }
//                }
//            } catch (e: Exception) {
//                Timber.e(e)
//            }
//        })
    }
}