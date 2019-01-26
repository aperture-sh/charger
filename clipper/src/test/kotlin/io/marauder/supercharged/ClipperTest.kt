package io.marauder.supercharged

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
        val clipped = clipper.clip(GeoJSON(features = listOf(feature)), 0.0, 0.0, 0.0, 500.0, 500.0)
        clipped.features.forEach { f ->
            assertThat((f.geometry as Geometry.Point).coordinates[0]).isEqualTo((feature.geometry as Geometry.Point).coordinates[0])
            assertThat((f.geometry as Geometry.Point).coordinates[1]).isEqualTo((feature.geometry as Geometry.Point).coordinates[1])
        }
    }

    @Test
    fun clipLine() {
        val feature = readFeature("geometries", "linestring")
        val clipped = clipper.clip(GeoJSON(features = listOf(feature)), 0.0, 600.0, 600.0, 1400.0, 1400.0)
        clipped.features.forEach { f ->
            assertThat((f.geometry as Geometry.LineString).coordinates[0][0]).isEqualTo(600)
            assertThat((f.geometry as Geometry.LineString).coordinates[0][1]).isEqualTo(600)
            assertThat((f.geometry as Geometry.LineString).coordinates[1][0]).isEqualTo((feature.geometry as Geometry.LineString).coordinates[1][0])
            assertThat((f.geometry as Geometry.LineString).coordinates[1][1]).isEqualTo((feature.geometry as Geometry.LineString).coordinates[1][1])
            assertThat((f.geometry as Geometry.LineString).coordinates[2][0]).isEqualTo(1400)
            assertThat((f.geometry as Geometry.LineString).coordinates[2][1]).isEqualTo(1400)
        }
    }
}