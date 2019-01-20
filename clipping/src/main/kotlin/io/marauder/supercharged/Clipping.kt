package io.marauder.supercharged

import io.marauder.supercharged.models.Feature
import io.marauder.supercharged.models.GeoJSON
import io.marauder.supercharged.models.Geometry
import io.marauder.supercharged.models.GeometryType

class Clipping(val calcBoundingBoxes: Boolean = false) {

    fun clip(fc: GeoJSON, scale: Double, k1: Double, k2: Double, k3: Double, k4: Double) : GeoJSON {
//    println("scale: $scale, k1: $k1, k2: $k2, axis: $axis")
        val scaleK1 = k1 / scale
        val scaleK2 = k2 / scale
        val scaleK3 = k3 / scale
        val scaleK4 = k4 / scale
        val minFCx = fc.bbox[0]
        val maxFCx = fc.bbox[2 + 0]
        val minFCy = fc.bbox[1]
        val maxFCy = fc.bbox[2 + 1]

        if (minFCx >= scaleK2 || maxFCx <= scaleK1 || minFCy >= scaleK4 || maxFCy <= scaleK3) {
//        println("kil")
            return GeoJSON(features = emptyList())
        }
        return GeoJSON(features =
        fc.features.filter { f ->
            val minX = f.bbox[0]
            val maxX = f.bbox[2 + 0]
            val minY = f.bbox[1]
            val maxY = f.bbox[2 + 1]
//        condition for trivia reject
            !(minX > scaleK2 || maxX < scaleK1 || minY > scaleK4 || maxY < scaleK3)
            //TODO: reject when complete collection or complete features bboxes are out of bounds
//        true
        }.flatMap { f ->
            when (f.geometry) {
                is Geometry.Point -> listOf(f)
                is Geometry.Polygon -> {
                    val vertClipped = clipPolygon(f.geometry as Geometry.Polygon, scaleK1, scaleK2, scaleK3, scaleK4, 0)
                    listOf(Feature(geometry = clipPolygon(vertClipped, scaleK3, scaleK4, scaleK3, scaleK4, 1),
                            properties = f.properties)
                    )
                }
                is Geometry.MultiPolygon -> {
                    val vertClipped = (f.geometry as Geometry.MultiPolygon).coordinates.map { clipPolygon(Geometry.Polygon(coordinates = it), scaleK1, scaleK2, scaleK3, scaleK4, 0) }
                    listOf(Feature(geometry = Geometry.MultiPolygon(coordinates = vertClipped.map { clipPolygon(Geometry.Polygon(coordinates = it.coordinates), scaleK3, scaleK4, scaleK3, scaleK4, 1).coordinates }),
                            properties = f.properties)
                    )
                }
                else -> listOf(f)
            }

            /* if (f.geometry.coordinates.isEmpty()) {
                 listOf()
                 //TODO: accept everyhing when bbox matches all
                 //condition for trivia accept
     //        } else if (min >= scaleK1 && max <= scaleK2) {
     //            listOf(f)
             } else {
                 //TODO: adapt min/max during clipping
                 val geomX = clipGeometry(f.geometry, scaleK1, scaleK2, 0)
                 val geom = clipGeometry(geomX, scaleK3, scaleK4, 1)
                 if (geom.coordinates[0][0].isNotEmpty()) {
                     listOf(Feature(
                             f.type,
                             geom,
                             f.properties
                     )
                     )
                 } else {
                     listOf()
                 }

             }*/
        }
        )
    }

    fun clipX(fc: GeoJSON, scale: Double, k1: Double, k2: Double) {

    }

    fun clipY(fc: GeoJSON, scale: Double, k1: Double, k2: Double) {

    }


