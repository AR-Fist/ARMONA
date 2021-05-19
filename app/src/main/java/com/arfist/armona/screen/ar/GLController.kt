package com.arfist.armona.screen.ar

import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import com.arfist.armona.Quaternion
import com.arfist.armona.RadToDeg
import timber.log.Timber
import java.lang.Exception
import kotlin.math.PI

class GLController(viewModel: ArViewModel, lifecycleOwner: LifecycleOwner, glView: GLView) {
    init {
        viewModel.arrowModel.roadLine.observe(lifecycleOwner, {
//            Timber.i("updateRoadLine: $it")
            try {
                with(glView){
                    queueEvent{
                        with(glRenderer){
                            withGLProgram {
                                navView.roadVertexCoords = it.fold(ArrayList<Float>(), {
                                        floatArr, pt -> floatArr.add(pt.x.toFloat());floatArr.add(pt.y.toFloat());floatArr
                                }).toFloatArray()
                            }
                        }
                        requestRender()
                    }
                }
            } catch(e: Exception) {
                Timber.e(e)
            }
        })

        viewModel.cameraModel.liveViewBitmap.observe(lifecycleOwner, {
//            Timber.i("updateBitmap: $it")
            try {
                with(glView!!) {
                    queueEvent {
                        with(glRenderer) {
                            withGLProgram {
                                liveViewProgram.cameraViewBitmap = it
                            }
                        }
                        requestRender()
                    }
                }
            } catch(e: Exception) {
                Timber.e(e)
            }
        })
//
//        viewModel.arrowRotationSlerp.observe(lifecycleOwner, {
//            Timber.i("updateRotation ${it.joinToString()} on $glView")
//            try {
//                with(glView!!) {
//                    queueEvent {
//                        glRenderer.arrowProgram.rotation = it[0] * 180.0f / PI.toFloat() *-1
//                        requestRender()
//                    }
//                }
//            } catch (e: Exception){
//                Timber.e(e)
//            }
//
//        })

        viewModel.newDegree.observe(lifecycleOwner, {
//            Log.i("Degree arrow", it.toString())
            try{
                with(glView!!) {
                    queueEvent{
                        glRenderer.arrowProgram.rotation = -it
                        requestRender()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
            }
        })

//        viewModel.arrowRotationSlerpQuaternion.observe(lifecycleOwner, {
//            try {
//                with(glView!!) {
//                    queueEvent {
//                        glRenderer.arrowProgram.quaternion = it
//                        requestRender()
//                    }
//                }
//            } catch (e: Exception){
//                Timber.e(e)
//            }
//        })

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