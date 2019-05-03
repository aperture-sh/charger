import io.marauder.charged.Encoder
import io.marauder.charged.models.Feature
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.parse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import vector_tile.VectorTile

/**
 * Tests for encoding only.
 * - empty geometries
 * - simple geometries
 * - property map to tag set check
 */
@ImplicitReflectionSerializer
class EncoderTest {

    private val encoder = Encoder()

    private fun readFeature(type: String, fileName: String) =
        JSON.plain.parse<Feature>(javaClass.getResource("fixtures/$type/$fileName.json").readText())

    private fun assertEmptyTile(tile: VectorTile.Tile) = assertThat(tile.getLayers(0).getFeatures(0).geometryCount).isEqualTo(0)

    @Test
    fun encodeEmptyPoint() {
        assertEmptyTile(encoder.encode(listOf(readFeature("empty_geometries", "point")), "test"))
    }

    @Test
    fun encodeEmptyMultiPoint() {
        assertEmptyTile(encoder.encode(listOf(readFeature("empty_geometries", "multi_point")), "test"))
    }

    @Test
    fun encodeEmptyLineString() {
        assertEmptyTile(encoder.encode(listOf(readFeature("empty_geometries", "linestring")), "test"))
    }

    @Test
    fun encodeEmptyMultiLineString() {
        assertEmptyTile(encoder.encode(listOf(readFeature("empty_geometries", "multi_linestring")), "test"))
    }

    @Test
    fun encodeEmptyPolygon() {
        assertEmptyTile(encoder.encode(listOf(readFeature("empty_geometries", "polygon")), "test"))
    }
    
    @Test
    fun encodeEmptyMultiPolygon() {
        assertEmptyTile(encoder.encode(listOf(readFeature("empty_geometries", "multi_polygon")), "test"))
    }

    @Test
    fun encodePoint() {
        val tile = encoder.encode(listOf(readFeature("geometries", "point")), "test")
        val f = tile.getLayers(0).getFeatures(0)
        //first command is MOVE_TO
        assertThat(f.getGeometry(0)).isEqualTo(9)
        assertThat(f.getGeometry(1)).isEqualTo(2048)
        assertThat(f.getGeometry(2)).isEqualTo(2048)
        //no second command
        assertThat(f.geometryCount == 3)
    }

    @Test
    fun encodeMultiPoint() {
        val tile = encoder.encode(listOf(readFeature("geometries", "multi_point")), "test")
        val f = tile.getLayers(0).getFeatures(0)
        //2nd command is MOVE_TO
        assertThat(f.getGeometry(3)).isEqualTo(9)
        assertThat(f.getGeometry(4)).isEqualTo(2048)
        assertThat(f.getGeometry(5)).isEqualTo(2048)
        //no 3rd command
        assertThat(f.geometryCount).isEqualTo(6)
    }

    @Test
    fun encodeLineString() {
        val tile = encoder.encode(listOf(readFeature("geometries", "linestring")), "test")
        val f = tile.getLayers(0).getFeatures(0)
        assertThat(f.getGeometry(0)).isEqualTo(9)
        assertThat(f.getGeometry(1)).isEqualTo(1024)
        assertThat(f.getGeometry(2)).isEqualTo(1024)

        assertThat(f.getGeometry(3)).isEqualTo(18)
        assertThat(f.getGeometry(4)).isEqualTo(1024)
        assertThat(f.getGeometry(5)).isEqualTo(1024)

        assertThat(f.getGeometry(6)).isEqualTo(1024)
        assertThat(f.getGeometry(7)).isEqualTo(1024)
    }

    @Test
    fun encodeMultiLineString() {
        val tile = encoder.encode(listOf(readFeature("geometries", "multi_linestring")), "test")
        val f = tile.getLayers(0).getFeatures(0)
        assertThat(f.getGeometry(8)).isEqualTo(9)
        assertThat(f.getGeometry(9)).isEqualTo(2047)
        assertThat(f.getGeometry(10)).isEqualTo(0)
        assertThat(f.getGeometry(11)).isEqualTo(18)
    }

    @Test
    fun encodePolygonSingleRing() {
        val tile = encoder.encode(listOf(readFeature("geometries", "polygon_single_ring")), "test")
        val f = tile.getLayers(0).getFeatures(0)
        assertThat(f.geometryList.filter { it == 34 }.size).isEqualTo(1)
        assertThat(f.geometryList.filter { it == 9 }.size).isEqualTo(1)
        assertThat(f.geometryList.filter { it == 15 }.size).isEqualTo(1)
    }

    @Test
    fun encodePolygonSingleInnerRing() {
        val tile = encoder.encode(listOf(readFeature("geometries", "polygon_single_inner_ring")), "test")
        val f = tile.getLayers(0).getFeatures(0)
        assertThat(f.geometryList.filter { it == 34 }.size).isEqualTo(2)
        assertThat(f.geometryList.filter { it == 9 }.size).isEqualTo(2)
        assertThat(f.geometryList.filter { it == 15 }.size).isEqualTo(2)


    }

    @Test
    fun encodeMultiPolygon() {
        val tile = encoder.encode(listOf(readFeature("geometries", "multi_polygon")), "test")
        val f = tile.getLayers(0).getFeatures(0)
        assertThat(f.geometryList.filter { it == 34 }.size).isEqualTo(2)
        assertThat(f.geometryList.filter { it == 9 }.size).isEqualTo(2)
        assertThat(f.geometryList.filter { it == 15 }.size).isEqualTo(2)
    }

    @Test
    fun encodeSingleFeatureProperty() {
        encoder.encode(listOf(readFeature("point_properties", "single_feature_1")), "test")
    }

    @Test
    fun encodeTwoFeatureProperty() {
        val first = readFeature("point_properties", "single_feature_1")
        val second = readFeature("point_properties", "single_feature_2")
        val tile = encoder.encode(listOf(first, second), "test")
        val layer = tile.getLayers(0)
        val f1 = layer.getFeatures(0)
        val f2 = layer.getFeatures(1)
        assertThat(f1.getTags(0)).isEqualTo(0)
        assertThat(f1.getTags(1)).isEqualTo(0)
        assertThat(f1.getTags(2)).isEqualTo(1)
        assertThat(f1.getTags(3)).isEqualTo(1)
        assertThat(f1.getTags(4)).isEqualTo(2)
        assertThat(f1.getTags(5)).isEqualTo(2)

        assertThat(f2.getTags(0)).isEqualTo(0)
        assertThat(f2.getTags(1)).isEqualTo(0)
        assertThat(f2.getTags(2)).isEqualTo(3)
        assertThat(f2.getTags(3)).isEqualTo(1)
        assertThat(f2.getTags(4)).isEqualTo(4)
        assertThat(f2.getTags(5)).isEqualTo(3)
    }
}
