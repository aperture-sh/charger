package io.marauder.charged.models

import kotlinx.serialization.*
import kotlinx.serialization.internal.DoubleSerializer
import kotlinx.serialization.internal.LongSerializer
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.json.JsonElement

/**
 * Representation of a [Value] in a [Feature]'s key-value store
 */
@Serializable(with=ValueSerializer::class)
sealed class Value {
    /**
     * A String [Value]
     * @property value The actual [String] value.
     */
    @Serializable
    data class StringValue(val value: String) : Value()
    /**
     * A Double [Value]
     * @property value The actual [Double] value.
     */
    @Serializable
    data class DoubleValue(val value: Double) : Value()
    /**
     * A String [Value]
     * @property value The actual [Long] value.
     */
    @Serializable
    data class IntValue(val value: Long) : Value()
}

/**
 * A custom serializer for [Value] in a [Feature]'s key-value store
 */
object ValueSerializer: KSerializer<Value> {
    override val descriptor: SerialDescriptor = SerialClassDescImpl("Map")

    override fun deserialize(input: Decoder): Value {
        val jsonReader = input as? JSON.JsonInput
                ?: throw SerializationException("This class can be loaded only by JSON")
        val tree = jsonReader.readAsTree() as? JsonElement
                ?: throw SerializationException("Expected JSON object")

        val value = tree.primitive.longOrNull ?: tree.primitive.doubleOrNull ?: tree.primitive.content
        return when (value) {
            is Long -> Value.IntValue(value)
            is Double -> Value.DoubleValue(value)
            is String -> Value.StringValue(value)
            else -> Value.StringValue(value.toString())
        }

    }

    @ImplicitReflectionSerializer
    override fun serialize(output: Encoder, obj: Value) {
        when (obj) {
            is Value.DoubleValue -> DoubleSerializer.serialize(output, obj.value)
            is Value.IntValue -> LongSerializer.serialize(output, obj.value)
            is Value.StringValue -> StringSerializer.serialize(output, obj.value)
        }
    }
}