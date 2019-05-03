package io.marauder.charged

import io.marauder.charged.models.Feature
import io.marauder.charged.models.GeoJSON
import io.marauder.charged.models.Geometry

/**
 * The clipper provides functions to clip generic geometries and given boundaries
 * @property calcBoundingBoxes If true calculate bounding boxes before clipping
 */
class Clipper(private val calcBoundingBoxes: Boolean = false) {

    /**
     * Clip a feature collection at given boundaries and a scaling factor
     * @param fc Feature Collection to clip
     * @param scale Scaling factor to use before clipping
     * @param k1 low horizontal boundary
     * @param k2 high horizontal boundary
     * @param k3 low vertical boundary
     * @param k4 high vertical boundary
     * @return Clipped Feature Collection
     */
    fun clip(fc: GeoJSON, scale: Double, k1: Double, k2: Double, k3: Double, k4: Double, checkBoundingBox: Boolean = true) : GeoJSON {
        val scaleK1 = k1 / scale
        val scaleK2 = k2 / scale
        val scaleK3 = k3 / scale
        val scaleK4 = k4 / scale
        val minFCx = fc.bbox[0]
        val maxFCx = fc.bbox[2 + 0]
        val minFCy = fc.bbox[1]
        val maxFCy = fc.bbox[2 + 1]

        if (checkBoundingBox && (minFCx >= scaleK2 || maxFCx <= scaleK1 || minFCy >= scaleK4 || maxFCy <= scaleK3)) {
            return GeoJSON(features = emptyList())
        }
        return GeoJSON(features =
        fc.features.filter { f ->
            val minX = f.bbox[0]
            val maxX = f.bbox[2 + 0]
            val minY = f.bbox[1]
            val maxY = f.bbox[2 + 1]
        // condition for trivia reject
            !(minX > scaleK2 || maxX < scaleK1 || minY > scaleK4 || maxY < scaleK3) || !checkBoundingBox
        }.flatMap { f -> clipFeature(f, scaleK1, scaleK2, scaleK3, scaleK4) }
        )
    }

    /**
     * Clip a feature at given boundaries and a scaling factor
     * @param f Feature to clip
     * @param k1 low horizontal boundary
     * @param k2 high horizontal boundary
     * @param k3 low vertical boundary
     * @param k4 high vertical boundary
     * @return Clipped Feature
     */
    fun clip(f: Feature, scale: Double, k1: Double, k2: Double, k3: Double, k4: Double, checkBoundingBox: Boolean = true) : Feature? {
        val scaleK1 = k1 / scale
        val scaleK2 = k2 / scale
        val scaleK3 = k3 / scale
        val scaleK4 = k4 / scale
        val minX = f.bbox[0]
        val maxX = f.bbox[2 + 0]
        val minY = f.bbox[1]
        val maxY = f.bbox[2 + 1]

        if (checkBoundingBox && (minX >= scaleK2 || maxX <= scaleK1 || minY >= scaleK4 || maxY <= scaleK3)) {
            return null
        }
        return clipFeature(f, scaleK1, scaleK2, scaleK3, scaleK4).firstOrNull()
    }

    /**
     * Clip a feature at given boundaries
     * @param f Feature to clip
     * @param k1 low horizontal boundary
     * @param k2 high horizontal boundary
     * @param k3 low vertical boundary
     * @param k4 high vertical boundary
     * @return Clipped Feature
     */
    private fun clipFeature(f: Feature, k1: Double, k2: Double, k3: Double, k4: Double) = when (f.geometry) {
        is Geometry.Point -> listOf(Feature(
                geometry = Geometry.Point(coordinates = filterPoints(listOf((f.geometry as Geometry.Point).coordinates), k1, k2, k3, k4).firstOrNull() ?: emptyList()),
                properties = f.properties
        ))
        is Geometry.MultiPoint -> listOf(Feature(
                geometry = Geometry.MultiPoint(coordinates = filterPoints((f.geometry as Geometry.MultiPoint).coordinates, k1, k2, k3, k4)),
                properties = f.properties
        ))
        is Geometry.LineString -> {
            listOf(Feature(geometry = Geometry.LineString(
                    coordinates = clipLine(clipLine(listOf((f.geometry as Geometry.LineString).coordinates), k1, k2, 0), k3, k4, 1).firstOrNull() ?: emptyList()),
                    properties = f.properties)
            )
        }
        is Geometry.MultiLineString -> {
            listOf(Feature(geometry = Geometry.MultiLineString(
                    coordinates = clipLine(clipLine((f.geometry as Geometry.MultiLineString).coordinates, k1, k2, 0), k3, k4, 1)
            )))
        }
        is Geometry.Polygon -> {
            val newFeature = Feature(geometry = Geometry.Polygon(
                    coordinates = clipPolygon(clipPolygon((f.geometry as Geometry.Polygon).coordinates, k1, k2, 0), k3, k4, 1)),
                    properties = f.properties)
            if ((newFeature.geometry as Geometry.Polygon).coordinates.isEmpty() || (newFeature.geometry as Geometry.Polygon).coordinates[0].isEmpty()) {
                emptyList()
            } else {
                listOf(newFeature)
            }
        }
        is Geometry.MultiPolygon -> {
            val newFeature = Feature(geometry = Geometry.MultiPolygon(
                    coordinates = (f.geometry as Geometry.MultiPolygon).coordinates.map {
                        clipPolygon(clipPolygon(it, k1, k2, 0), k3, k4, 1)
                    }
            ))
            if ((newFeature.geometry as Geometry.MultiPolygon).coordinates.isEmpty() || (newFeature.geometry as Geometry.MultiPolygon).coordinates[0].isEmpty()) {
                emptyList()
            } else {
                listOf(newFeature)
            }
        }
        else -> listOf(f)
    }

