package io.marauder.supercharged.models

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable


/**
 * Types are a MUST string member of every GeoJSON Object
 * (mostly set by default in data Classes
 */
enum class GeoJsonType {
    Point, LineString, Polygon, MultiPoint, MultiLineString, MultiPolygon, GeometryCollection, Feature, FeatureCollection
}

/**
 * A representation of a GeoJSON object being a [type] of [GeoJsonType]
 * @property features a list of [Feature]
 * @property bbox The bounding box around the feature (for internal usage, will not be serialized)
 *
 * TODO: only feature collections are implemented or rename somehow
 */
@Serializable
data class GeoJSON(
        val type: GeoJsonType = GeoJsonType.FeatureCollection,
        val features: List<Feature>,
        @Optional @Transient val bbox: MutableList<Double> = mutableListOf(Double.MAX_VALUE, Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE)
)