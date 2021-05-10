package com.arfist.armona.screen.ar

import android.graphics.Bitmap
import android.util.Log
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import timber.log.Timber
import kotlin.math.PI
import kotlin.math.atan2

typealias Transformer = (Point?, Size) -> Point

class RoadDetectionService(magicX: Float, magicY: Float) {
    private val magicX = magicX;
    private val magicY = magicY;

    private var lineCaches = arrayOfNulls<Point>(4)
    private val finalCoordTransformer: Array<Transformer> = arrayOf(
        { p, imgSize ->
            if(p == null)
                Point(imgSize.width / 2f - magicX, imgSize.height / 2f - magicY)
            else
                Point(p.x, (p.y + (imgSize.height / 2f - magicY)) / 2f)
        },
        { p, imgSize ->
            if(p == null)
                Point(imgSize.width, 0.0)
            else
                Point(p.x, p.y / 2f)
        },
        { p, imgSize ->
            if(p == null)
                Point(imgSize.width, imgSize.height)
            else
                Point(p.x, (p.y + imgSize.height) / 2f)
        },
        { p, imgSize ->
            if(p == null)
                Point(imgSize.width / 2f - magicX, imgSize.height / 2f + magicY)
            else
                Point(p.x, (p.y + (imgSize.height / 2f + magicY)) / 2f)
        },
    )

    public fun detectRoadFromBitmap(i: Mat): ArrayList<ArrayList<Int>> {
        val roi = roi(detectEdges(i))

        var lines = Mat()
        var roi_gray = Mat()
        Imgproc.cvtColor(roi, roi_gray, Imgproc.COLOR_RGB2GRAY, 4)
        Imgproc.HoughLinesP(roi_gray, lines, 1.0, PI / 180.0, 50, 30.0, 10.0)

        Log.d("Houghlines",lines.rows().toString())

        val resultLine = average_slope_intercept(i,lines)
        val imageSize = Size(i.cols().toDouble(), i.rows().toDouble())
        val (right, left) = arrayOf("LeftLine", "RightLine").mapIndexed { i, _ ->
            val line = resultLine[i]
            Timber.i("Process: $i")
            arrayOf(0, 1).mapIndexed { j, _ ->
                val rawPt = Point(line[j * 2].toDouble(), line[j * 2 + 1].toDouble())
                val index = i * 2 + j
                val maskedPt = if (rawPt == Point(0.0, 0.0)) lineCaches[index] else rawPt
                finalCoordTransformer[index](maskedPt, imageSize)
            }.fold(ArrayList<Int>(), {
                l, pt ->
                    l.add(pt.x.toInt());l.add(pt.y.toInt())
                    l
            })
        }

        if (left[3] < right[1]) {
            Log.d("Cross","line cross")
            val temp = left[3]
            left[3] = right[1]
            right[1] = temp
        }

        return arrayListOf(left, right)
    }

    // Perform edge detection
    private fun detectEdges(i: Mat): Mat {
        val gauss = Mat()
        val edges = Mat(i.size(), CvType.CV_8UC1)
        Imgproc.cvtColor(i, gauss, Imgproc.COLOR_RGB2GRAY, 4)
        Imgproc.GaussianBlur(gauss, edges, Size(3.0, 3.0), 3.0, 3.0)
        Imgproc.Canny(edges, edges, 80.0, 100.0)
        Imgproc.cvtColor(edges, edges, Imgproc.COLOR_GRAY2RGBA)
        return edges
    }

    private fun roi(canny: Mat): Mat {
        val h = canny.height()
        val w = canny.width()
        Log.d("ROI", "img >> "+ canny.width().toString() + ", " + canny.height().toString() + ", " + canny.type().toString())
        var mask = Mat(canny.height(), canny.width(), CvType.CV_8UC4, Scalar(0.0, 0.0, 0.0))
        Log.d("ROI", "mask >> "+ mask.width().toString() + ", " + mask.height().toString() + ", " + mask.type().toString())
        var points: List<Point> = listOf<Point>(Point(w.toDouble(), 0.0), Point(w.toDouble(), h.toDouble()), Point(w / 2.0 - magicX, (h / 2.0) + magicY), Point(w / 2.0 - magicX, (h / 2.0) - magicY))
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

    private fun average_slope_intercept(i : Mat, lines: Mat): ArrayList<ArrayList<Int>> {
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
        var left_line = make_coor(i,left_slope_mean,left_intercept_mean, "Left")
        var right_line = make_coor(i,right_slope_mean,right_intercept_mean, "Right")
        return arrayListOf(left_line,right_line)
    }

    private fun make_coor(i: Mat, slope: Double, intercept: Double, side: String): ArrayList<Int> {
        val h = i.rows()
        val w = i.cols()
        var roadLine = arrayListOf(0, 0, 0, 0)

        // Find intercept in trapezoid

        val top_trapezoid_intersection = lineIntersection(slope,intercept,
            arrayListOf(w/2-magicX,h/2-magicY,w/2-magicX,h/2+magicY).map { it.toInt() } as ArrayList<Int>)
        val bottom_trapezoid_intersection = lineIntersection(slope,intercept, arrayListOf(w,0,w,h))
        val left_trapezoid_intersection = lineIntersection(slope,intercept,
            arrayListOf(w,h,w/2-magicX,h/2+magicY).map { it.toInt() } as ArrayList<Int>)
        val right_trapezoid_intersection = lineIntersection(slope,intercept,
            arrayListOf(w/2-magicX,h/2-magicY,w,0).map { it.toInt() } as ArrayList<Int>)

        if(top_trapezoid_intersection != Pair(-1,-1)) {
            if(side == "Left") {
                roadLine[0] = top_trapezoid_intersection.first
                roadLine[1] = top_trapezoid_intersection.second
            }
            else {
                roadLine[2] = top_trapezoid_intersection.first
                roadLine[3] = top_trapezoid_intersection.second
            }
        }
        if(bottom_trapezoid_intersection != Pair(-1,-1)) {
            if(side == "Left") {
                roadLine[2] = bottom_trapezoid_intersection.first
                roadLine[3] = bottom_trapezoid_intersection.second
            }
            else {
                roadLine[0] = bottom_trapezoid_intersection.first
                roadLine[1] = bottom_trapezoid_intersection.second
            }
        }
        if(left_trapezoid_intersection != Pair(-1,-1)) {
            if(side == "Left") {
                roadLine[2] = w
                roadLine[3] = 0
            }
        }
        if(right_trapezoid_intersection != Pair(-1,-1)) {
            if(side == "Right") {
                roadLine[0] = w
                roadLine[1] = h
            }
        }
        return roadLine
    }

    private fun lineIntersection(line1_slope : Double, line1_intercept : Double , line2 : ArrayList<Int> ): Pair<Int, Int> {

        // line1 from road line, line2 from trapezoid

        val m2 =
            (line2[3] - line2[1]) / (line2[2] - line2[0] + 0.0001) // slope (divided by zero prevention)
        val b2 = line2[1] - (m2 * line2[0]) // y-intercept
        if (line1_slope == m2) {
            return Pair(-1, -1) // parallel line
        } else {
            val xi = (line1_intercept - b2) / (m2 - line1_slope)
            val yi = line1_slope * xi + line1_intercept
            return Pair(xi.toInt(), yi.toInt())
        }
    }
}

