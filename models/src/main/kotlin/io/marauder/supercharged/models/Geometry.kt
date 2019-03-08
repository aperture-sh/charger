package io.marauder.supercharged.models

import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.json.JSON
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTreeMapper
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory

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
     * Converts the tank geometry model to JTS
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
     * Converts the tank geometry model to Well-Known-Text (WKT)
     * @return Well-Known-Text (WKT)
     */
    fun toWKT() = toJTS().toText()

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

