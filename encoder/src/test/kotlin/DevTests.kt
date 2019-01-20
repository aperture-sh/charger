import io.marauder.supercharged.Encoder
import io.marauder.supercharged.models.*
import kotlinx.serialization.*
import kotlinx.serialization.json.JSON
import java.io.File
import java.io.FileReader

object DevTests {

    @ImplicitReflectionSerializer
    @JvmStatic
    fun main(args: Array<String>) {

//        val json = FileReader("/Users/fzouhar/cloud-ext/vt-select-algo/data/canada.geojson").readText()
//        val json = FileReader("resources/fixtures/test1.json").readText()
        val json = FileReader("/Users/fzouhar/Downloads/da.json").readText()
//        val json = FileReader("/Users/fzouhar/git/geotoolbox/geo-csv-to-geojson/data/500-000.csv.json").readText()


        val feature = GeoJSON(
                type = GeoJsonType.FeatureCollection,
                features = listOf(
                        Feature(
                                type = GeoJsonType.Feature,
                                geometry = Geometry.Polygon(
                                        type = GeometryType.Polygon,
                                        coordinates = listOf(
                                                listOf(
                                                        listOf(10.0,10.0), listOf(10.0,20.0), listOf(20.0,20.0), listOf(20.0,10.0), listOf(10.0,10.0)
                                                )
                                        )
                                ),
                                properties = mapOf("A" to Value.StringValue("B"), "N" to Value.IntValue(3), "tester-2" to Value.StringValue("[3,4.3,2,4]"), "C" to Value.StringValue("B"))
                        ),
                        Feature(
                                type = GeoJsonType.Feature,
                                geometry = Geometry.Polygon(
                                        type = GeometryType.Polygon,
                                        coordinates = listOf(
                                                listOf(
                                                        listOf(10.0,10.0), listOf(10.0,20.0), listOf(20.0,20.0), listOf(20.0,10.0), listOf(10.0,10.0)
                                                )
                                        )
                                ),
                                properties = mapOf("A" to Value.StringValue("B2"), "N" to Value.IntValue(32), "tester-2" to Value.StringValue("[3,4.3,2,4]"), "C" to Value.StringValue("B2"))
                        )
                )
        )

        val f = Feature(
                type = GeoJsonType.Feature,
                geometry = Geometry.Polygon(
                        type = GeometryType.Polygon,
                        coordinates = listOf(
                                listOf(
                                        listOf(10.0, 10.0), listOf(10.0, 20.0), listOf(20.0, 20.0), listOf(20.0, 10.0), listOf(10.0, 10.0)
                                )
                        )
                ),
                properties = mapOf("A" to Value.StringValue("B"), "N" to Value.IntValue(3), "C" to Value.IntValue(2))
        )
//        val layer = ProtobufLayer("test", listOf(feature), listOf("test"), listOf(ProtobufValue(string_value = "testA")))
//        val tile = ProtobufTile(listOf(layer))

//        File("resources/fixtures/tile_data_2").writeBytes(ProtoBuf.plain.dump(tile))

//        ProtoBuf.plain.load<ProtobufTile>(File("resources/fixtures/tile_data_1").readBytes())
//        val test2 = ProtoBuf.plain.load<ProtobufTile>(File("resources/fixtures/tile_data_2").readBytes())
//        println(test2)


        val test = JSON.plain.parse<GeoJSON>(json)
        println(test)
        println(JSON.indented.stringify(feature))

        val eng = Encoder()
        val t = eng.encode(feature.features, "test")

        println(t)
        File("resources/fixtures/tile_data_2").writeBytes(t.toByteArray())

        eng.deserialize(File("resources/fixtures/tile_data_2").readBytes())
//        ProtoBuf.dump(eng.encode(test.features, "test"))

//        val builder = vector_tile.VectorTile.Tile.parseFrom(ProtoBuf.dump(eng.encode(test.features, "test")))

//        println(builder)



    }
}