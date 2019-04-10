import io.marauder.supercharged.Clipper
import io.marauder.supercharged.models.Feature
import io.marauder.supercharged.models.GeoJSON
import io.marauder.supercharged.models.Geometry
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.parse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@ImplicitReflectionSerializer
class ClipperTest {

    private val clipper = Clipper()

    private fun readFeature(type: String, fileName: String) =
            JSON.plain.parse<Feature>(javaClass.getResource("fixtures/$type/$fileName.json").readText())

    @Test
    fun clipPoint() {
        val feature = readFeature("geometries", "point")
        val clipped = clipper.clip(GeoJSON(features = listOf(feature)), 1.0, 0.0, 1500.0, 0.0, 1500.0, false)
        clipped.features.forEach { f ->
            assertThat((f.geometry as Geometry.Point).coordinates[0]).isEqualTo((feature.geometry as Geometry.Point).coordinates[0])
            assertThat((f.geometry as Geometry.Point).coordinates[1]).isEqualTo((feature.geometry as Geometry.Point).coordinates[1])
        }
    }

    @Test
    fun clipMultiPoint() {
        val feature = readFeature("geometries", "multi_point")
        val clipped = clipper.clip(GeoJSON(features = listOf(feature)), 1.0, 0.0, 1000.0, 0.0, 1000.0, false)
        val coords = (clipped.features[0].geometry as Geometry.MultiPoint).coordinates
        assertThat(coords.size).isEqualTo(1)
        assertThat(coords[0][0]).isEqualTo(512.0)
        assertThat(coords[0][1]).isEqualTo(512.0)
    }

    @Test
    fun clipLine() {
        val feature = readFeature("geometries", "linestring")
        val clipped = clipper.clip(GeoJSON(features = listOf(feature)), 1.0, 600.0, 1400.0, 600.0, 1400.0, false)
        clipped.features.forEach { f ->
            assertThat((f.geometry as Geometry.LineString).coordinates[0][0]).isEqualTo(600.0)
            assertThat((f.geometry as Geometry.LineString).coordinates[0][1]).isEqualTo(600.0)
            assertThat((f.geometry as Geometry.LineString).coordinates[1][0]).isEqualTo((feature.geometry as Geometry.LineString).coordinates[1][0])
            assertThat((f.geometry as Geometry.LineString).coordinates[1][1]).isEqualTo((feature.geometry as Geometry.LineString).coordinates[1][1])
            assertThat((f.geometry as Geometry.LineString).coordinates[2][0]).isEqualTo(1400.0)
            assertThat((f.geometry as Geometry.LineString).coordinates[2][1]).isEqualTo(1400.0)
        }
    }

    @Test
    fun clipPolygon1() {
        val feature = readFeature("geometries", "polygon_single_ring")
        val clipped = clipper.clip(GeoJSON(features = listOf(feature)), 1.0, 600.0, 1400.0, 600.0, 1400.0, false)
        clipped.features.forEach { f ->
            assertThat((f.geometry as Geometry.Polygon).coordinates[0][0][0]).isEqualTo(1400.0)
            assertThat((f.geometry as Geometry.Polygon).coordinates[0][0][1]).isEqualTo(1400.0)
            assertThat((f.geometry as Geometry.Polygon).coordinates[0][1][0]).isEqualTo(1400.0)
            assertThat((f.geometry as Geometry.Polygon).coordinates[0][1][1]).isEqualTo(600.0)
            assertThat((f.geometry as Geometry.Polygon).coordinates[0][2][0]).isEqualTo(600.0)
            assertThat((f.geometry as Geometry.Polygon).coordinates[0][2][1]).isEqualTo(600.0)
            assertThat((f.geometry as Geometry.Polygon).coordinates[0][3][0]).isEqualTo(600.0)
            assertThat((f.geometry as Geometry.Polygon).coordinates[0][3][1]).isEqualTo(1400.0)
        }
    }

    @Test
    fun clipOutsidePloygon() {
        val g = Geometry.Polygon(coordinates = listOf(listOf(listOf(0.0,0.0),listOf(0.0,0.0),listOf(0.0,0.0),listOf(0.0,0.0),listOf(0.0,0.0))))
        val f = clipper.clip(Feature(geometry = g), 1.0, 1.0, 2.0, 1.0, 2.0, false)
        assertThat(f).isNull()
    }
}