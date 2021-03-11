package com.arfist.armona.screen.ar

import android.graphics.Bitmap
import android.util.Log
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.PI
import kotlin.math.atan2

fun detectRoadFromBitmap(i: Mat): ArrayList<ArrayList<Int>> {
    val roi = roi(detectEdges(i))

    var lines = Mat()
    var roi_gray = Mat()
    Imgproc.cvtColor(roi, roi_gray, Imgproc.COLOR_RGB2GRAY, 4)
    Imgproc.HoughLinesP(roi_gray, lines, 1.0, PI / 180.0, 50, 30.0, 10.0)

    Log.d("Houghlines",lines.rows().toString())

    return average_slope_intercept(i,lines)
}

// Perform edge detection
 fun detectEdges(i: Mat): Mat {
    val gauss = Mat()
    val edges = Mat(i.size(), CvType.CV_8UC1)
    Imgproc.cvtColor(i, gauss, Imgproc.COLOR_RGB2GRAY, 4)
    Imgproc.GaussianBlur(gauss, edges, Size(3.0, 3.0), 3.0, 3.0)
    Imgproc.Canny(edges, edges, 80.0, 100.0)
    Imgproc.cvtColor(edges, edges, Imgproc.COLOR_GRAY2RGBA)
    return edges
}

 fun roi(canny: Mat): Mat {
     val h = canny.height()
     val w = canny.width()
    Log.d("ROI", "img >> "+ canny.width().toString() + ", " + canny.height().toString() + ", " + canny.type().toString())
    var mask = Mat(canny.height(), canny.width(), CvType.CV_8UC4, Scalar(0.0, 0.0, 0.0))
    Log.d("ROI", "mask >> "+ mask.width().toString() + ", " + mask.height().toString() + ", " + mask.type().toString())
    var points: List<Point> = listOf<Point>(Point(w.toDouble(), 0.0), Point(w.toDouble(), h.toDouble()), Point(w / 2.0 - 20.0, (h / 2.0) + 150.0), Point(w / 2.0 - 20.0, (h / 2.0) - 150.0))
    var mpoints = MatOfPoint()
    mpoints.fromList(points)
    var finalpoints = ArrayList<MatOfPoint>()
    finalpoints.add(mpoints)
    Imgproc.fillPoly(mask, finalpoints, Scalar(255.0, 255.0, 255.0))
    var dst = Mat()
    Core.bitwise_and(canny, mask, dst)
    Log.d("ROI", "dst >> "+ dst.width().toString() + ", " + dst.height().toString() + ", " + dst.type().toString())
    return dst
}

 fun average_slope_intercept(i : Mat, lines: Mat): ArrayList<ArrayList<Int>> {
    var left_slope_mean = 0.0
    var right_slope_mean = 0.0
    var left_intercept_mean = 0.0
    var right_intercept_mean = 0.0
    var left_n = 0
    var right_n = 0
    for(i in 0 until lines.rows()) {
        val points = lines[i, 0]
        var x1 = points[0]
        var y1 = points[1]
        var x2 = points[2]
        var y2 = points[3]

        // cut out some lines
        val Angle: Double = atan2(y2 - y1, x2 - x1) * 180.0 / PI
        if( ( -90.0 <= Angle && Angle <= -60.0) || (Angle in 60.0..90.0)) {
            continue
        }
        Log.d("Angle", Angle.toString())

        var slope = (y2-y1)/(x2-x1)
        var intercept = y1 - (slope * x1)
        Log.d("average_slope_intercept","slope: " + slope.toString() + ", intercept: " + intercept.toString())
        if(slope < 0) {
            left_slope_mean += slope
            left_intercept_mean += intercept
            left_n +=1
        }
        else {
            right_slope_mean += slope
            right_intercept_mean += intercept
            right_n +=1
        }
    }
    left_slope_mean /= left_n
    left_intercept_mean /= left_n
    right_slope_mean /= right_n
    right_intercept_mean /= right_n
    var left_line = make_coor(i,left_slope_mean,left_intercept_mean)
    var right_line = make_coor(i,right_slope_mean,right_intercept_mean)
    return arrayListOf(left_line,right_line)
}

 fun make_coor(i: Mat, slope: Double, intercept: Double): ArrayList<Int> {
    var y1 = i.cols()
    var y2 = (y1 * (4/5))
    var x1 = ((y1 - intercept) / slope).toInt()
    var x2 = ((y2 - intercept) / slope).toInt()

    return arrayListOf(x1, y1, x2, y2)
}