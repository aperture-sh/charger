import io.marauder.supercharged.Encoder
import io.marauder.supercharged.models.Feature
import io.marauder.supercharged.models.GeoJSON
import io.marauder.supercharged.models.Geometry
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.parse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import vector_tile.VectorTile

/**
 * Tests for decoding self-encoded features
 */
@ImplicitReflectionSerializer
class DecoderTest {

    private val encoder = Encoder()

    private fun readFeature(type: String, fileName: String) =
            JSON.plain.parse<Feature>(javaClass.getResource("fixtures/$type/$fileName.json").readText())

    private fun readFC(type: String, fileName: String) =
            JSON.plain.parse<GeoJSON>(javaClass.getResource("fixtures/$type/$fileName.json").readText())

    private fun encode(type: String, fileName: String) =
            encoder.encode(listOf(readFeature(type, fileName)), "test")

    private fun decode(tile: VectorTile.Tile) =
            encoder.decode(tile)

    @Test
    fun decodePoint() {
        val feature = readFeature("geometries", "point")
        val tile = encode("geometries", "point")
        val decoded = decode(tile)
        assertThat(feature.id).isEqualTo(decoded[0].id)

        (feature.geometry as Geometry.Point).coordinates.forEachIndexed { i, p ->
            assertThat(p).isEqualTo((decoded[0].geometry as Geometry.Point).coordinates[i])
        }
    }

    @Test
    fun decodeMultiPoint() {
        val feature = readFeature("geometries", "multi_point")
        val tile = encode("geometries", "multi_point")
        val decoded = decode(tile)
        assertThat(decoded[0].id).isEqualTo("0")

        (feature.geometry as Geometry.MultiPoint).coordinates.forEachIndexed { i, p ->
            assertThat(p[0]).isEqualTo((decoded[0].geometry as Geometry.MultiPoint).coordinates[i][0])
            assertThat(p[1]).isEqualTo((decoded[0].geometry as Geometry.MultiPoint).coordinates[i][1])
        }
    }

    @Test
    fun decodeLineString() {
        val feature = readFeature("geometries", "linestring")
        val tile = encode("geometries", "linestring")
        val decoded = decode(tile)

        (feature.geometry as Geometry.LineString).coordinates.forEachIndexed { i, p ->
            assertThat(p[0]).isEqualTo((decoded[0].geometry as Geometry.LineString).coordinates[i][0])
            assertThat(p[1]).isEqualTo((decoded[0].geometry as Geometry.LineString).coordinates[i][1])
        }
    }

    @Test
    fun decodeMultiLineString() {
        val feature = readFeature("geometries", "multi_linestring")
        val tile = encode("geometries", "multi_linestring")
        val decoded = decode(tile)
        assertThat((decoded[0].geometry as Geometry.MultiLineString).coordinates.size == 2)

        (feature.geometry as Geometry.MultiLineString).coordinates.forEachIndexed { i, line ->
            line.forEachIndexed { j, p ->
                assertThat(p[0]).isEqualTo((decoded[0].geometry as Geometry.MultiLineString).coordinates[i][j][0])
                assertThat(p[1]).isEqualTo((decoded[0].geometry as Geometry.MultiLineString).coordinates[i][j][1])
            }
        }
    }

    @Test
    fun decodePolygon() {
        val feature = readFeature("geometries", "polygon_single_ring")
        val tile = encode("geometries", "polygon_single_ring")
        val decoded = decode(tile)

        (feature.geometry as Geometry.Polygon).coordinates.forEachIndexed { i, ring ->
            ring.forEachIndexed { j, p ->
                assertThat(p[0]).isEqualTo((decoded[0].geometry as Geometry.Polygon).coordinates[i][j][0])
                assertThat(p[1]).isEqualTo((decoded[0].geometry as Geometry.Polygon).coordinates[i][j][1])
            }
        }
    }

    @Test
    fun decodePolygonInnerRing() {
        val feature = readFeature("geometries", "polygon_single_inner_ring")
        val tile = encode("geometries", "polygon_single_inner_ring")
        val decoded = decode(tile)

        (feature.geometry as Geometry.Polygon).coordinates.forEachIndexed { i, ring ->
            ring.forEachIndexed { j, p ->
                assertThat(p[0]).isEqualTo((decoded[0].geometry as Geometry.Polygon).coordinates[i][j][0])
                assertThat(p[1]).isEqualTo((decoded[0].geometry as Geometry.Polygon).coordinates[i][j][1])
            }
        }
    }

    @Test
    fun decodeMultiPolygon() {
        val feature = readFeature("geometries", "multi_polygon")
        val tile = encode("geometries", "multi_polygon")
        val decoded = decode(tile)
        assertThat((decoded[0].geometry as Geometry.MultiPolygon).coordinates.size).isEqualTo(2)

        (feature.geometry as Geometry.MultiPolygon).coordinates.forEachIndexed { i, polygon ->
            polygon.forEachIndexed { k, ring ->
                ring.forEachIndexed { j, p ->
                    assertThat(p[0]).isEqualTo((decoded[0].geometry as Geometry.MultiPolygon).coordinates[i][k][j][0])
                    assertThat(p[1]).isEqualTo((decoded[0].geometry as Geometry.MultiPolygon).coordinates[i][k][j][1])
                }
            }

        }
    }

    @Test
    fun decodeSingleFeatureProperty() {
        val feature = readFeature("point_properties", "single_feature_1")
        val tile = encode("point_properties", "single_feature_1")
        val decoded = decode(tile)

        feature.properties.forEach { key, value ->
            assertThat(decoded[0].properties[key]).isEqualTo(value)
        }
    }

    @Test
    fun decodeTwoFeatureProperty() {
        val f1 = readFeature("point_properties", "single_feature_1")
        val f2 = readFeature("point_properties", "single_feature_2")
        val tile = encoder.decode(encoder.encode(listOf(f1, f2), "test"))

        tile[0].properties.forEach { key, value ->
            assertThat(f1.properties[key]).isEqualTo(value)
        }

        tile[1].properties.forEach { key, value ->
            assertThat(f2.properties[key]).isEqualTo(value)
        }
    }

    @Test
    fun decodeFeatureCollection() {
        encoder.decode(encoder.encode(readFC("fc", "ne_50m_populated_places").features, "test"))
    }

}
