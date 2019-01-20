package io.marauder.supercharged.models

import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Representation of a [Feature]
 * @property id An optional unique identifier
 * @property type A MUST member of the feature always set to [GeoJsonType.Feature]
 * @property geometry The actual geometry inside the [Feature]
 * @property properties A key-value store containing feature data. A [Value] can be Int, Double or String.
 * @property bbox The bounding box around the feature (for internal usage, will not be serialized)
 */
@Serializable
data class Feature(
        @Optional val id: String = "",
        val type: GeoJsonType = GeoJsonType.Feature,
        @Serializable(with=GeometrySerializer::class) val geometry: Geometry,
        val properties: Map<String, Value> = emptyMap(),
        @Optional @Transient val bbox: MutableList<Double> = mutableListOf(Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE, Double.MIN_VALUE)
)