    private fun filterPoints(coordinates: List<List<Double>>, scaleK1: Double, scaleK2: Double, scaleK3: Double, scaleK4: Double) =
            coordinates.filter {
                !(it[0] > scaleK2 || it[0] < scaleK1 || it[1] > scaleK4 || it[1] < scaleK3)
            }

    private fun clipPolygon(g: List<List<List<Double>>>, k1: Double, k2: Double, axis: Int): List<List<List<Double>>> {
        val polygon = mutableListOf<List<List<Double>>>()
        g.forEach { ring ->

            val slice = mutableListOf<List<Double>>()
            end@ for (i in ring.indices) {
                if (i >= ring.size - 1) {
                    break@end
                }
                if (ring[i][axis] < k1) {
                    if (ring[i + 1][axis] > k2) {
                        slice.addAll(listOf(intersect(ring[i], ring[i + 1], k1, axis), intersect(ring[i], ring[i + 1], k2, axis)))
                        // ---|-----|-->
                    } else if (ring[i + 1][axis] >= k1) {
                        slice.add(intersect(ring[i], ring[i + 1], k1, axis))
                        // ---|-->  |
                    }

                } else if (ring[i][axis] > k2) {
                    if (ring[i + 1][axis] < k1) {
                        slice.addAll(listOf(intersect(ring[i], ring[i + 1], k2, axis), intersect(ring[i], ring[i + 1], k1, axis)))
                        // <--|-----|---
                    } else if (ring[i + 1][axis] <= k2) {
                        slice.add(intersect(ring[i], ring[i + 1], k2, axis))
                        // |  <--|---
                    }
                } else {
                    slice.add(ring[i])
                    if (ring[i + 1][axis] < k1) {
                        slice.add(intersect(ring[i], ring[i + 1], k1, axis))
                        // <--|---  |
                    } else if (ring[i + 1][axis] > k2) {
                        slice.add(intersect(ring[i], ring[i + 1], k2, axis))
                        // |  ---|-->
                    }
                    // | --> |
                }
            }

            //TODO("investigate why and what a correct behaviour would be")
            if (ring.isNotEmpty()) {
                val a = ring.last()
                if (a[axis] in k1..k2) slice.add(a)
                if (slice.isNotEmpty() && (slice[0][0] != slice.last()[0] || slice[0][1] != slice.last()[1])) {
                    slice.add(slice[0])
                }
                polygon.add(slice)
            }
        }

        return polygon
    }

    private fun clipLine(g: List<List<List<Double>>>, k1: Double, k2: Double, axis: Int): List<List<List<Double>>> {
        val lines = mutableListOf<List<List<Double>>>()
        g.forEach { line ->
            val slice = mutableListOf<List<Double>>()
            end@ for (i in line.indices) {
                if (i >= line.size - 1) {
                    break@end
                }
                if (line[i][axis] < k1) {
                    if (line[i + 1][axis] > k2) {
                        slice.addAll(listOf(intersect(line[i], line[i + 1], k1, axis), intersect(line[i], line[i + 1], k2, axis)))
                        // ---|-----|-->
                    } else if (line[i + 1][axis] >= k1) {
                        slice.add(intersect(line[i], line[i + 1], k1, axis))
                        // ---|-->  |
                    }

                } else if (line[i][axis] > k2) {
                    if (line[i + 1][axis] < k1) {
                        slice.addAll(listOf(intersect(line[i], line[i + 1], k2, axis), intersect(line[i], line[i + 1], k1, axis)))
                        // <--|-----|---
                    } else if (line[i + 1][axis] <= k2) {
                        slice.add(intersect(line[i], line[i + 1], k2, axis))
                        // |  <--|---
                    }
                } else {
                    slice.add(line[i])
                    when {
                        line[i + 1][axis] < k1 -> slice.add(intersect(line[i], line[i + 1], k1, axis))
                        // <--|---  |
                        line[i + 1][axis] > k2 -> slice.add(intersect(line[i], line[i + 1], k2, axis))
                        // |  ---|-->
                        i >= line.size - 2 -> slice.add(line[i+1])
                        // | --> |
                    }
                }
            }

            lines.add(slice)
        }

        return lines
    }

    private fun intersect(a: List<Double>, b: List<Double>, clip: Double, axis: Int): List<Double> =
            when (axis) {
                0 -> listOf(clip, (clip - a[0]) * (b[1] - a[1]) / (b[0] - a[0]) + a[1])
                else -> listOf((clip - a[1]) * (b[0] - a[0]) / (b[1] - a[1]) + a[0], clip)
            }
}
