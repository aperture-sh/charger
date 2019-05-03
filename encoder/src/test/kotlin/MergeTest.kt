import io.marauder.charged.Encoder
import io.marauder.charged.models.Feature
import io.marauder.charged.models.GeoJSON
import io.marauder.charged.models.Value
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.parse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.system.measureTimeMillis

@ImplicitReflectionSerializer
class MergeTest {
    private val encoder = Encoder()

    private fun readFeature(type: String, fileName: String) =
            JSON.plain.parse<Feature>(javaClass.getResource("fixtures/$type/$fileName.json").readText())

    private fun readFC(type: String, fileName: String) =
            JSON.plain.parse<GeoJSON>(javaClass.getResource("fixtures/$type/$fileName.json").readText())

    @Test
    fun mergePropertyPoints() {
        val f1 = readFeature("point_properties", "single_feature_1")
        val f2 = readFeature("point_properties", "single_feature_2")
        val tile = encoder.merge(
                encoder.encode(listOf(f1), "test"),
                encoder.encode(listOf(f2), "test")
        )
        val decoded = encoder.decode(tile)
        decoded[0].properties.forEach { key, value ->
            assertThat(f1.properties[key]).isEqualTo(value)
        }

        decoded[1].properties.forEach { key, value ->
            assertThat(f2.properties[key]).isEqualTo(value)
        }
    }

    @Test
    fun injectPropertyPoints() {
        val f1 = readFeature("point_properties", "single_feature_1")
        val f2 = readFeature("point_properties", "single_feature_2")
        val t1 = encoder.encode(listOf(f1), "test")
        val layer1 = t1.getLayers(0)

        val keyList = layer1.keysList.mapIndexed { i, s -> s to i }.toMap().toMutableMap()
        val l= layer1.valuesList.mapIndexed { i, v ->
            when {
                v.hasDoubleValue() -> Value.DoubleValue(v.doubleValue) to i
                v.hasIntValue() ->  Value.IntValue(v.intValue) to i
                else -> Value.StringValue(v.stringValue) to i
            }
        }.toMap().toMutableMap()

        val t2 = encoder.encode(listOf(f2), "test", keyList, l, layer1.featuresList)
        val decoded = encoder.decode(t2)
        decoded[0].properties.forEach { key, value ->
            assertThat(f1.properties[key]).isEqualTo(value)
        }

        decoded[1].properties.forEach { key, value ->
            assertThat(f2.properties[key]).isEqualTo(value)
        }
    }



    @Test
    fun mergeStressTest() {
        val time = measureTimeMillis {
            val tile = encoder.merge(
                    encoder.encode(readFC("fc", "ne_50m_populated_places").features, "test"),
                    encoder.encode(readFC("fc", "ne_50m_populated_places").features, "test")
            )
        }
        assertThat(time <= 10000)
    }


}