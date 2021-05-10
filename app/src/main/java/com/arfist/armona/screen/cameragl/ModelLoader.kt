package com.arfist.armona.screen.cameragl

import android.annotation.SuppressLint
import android.content.res.AssetManager
import android.opengl.GLES20
import android.util.Log
import java.io.BufferedReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.*
import kotlin.collections.HashMap

private fun floatArrayToBuffer(fa: FloatArray): FloatBuffer {
  return ByteBuffer
    .allocateDirect(fa.size * 4)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer()
    .apply {
      put(fa)
      position(0)
    }
}

private fun floatVectorToBuffer(fa: Vector<Float>): FloatBuffer {
  return ByteBuffer
    .allocateDirect(fa.size * 4)
    .order(ByteOrder.nativeOrder())
    .asFloatBuffer()
    .apply {
      fa.forEach { put(it) }
      position(0)
    }
}

class ModelLoader(val assets: AssetManager, val folder: String) {

  fun loadOBJ(filename: String): MeshGroup {
    return parseOBJ(getSource(filename))
  }

  private fun getSource(filename: String) = assets
    .open("$folder/$filename")
    .bufferedReader()
    .use(BufferedReader::readText)

  class Polygon {
    val vectorPosition = Vector<Float>()
    val vectorTexture = Vector<Float>()
    val vectorNormal = Vector<Float>()
    fun isEmpty() = vectorPosition.isEmpty()
    @SuppressLint("Assert")
    fun numFaces(): Int {
      assert(vectorPosition.size % 3 == 0) { "Polygon vectorPosition should be mod of 3" }
      return vectorPosition.size / 3
    }
    lateinit var bufferPosition: FloatBuffer
    lateinit var bufferTexture: FloatBuffer
    lateinit var bufferNormal: FloatBuffer

    private var isFreeze = false
    fun freeze() {
      if (isFreeze) return
      isFreeze = true
      bufferPosition = floatVectorToBuffer(vectorPosition)
      bufferTexture = floatVectorToBuffer(vectorTexture)
      bufferNormal = floatVectorToBuffer(vectorNormal)
    }
//    fun bind(positionHandle: Int?, normalHandle: Int?, textureHandle: Int) {
//      freeze()
//      positionHandle?.let { GLES20.glVertexAttribPointer(it, 3, GLES20.GL_FLOAT, false, 0, bufferPosition) }
//      normalHandle?.let   { GLES20.glVertexAttribPointer(it, 3, GLES20.GL_FLOAT, false, 0, bufferNormal) }
//      textureHandle?.let  { GLES20.glVertexAttribPointer(it, 2, GLES20.GL_FLOAT, false, 0, bufferTexture) }
//    }
//    fun draw() {
//      GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, numFaces())
//    }
  }
  class Material(val name: String) {
    // DOCS: http://paulbourke.net/dataformats/mtl/
    var d = 1.0f // halo factor | material dissolve is multiplied by the texture value (dissolve = 1.0 - (N*v)(1.0-factor))
    var Ns = 198.276830f // exponent | material specular exponent is multiplied by the texture value
    var Ni = 1.0f // optical_density : Specifies the optical density for the surface.  This is also known as index of refraction.
    var Ka = floatArrayOf(1.0f, 1.0f, 1.0f) // material ambient is multiplied by the texture value
    var Kd = floatArrayOf(1.0f, 1.0f, 1.0f) // material diffuse is multiplied by the texture value
    var Ks = floatArrayOf(1.0f, 1.0f, 1.0f) // material specular is multiplied by the texture value
    fun bind(kaHandle: Int, kdHandle: Int, ksHandle: Int, nsHandle: Int) {
      GLES20.glUniform3f(kaHandle, Ka[0], Ka[1], Ka[2])
      GLES20.glUniform3f(kdHandle, Kd[0], Kd[1], Kd[2])
      GLES20.glUniform3f(ksHandle, Ks[0], Ks[1], Ks[2])
      GLES20.glUniform1f(nsHandle, Ns)
    }
  }
  class Mesh {
    var material: Material = Material("no-name-material")
    var polygon: Polygon = Polygon()
    var objectName: String? = null
    var groupName: String? = null
  }
  class MeshGroup(val meshes: Vector<Mesh>) {

  }