    fun clipPolygon(g: Geometry.Polygon, k1: Double, k2: Double, k3: Double, k4: Double, axis: Int): Geometry.Polygon {
        val slice = mutableListOf<List<Double>>()
        end@ for (i in g.coordinates[0].indices) {
            if (i >= g.coordinates[0].size - 1) {
                break@end
            }
            if (g.coordinates[0][i][axis] < k1) {
                if (g.coordinates[0][i + 1][axis] > k2) {
                    slice.addAll(listOf(intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k1, axis), intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k2, axis)))
                    // ---|-----|-->
                } else if (g.coordinates[0][i + 1][axis] >= k1) {
                    slice.add(intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k1, axis))
                    // ---|-->  |
                }

            } else if (g.coordinates[0][i][axis] > k2) {
                if (g.coordinates[0][i + 1][axis] < k1) {
                    slice.addAll(listOf(intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k2, axis), intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k1, axis)))
                    // <--|-----|---
                } else if (g.coordinates[0][i + 1][axis] <= k2) {
                    slice.add(intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k2, axis))
                    // |  <--|---
                }
            } else {
                slice.add(g.coordinates[0][i])
                if (g.coordinates[0][i + 1][axis] < k1) {
                    slice.add(intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k1, axis))
                    // <--|---  |
                } else if (g.coordinates[0][i + 1][axis] > k2) {
                    slice.add(intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k2, axis))
                    // |  ---|-->
                }
                // | --> |
            }
        }

        val a = g.coordinates[0].last()
        if (a[axis] in k1..k2) slice.add(a)
        if (slice.isNotEmpty() && (slice[0][0] != slice.last()[0] || slice[0][1] != slice.last()[1]) && (g.type == GeometryType.Polygon || g.type == GeometryType.MultiPolygon)) {
            slice.add(slice[0])
        }

        return Geometry.Polygon(g.type, mutableListOf(slice))
    }

/*
fun clipGeometry(g: Geometry, k1: Double, k2: Double, axis: Int): Geometry {
    val slice = mutableListOf<List<Double>>()
    end@for(i in g.coordinates[0].indices) {
        if (i >= g.coordinates[0].size - 1) {
            break@end
        }
        if (g.coordinates[0][i][axis] < k1) {
            if (g.coordinates[0][i+1][axis] > k2) {
                slice.addAll(listOf(intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k1, axis), intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k2, axis)))
                // ---|-----|-->
            } else if (g.coordinates[0][i+1][axis] >= k1) {
                slice.add(intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k1, axis))
                // ---|-->  |
            }

        } else if (g.coordinates[0][i][axis] > k2) {
            if (g.coordinates[0][i+1][axis] < k1) {
                slice.addAll(listOf(intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k2, axis), intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k1, axis)))
                // <--|-----|---
            } else if (g.coordinates[0][i+1][axis] <= k2) {
                slice.add(intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k2, axis))
                // |  <--|---
            }
        } else {
            slice.add(g.coordinates[0][i])
            if (g.coordinates[0][i+1][axis] < k1) {
                slice.add(intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k1, axis))
                // <--|---  |
            } else if (g.coordinates[0][i+1][axis] > k2) {
                slice.add(intersect(g.coordinates[0][i], g.coordinates[0][i + 1], k2, axis))
                // |  ---|-->
            }
            // | --> |
        }

    }


    val a = g.coordinates[0].last()
    if (a[axis] in k1..k2) slice.add(a)
    if (slice.isNotEmpty() && (slice[0][0] != slice.last()[0] || slice[0][1] != slice.last()[1]) && (g.type == "Polygon" || g.type == "MultiPolygon")) {
        slice.add(slice[0])
    }
    //TODO: somehow linear rings with < 4 points are created
    if (slice.size < 4) {
        return Geometry(g.type, mutableListOf(listOf(emptyList())))
    }

    return Geometry(g.type, mutableListOf(slice))
}
*/

    fun intersect(a: List<Double>, b: List<Double>, clip: Double, axis: Int): List<Double> =
            when (axis) {
                0 -> listOf(clip, (clip - a[0]) * (b[1] - a[1]) / (b[0] - a[0]) + a[1])
                else -> listOf((clip - a[1]) * (b[0] - a[0]) / (b[1] - a[1]) + a[0], clip)
            }

    fun fcOutOfBounds(fc: GeoJSON, scale: Double, k1: Double, k2: Double, axis: Int) : Int {
        val scaleK1 = k1 / scale
        val scaleK2 = k2 / scale
        val minFC = fc.bbox[axis]
        val maxFC = fc.bbox[2 + axis]
        if (minFC >= scaleK2) return 1
        if (maxFC <= scaleK1) return 2
        return 0
    }

}

