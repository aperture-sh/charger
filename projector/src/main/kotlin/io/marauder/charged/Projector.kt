package io.marauder.charged

import io.marauder.charged.models.*

/**
 * Projector provides vector tile specific projection and transformation functions
 * @property extend tile pixel resolution
 */
class Projector(val extend: Int = 4096) {

    /**
     * Project features into CRS 0 to 1
     * @param featureCollection Feature Collection to project
     */
    fun projectFeatures(featureCollection: GeoJSON): GeoJSON =
            GeoJSON(featureCollection.type, featureCollection.features.map { f -> projectFeature(f) })

    /**
     * Project feature into CRS 0 to 1
     * @param f Feature to project
     */
    fun projectFeature(f: Feature): Feature {
        val geometry = when(f.geometry) {
            is Geometry.Point -> Geometry.Point(GeometryType.Point, projectPoint((f.geometry as Geometry.Point).coordinates))
            is Geometry.MultiPoint -> Geometry.MultiPoint(coordinates = (f.geometry as Geometry.MultiPoint).let { point ->
                point.coordinates.map { p-> projectPoint(p) }
            })
            is Geometry.LineString -> Geometry.LineString(coordinates = (f.geometry as Geometry.LineString).let {
                it.coordinates.map { p -> projectPoint(p) }
            })
            is Geometry.MultiLineString -> Geometry.MultiLineString(coordinates = (f.geometry as Geometry.MultiLineString).let {
                it.coordinates.map { line ->
                    line.map { p -> projectPoint(p) }
                }
            })
            is Geometry.Polygon -> Geometry.Polygon(coordinates = (f.geometry as Geometry.Polygon).let {
                it.coordinates.map { ring ->
                    ring.map { p -> projectPoint(p) }
                }
            })
            is Geometry.MultiPolygon -> Geometry.MultiPolygon(coordinates = (f.geometry as Geometry.MultiPolygon).let {
                it.coordinates.map { polygon ->
                    polygon.map { ring ->
                        ring.map { p -> projectPoint(p) }
                    }
                }
            })
        }
        return Feature (
                properties = f.properties,
                geometry = geometry,
                id = f.id
        )
    }

    /**
     * Project point into CRS 0 to 1
     * @param p Point to project
     */
    fun projectPoint(p: List<Double>): List<Double> {
        val sin = Math.sin(p[1] * Math.PI / 180)
        val x = p[0] / 360 + 0.5
        var y = (0.5 - 0.25 * Math.log((1 + sin) / (1 - sin)) / Math.PI)

        y = when {
            y < 0 -> 0.0
            y > 1 -> 1.0
            else -> y
        }
        return listOf(x, y)
    }

    /**
     * Calculate bounding box for feature
     */
    fun calcBbox(f: Feature) = calcBbox(foldCoordinates(f), f.bbox)

    /**
     * Calculate Bounding box for list of points
     */
    fun calcBbox(points: List<List<Double>>, bbox: MutableList<Double>) {
        points.forEach {p ->
            bbox[0] = Math.min(p[0], bbox[0])
            bbox[2] = Math.max(p[0], bbox[2])
            bbox[1] = Math.min(p[1], bbox[1])
            bbox[3] = Math.max(p[1], bbox[3])
        }
    }

    /**
     * Calculate bounding box for feature collection
     */
    fun calcBbox(f: GeoJSON) {
        f.features.forEach {
            calcBbox(it)
            f.bbox[0] = Math.min(it.bbox[0], f.bbox[0])
            f.bbox[2] = Math.max(it.bbox[2], f.bbox[2])
            f.bbox[1] = Math.min(it.bbox[1], f.bbox[1])
            f.bbox[3] = Math.max(it.bbox[3], f.bbox[3])
        }
    }


    /**
     * Calculate bounding box for tile extend
     */
    fun tileBBox(z: Int, x: Int, y: Int) = listOf(tileToLon(x, z), tileToLat( y+1, z), tileToLon(x+1, z), tileToLat(y, z))

