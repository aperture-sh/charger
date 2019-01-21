package io.marauder.supercharged

class Intersector {
    fun intersects(b1: List<Double>, b2: List<Double>): Boolean =
            (b1[0] < b2[2] && b1[2] > b2[0] && b1[3] > b2[1] && b1[1] < b2[3])

    fun includesPoints(b: List<Double>, coords: List<List<Double>>): Boolean =
            coords.map { includesPoint(b, it) }.fold(true) { b1, b2 -> b1 && b2 }

    fun includesPoint(b: List<Double>, coord: List<Double>): Boolean =
            coord[0] < b[2] && coord[0] > b[0] && coord[1] < b[3] && coord[1] > b[1]
}