import io.marauder.supercharged.Encoder
import io.marauder.supercharged.models.GeoJSON
import io.marauder.supercharged.models.Geometry
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.parse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Integration tests against major front-end/back-end decoder/encoder
 */
@ImplicitReflectionSerializer
class IntegrationTest {

    private val encoder = Encoder()

    private fun readFile(fileName: String) =
        javaClass.getResource("fixtures/pbf/$fileName")

    @Test
    fun decodeOSMTile() {
        val t1 = encoder.decode(encoder.deserialize(readFile("osm_tile_14_8589_5547.mvt").readBytes()))
        val t2 = JSON.plain.parse<GeoJSON>(readFile("osm_tile_14_8589_5547.json").readText())

        t1.forEachIndexed { i1, f->
            (f.geometry as Geometry.Polygon).coordinates[0].forEachIndexed { i2, p ->
                assertThat((t2.features[i1].geometry as Geometry.Polygon).coordinates[0][i2][0]).isEqualTo(p[0])
                assertThat((t2.features[i1].geometry as Geometry.Polygon).coordinates[0][i2][1]).isEqualTo(p[1])
            }
        }
    }


}