package io.marauder.charged.models

import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.json.JSON
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTreeMapper
import org.locationtech.jts.geom.*
import org.locationtech.jts.io.WKTReader

/**
 * Type are a MUST string member of `Geometry` Objects
 * (all set by default in the constructor)
 */
enum class GeometryType {
    Point, LineString, Polygon, MultiPoint, MultiLineString, MultiPolygon, GeometryCollection
}

/**
 * Representation for one [Geometry]
 */
sealed class Geometry {
    /**
     * Representation for one [Point] with [type] = [GeometryType.Point]
     * @property coordinates a list containing the x and y coordinate
     */
    @Serializable
    data class Point(val type: GeometryType = GeometryType.Point, val coordinates: List<Double>) : Geometry()
    /**
     * Representation for one [MultiPoint] with [type] = [GeometryType.MultiPoint]
     * @property coordinates a list containing multiple lists containing a x and y coordinate
     */
    @Serializable
    data class MultiPoint(val type: GeometryType = GeometryType.MultiPoint, val coordinates:  List<List<Double>>) : Geometry()
    /**
     * Representation for one [LineString] with [type] = [GeometryType.LineString]
     * @property coordinates a list containing multiple lists containing a x and y coordinate
     */
    @Serializable
    data class LineString(val type: GeometryType = GeometryType.LineString, val coordinates:  List<List<Double>>) : Geometry()
    /**
     * Representation for one [MultiLineString] with [type] = [GeometryType.MultiLineString]
     * @property coordinates a list containing multiple lists containing a linestring each
     */
    @Serializable
    data class MultiLineString(val type: GeometryType = GeometryType.MultiLineString, val coordinates:  List<List<List<Double>>>) : Geometry()
    /**
     * Representation for one [Polygon] with [type] = [GeometryType.Polygon]
     * @property coordinates a list containing multiple lists containing a linear ring each (the first has to be a exterior ring)
     */
    @Serializable
    data class Polygon(val type: GeometryType = GeometryType.Polygon, val coordinates: List<List<List<Double>>>) : Geometry()
    /**
     * Representation for one [MultiPolygon] with [type] = [GeometryType.MultiPolygon]
     * @property coordinates a list containing multiple lists containing a polygon each
     */
    @Serializable
    data class MultiPolygon(val type: GeometryType = GeometryType.MultiPolygon, val coordinates: List<List<List<List<Double>>>>) : Geometry()

    /**
     * Converts the supercharged geometry object to JTS
     * @return a JTS geometry for better library integration
     */
    fun toJTS() : org.locationtech.jts.geom.Geometry {
        val gf = GeometryFactory()
        return when(this) {
            is Point -> gf.createPoint(Coordinate(this.coordinates[0], this.coordinates[1]))
            is MultiPoint -> gf.createMultiPoint(
                    this.coordinates.map {
                        gf.createPoint(Coordinate(it[0], it[1]))
                    }.toTypedArray()
            )
            is LineString -> gf.createLineString(this.coordinates.map { Coordinate(it[0], it[1]) }.toTypedArray())
            is MultiLineString -> gf.createMultiLineString(
                    this.coordinates.map { line ->
                        gf.createLineString(line.map { Coordinate(it[0], it[1]) }.toTypedArray())
                    }.toTypedArray()
            )
            is Polygon -> gf.createPolygon(
                    gf.createLinearRing(this.coordinates[0].map { Coordinate(it[0], it[1]) }.toTypedArray()),
                    this.coordinates.subList(1, this.coordinates.size).map { hole ->
                        gf.createLinearRing(hole.map { Coordinate(it[0], it[1]) }.toTypedArray())
                    }.toTypedArray()
            )
            is MultiPolygon -> gf.createMultiPolygon(
                    this.coordinates.map { polygon ->
                        gf.createPolygon(
                                gf.createLinearRing(polygon[0].map { Coordinate(it[0], it[1]) }.toTypedArray()),
                                polygon.subList(1, polygon.size).map { hole ->
                                    gf.createLinearRing(hole.map { Coordinate(it[0], it[1]) }.toTypedArray())
                                }.toTypedArray()
                        )
                    }.toTypedArray()
            )
        }
    }

    /**
     * Converts the supercharged geometry object to Well-Known-Text (WKT)
     * @return Well-Known-Text (WKT)
     */
    fun toWKT() = toJTS().toText()

