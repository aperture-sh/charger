package io.marauder.charged.models

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.ProtoNumberType
import kotlinx.serialization.protobuf.ProtoType
import vector_tile.VectorTile

/**
 * Vector Tile Specification for KotlinX.Serialization
 * (Could be used in future if packed fields are implemented in KotlinX)
 */
@Serializable
data class ProtobufTile(@SerialId(3) val layers: List<ProtobufLayer>)
@Serializable
data class ProtobufLayer(
        @SerialId(1) val name: String,
        @SerialId(2) val features: List<ProtobufFeature>,
        @Optional @SerialId(3) val keys: List<String> = emptyList(),
        @Optional @SerialId(4) val values: List<ProtobufValue> = emptyList(),
        @Transient @SerialId(5) @ProtoType(ProtoNumberType.DEFAULT) val extend: Int = 4096,
        @SerialId(15) @ProtoType(ProtoNumberType.DEFAULT) val version: Int = 1)
@Serializable
data class ProtobufFeature(
        @SerialId(1) @ProtoType(ProtoNumberType.DEFAULT) val id: Int = 0,
        @Optional @SerialId(2) @ProtoType(ProtoNumberType.DEFAULT) val tags: List<Int> = emptyList(),
        @Optional @SerialId(3) val type: VectorTile.Tile.GeomType = VectorTile.Tile.GeomType.UNKNOWN,
        @SerialId(4) val geometry: List<Int> = emptyList()
)
@Serializable
data class ProtobufValue(
        @Optional @SerialId(1) val string_value: String = "",
        @Optional @SerialId(2) @ProtoType(ProtoNumberType.FIXED) val float_value: Float = 0.0f,
        @Optional @SerialId(3) @ProtoType(ProtoNumberType.FIXED) val double_value: Double = 0.0,
        @Optional @SerialId(4) @ProtoType(ProtoNumberType.DEFAULT) val int_value: Int = 0,
        @Optional @SerialId(5) @ProtoType(ProtoNumberType.DEFAULT) val uint_value: Int = 0,
        @Optional @SerialId(6) @ProtoType(ProtoNumberType.SIGNED) val sint_value: Int = 0,
        @Optional @SerialId(7) val bool_value: Boolean = false
)