    /**
     * Extract list of points from a features geometries
     */
    fun foldCoordinates(f: Feature) : List<List<Double>> {
        return when(f.geometry) {
            is Geometry.Point -> listOf((f.geometry as Geometry.Point).coordinates)
            is Geometry.MultiPoint -> listOf((f.geometry as Geometry.MultiPoint).coordinates.fold(listOf()) { list, p ->
                list + p
            })
            is Geometry.LineString -> listOf((f.geometry as Geometry.LineString).coordinates.fold(listOf()) { list, p ->
                list + p
            })
            is Geometry.MultiLineString -> listOf((f.geometry as Geometry.MultiLineString).coordinates.fold(listOf()) { list, line ->
                list + line.fold(listOf<Double>()) { pList, p ->
                    pList + p
                }
            })
            is Geometry.Polygon -> {
                (f.geometry as Geometry.Polygon).coordinates.fold(listOf()) { list, ring ->
                    list + ring
                }
            }
            is Geometry.MultiPolygon -> {
                (f.geometry as Geometry.MultiPolygon).coordinates.fold(listOf()) { list, polygon ->
                    list + polygon.fold(listOf<List<Double>>()) { polyList, ring ->
                        polyList + ring
                    }
                }
            }
        }
    }

    /**
     * Transform coordinates into tile extend
     */
    fun transformTile(t: Tile) : Tile =
            Tile(GeoJSON(features = t.geojson.features.map {
                Feature(
                        it.id,
                        it.type,
                        transformGeometry(it.geometry, t.z, t.x, t.y),
                        it.properties
                )
            }),
                    t.z,
                    t.x,
                    t.y
            )

    /**
     * Transform coordinates into tile extend
     */
    fun transformGeometry(g: Geometry, z: Int, x: Int, y: Int) : Geometry = when(g) {
        is Geometry.Point -> Geometry.Point(coordinates = transformPoint(g.coordinates, extend, z, x, y))
        is Geometry.MultiPoint -> Geometry.MultiPoint(coordinates = g.coordinates.let { p ->
            p.map {
                transformPoint(it, extend, z, x, y)
            }
        })
        is Geometry.LineString -> Geometry.LineString(coordinates = g.coordinates.let { p ->
            p.map {
                transformPoint(it, extend, z, x, y)
            }
        })
        is Geometry.MultiLineString -> Geometry.MultiLineString(coordinates = g.coordinates.let { line ->
            line.map { p ->
                p.map {
                    transformPoint(it, extend, z, x, y)
                }
            }
        })
        is Geometry.Polygon -> Geometry.Polygon(coordinates = g.coordinates.let {
            it.map { ring ->
                ring.map { p -> transformPoint(p, extend, z, x, y) }
            }
        })
        is Geometry.MultiPolygon -> Geometry.MultiPolygon(coordinates = g.coordinates.let {
            it.map { polygon ->
                polygon.map { ring ->
                    ring.map { p -> transformPoint(p, extend, z, x, y) }
                }
            }
        })
    }

    /**
     * Transform coordinates into tile extend
     */
    fun transformPoint(p: List<Double>, extend: Int, z: Int, x: Int, y: Int) : List<Double> =
            listOf(Math.round(extend * (p[0] * z - x)).toDouble(), Math.round(extend * (p[1] * z - y)).toDouble())

    /**
     * Transform bounding box coordinates into tile extend
     */
    fun transformBBox(z: Int, x: Int, y: Int, bbox: List<Double>) : List<Double> =
                    transformPoint(projectPoint(bbox.subList(0,1)), extend, 1 shl z, x, y) +
                    transformPoint(projectPoint(bbox.subList(2,3)), extend, 1 shl z, x, y)

    /**
     * Calculate longtitude for top left tile corner
     */
    fun tileToLon(x: Int, z: Int) = x.toDouble() / Math.pow(2.0, z.toDouble()) * 360.0 - 180.0

    /**
     * Calculate latitude for top left tile corner
     */
    fun tileToLat(y: Int, z: Int) = Math.toDegrees(Math.atan(Math.sinh(Math.PI - (2.0 * Math.PI * y.toDouble()) / Math.pow(2.0, z.toDouble()))))

    /**
     * Find tile number to given WGS84 coordinate and zoom level
     */
    fun getTileNumber(lat: Double, lon: Double, zoom: Int): Triple<Int, Int, Int> {
        var xtile = Math.floor((lon + 180) / 360 * (1 shl zoom)).toInt()
        var ytile = Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 shl zoom)).toInt()
        if (xtile < 0)
            xtile = 0
        if (xtile >= 1 shl zoom)
            xtile = (1 shl zoom) - 1
        if (ytile < 0)
            ytile = 0
        if (ytile >= 1 shl zoom)
            ytile = (1 shl zoom) - 1
        return Triple(zoom, xtile, ytile)
    }
}