    companion object {

        /**
         * Reads a JTS Geomtery and converts it into a superchagred geometry object
         * @param g A [org.locationtech.jts.geom.Geometry] to convert
         * @return A supercharged geometry object or null when unknown type
         */
        @JvmStatic
        fun fromJTS(g: org.locationtech.jts.geom.Geometry) =
            when (g) {
                is org.locationtech.jts.geom.Point -> Point(coordinates = listOf(g.x, g.y))
                is org.locationtech.jts.geom.MultiPoint -> {
                    MultiPoint(coordinates =
                        (0 until g.numPoints).map { i -> (g.getGeometryN(i) as org.locationtech.jts.geom.Point).let { listOf(it.x, it.y)} }
                    )
                }
                is org.locationtech.jts.geom.LineString -> {
                    LineString(coordinates = g.coordinateSequence.toCoordinateArray().map { listOf(it.x, it.y) })
                }
                is org.locationtech.jts.geom.MultiLineString -> {
                    MultiLineString(coordinates =
                        (0 until g.numGeometries).map { i -> g.getGeometryN(i).coordinates.map { listOf(it.x, it.y) } }
                    )
                }
                is org.locationtech.jts.geom.Polygon -> {
                    Polygon(coordinates =
                        listOf(g.exteriorRing.coordinateSequence.toCoordinateArray().map { listOf(it.x, it.y) }) +
                                (0 until g.numInteriorRing).map { i -> g.getInteriorRingN(i).coordinateSequence.toCoordinateArray().map { listOf(it.x, it.y) } }
                    )
                }
                is org.locationtech.jts.geom.MultiPolygon -> {
                    MultiPolygon(coordinates =
                        (0 until g.numGeometries).map {  i ->
                            val polygon = (g.getGeometryN(i) as org.locationtech.jts.geom.Polygon)
                            listOf(polygon.exteriorRing.coordinateSequence.toCoordinateArray().map { listOf(it.x, it.y) }) +
                                    (0 until polygon.numInteriorRing).map { j -> polygon.getInteriorRingN(j).coordinateSequence.toCoordinateArray().map { listOf(it.x, it.y) } }
                        }
                    )
                }
                else -> null
            }

        /**
         * Reads a Well-Known-Text (WKT) geometry and converts it to a supercharged geometry object
         * @param wkt WKT string
         * @return  A supercharged geometry object
         */
        @JvmStatic
        fun fromWKT(wkt: String) = fromJTS(WKTReader().read(wkt))
    }

}

/**
 * A custom serializer for [Geometry]
 * TODO: geometry collection not implemented, needed?
 */
object GeometrySerializer: KSerializer<Geometry> {
    override val descriptor: SerialDescriptor = SerialClassDescImpl("Geometry")

    override fun deserialize(input: Decoder): Geometry {
        val jsonReader = input as? JSON.JsonInput
                ?: throw SerializationException("This class can be loaded only by JSON")
        val tree = jsonReader.readAsTree() as? JsonObject
                ?: throw SerializationException("Expected JSON object")

        return when (GeometryType.valueOf(tree["type"].primitive.content)) {
            GeometryType.Point -> JsonTreeMapper().readTree(tree, Geometry.Point.serializer())
            GeometryType.MultiPoint -> JsonTreeMapper().readTree(tree, Geometry.MultiPoint.serializer())
            GeometryType.LineString -> JsonTreeMapper().readTree(tree, Geometry.LineString.serializer())
            GeometryType.MultiLineString -> JsonTreeMapper().readTree(tree, Geometry.MultiLineString.serializer())
            GeometryType.Polygon -> JsonTreeMapper().readTree(tree, Geometry.Polygon.serializer())
            GeometryType.MultiPolygon -> JsonTreeMapper().readTree(tree, Geometry.MultiPolygon.serializer())
            GeometryType.GeometryCollection -> TODO("not yet implemented")
        }
    }

    override fun serialize(output: Encoder, obj: Geometry) {
        val jsonWriter = output as? JSON.JsonOutput
                ?: throw SerializationException("This class can be saved only by JSON")

        val tree = when (obj) {
            is Geometry.Point -> JsonTreeMapper().writeTree(obj, Geometry.Point.serializer())
            is Geometry.MultiPoint -> JsonTreeMapper().writeTree(obj, Geometry.MultiPoint.serializer())
            is Geometry.LineString -> JsonTreeMapper().writeTree(obj, Geometry.LineString.serializer())
            is Geometry.MultiLineString -> JsonTreeMapper().writeTree(obj, Geometry.MultiLineString.serializer())
            is Geometry.Polygon -> JsonTreeMapper().writeTree(obj, Geometry.Polygon.serializer())
            is Geometry.MultiPolygon -> JsonTreeMapper().writeTree(obj, Geometry.MultiPolygon.serializer())
        }
        jsonWriter.writeTree(tree)
    }

}