  @SuppressLint("Assert")
  private fun parseOBJ(source: String): MeshGroup {
    // DOCS: http://paulbourke.net/dataformats/obj/
    val meshes = Vector<Mesh>()
    var currentMesh: Mesh? = null

    val verticesOBJ = Vector<Float>()
    val texturesOBJ = Vector<Float>()
    val normalsOBJ = Vector<Float>()

    var currentObjectName: String? = null
    var currentGroupName: String? = null

    var materialsMap: HashMap<String, Material>? = null

    for (_line in source.lines()) {
      val line = _line.trim().split(' ')
      val cmd = line.first()
      val args = line.subList(1, line.size)
      when (cmd) {
        "", "#" -> {
        }
        "s" -> Log.w("Model.OBJParser", "TODO implement OBJ parser $cmd")
        "o" -> {
          currentObjectName = args[0]
          currentMesh = Mesh()
          currentMesh.objectName = currentObjectName
          currentMesh.groupName = currentGroupName
          meshes.addElement(currentMesh)
        }
        "g" -> {
          currentGroupName = args[0]
          currentMesh = Mesh()
          currentMesh.objectName = currentObjectName
          currentMesh.groupName = currentGroupName
          meshes.addElement(currentMesh)
        }
        "mtllib" -> materialsMap = this.parseMTL(this.getSource(args[0]))
        "usemtl" -> currentMesh!!.material = materialsMap!![args[0]]!!
        "v" ->  {
          args.forEach { verticesOBJ.add(it.toFloat()) }
          assert(args.size == 3) {"OBJ $cmd size not match $_line"}
        }
        "vt" -> {
          args.forEach { texturesOBJ.add(it.toFloat()) } // may be add(args[0]) add(1.0f - args[1])
          assert(args.size == 2) {"OBJ $cmd size not match $_line"}
        }
        "vn" -> {
          args.forEach { normalsOBJ.add(it.toFloat()) }
          assert(args.size == 3) {"OBJ $cmd size not match $_line"}
        }
        "f" -> {
          val vertices = when (args.size) {
            3 -> arrayOf(args[0], args[1], args[2])
            4 -> arrayOf(args[0], args[1], args[2], args[2], args[3], args[0])
            else -> throw Exception("OBJ face must be size 3 or 4 but goot ${args.size} in [$_line]")
          }
          for (_vertex in vertices) {
            val vertex = _vertex.split('/')
            val idxPosition: Int? = try { (vertex[0].toInt() - 1) * 3 } catch(e: NumberFormatException) { null }
            val idxTexture : Int? = try { (vertex[1].toInt() - 1) * 2 } catch(e: NumberFormatException) { null }
            val idxNormal  : Int? = try { (vertex[2].toInt() - 1) * 3 } catch(e: NumberFormatException) { null }
            currentMesh!!.polygon.apply {
              if (idxPosition != null) {
                vectorPosition.apply {
                  addElement(verticesOBJ[idxPosition + 0])
                  addElement(verticesOBJ[idxPosition + 1])
                  addElement(verticesOBJ[idxPosition + 2])
                }
              }
              if (idxTexture != null) {
                vectorTexture.apply {
                  addElement(texturesOBJ[idxTexture + 0])
                  addElement(texturesOBJ[idxTexture + 1])
                }
              }
              if (idxNormal != null) {
                vectorNormal.apply {
                  addElement(normalsOBJ[idxNormal + 0])
                  addElement(normalsOBJ[idxNormal + 1])
                  addElement(normalsOBJ[idxNormal + 2])
                }
              }
            }
          }
        }
        else -> throw Exception("OBJ parser not recognize $_line")
      }
    }
    meshes.removeIf { it.polygon.numFaces() == 0 }
    meshes.forEach { it.polygon.freeze() }
    return MeshGroup(meshes)
  }

  private fun parseMTL(source: String): HashMap<String, Material> {
    val materials = HashMap<String, Material>()
    var currentMaterial: Material? = null
    for (_line in source.lines()) {
      val line = _line.trim().split(" ")
      val cmd = line.first()
      val args = line.subList(1, line.count())
      when (cmd) {
        "", "#" -> {
        }
        "Ke", "Ni" -> {
          Log.w("MTL Parser", "TODO parse $cmd")
        }
        "d" -> {
          Log.w("MTL Parser", "TODO parse $cmd")
        }
        "newmtl" -> {
          currentMaterial = Material(args[0])
          materials[currentMaterial.name] = currentMaterial
        }
        "Ns" -> currentMaterial!!.Ns = args[0].toFloat()
        "Ka" -> args.forEachIndexed { idx, it -> currentMaterial!!.Ka[idx] = it.toFloat() }
        "Kd" -> args.forEachIndexed { idx, it -> currentMaterial!!.Kd[idx] = it.toFloat() }
        "Ks" -> args.forEachIndexed { idx, it -> currentMaterial!!.Ks[idx] = it.toFloat() }
        "illum" -> {
//            illum illum_# (# = 0..10)
//            0		Color on and Ambient off
//            1		Color on and Ambient on
//            2		Highlight on
//            3		Reflection on and Ray trace on
//            4		Transparency: Glass on Reflection: Ray trace on
//            5		Reflection: Fresnel on and Ray trace on
//            6		Transparency: Refraction on Reflection: Fresnel off and Ray trace on
//            7		Transparency: Refraction on Reflection: Fresnel on and Ray trace on
//            8		Reflection on and Ray trace off
//            9		Transparency: Glass on Reflection: Ray trace off
//            10		Casts shadows onto invisible surfaces
          Log.w("MTL Parser", "TODO illum is not describe [$_line]")
        }
        else -> {
          throw Exception("TODO unknown material parser command [$cmd] in ($_line)")
        }
      }
    }
    return materials
  }
}


fun main() {
  val mtl = """
    # Blender MTL File: 'arrowk.blend'
    # Material Count: 1
    
    newmtl felt
    Ns 198.276830
    Ka 1.000000 1.000000 1.000000
    Kd 0.014339 0.207794 0.800000
    Ks 0.846847 0.846847 0.846847
    Ke 0.000000 0.000000 0.000000
    Ni 1.000000
    d 1.000000
    illum 2
  """.trimIndent()
}
