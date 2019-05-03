package io.marauder.charged

class Simplifier {
    fun simplify(points: List<List<Double>>, sqTolerance: Double) : List<List<Double>> {
        if (points.isEmpty()) return listOf()
        val simple = mutableListOf(points[0])
        points.forEach { p -> if (getSquareDistance(p, simple.last()) > Math.pow(sqTolerance, 2.0)) simple.add(p)}
        if (simple.last() != points.last() || simple.size < 4) {
            simple.add(points.last())
//        println("last")
        }
//    println("${Math.pow(sqTolerance, 2.0)}>>${points.size}<>${simple.size}")
        if (simple.size < 4) {
            return listOf(points[0])
        }
        return simple
    }

    fun getSquareDistance(p1: List<Double>, p2: List<Double>): Double =
            Math.pow( p1[0] - p2[0], 2.0) + Math.pow(p1[1] - p2[1], 2.0)

    fun getSquareSegmentDistance(p0: List<Double>, p1: MutableList<Double>, p2: List<Double>) : Double {

        val dx = p2[0] - p1[0]
        val dy = p2[1] - p1[1]

        if (dx != 0.0 || dy != 0.0) {
            val t = ((p0[0] - p1[0]) * dx + (p0[1] - p1[1]) * dy) / (dx * dx + dy * dy)

            if (t > 1.0) {
                p1[0] = p2[0]
                p1[1] = p2[1]
            } else if (t > 0.0) {
                p1[0] += dx * t
                p1[1] += dy * t
            }
        }

        return getSquareDistance(p0, p1)
    }
}
