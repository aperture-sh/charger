package io.marauder.supercharged

import io.marauder.supercharged.models.*
import vector_tile.VectorTile

/**
 * The Encoder is a fast and reliable Encoder and Decoder for Mapbox Vector Tiles
 * It uses specification version 2.1
 * @see <a href="https://github.com/mapbox/vector-tile-spec/tree/master/2.1">https://github.com/mapbox/vector-tile-spec/tree/master/2.1</a>
 *
 * @param extend Defines the resolution used to encode the vector tiles.
 */
class Encoder(private val extend: Int = 4096) {

     /**
     * [Int] representation for commands used to encode geometry coordinates
     */
    enum class Command(val cmd: Int) {
        MOVE_TO(1), LINE_TO(2), CLOSE_PATH(7)
    }

    /**
     * Encodes a list of [Feature] for later serialization.
     * The parameters [keys] and [values] can be used to provide tag ids to be used for encoding.
     * @param features List of [Feature] to encode
     * @param layerName The layer name the features are added to
     * @param keys A map of property keys and tag ids to use for encoding
     * @param values A map of property values and tag ids to use for encoding
     * @param inject A list containing features to be merged into new tile
     * @return A [vector_tile.VectorTile.Tile] object ready to be serialized
     */
    @JvmOverloads
    fun encode(features: List<Feature>,
               layerName: String,
               keys: MutableMap<String, Int> = mutableMapOf(),
               values: MutableMap<Value, Int> = mutableMapOf(),
               inject: List<vector_tile.VectorTile.Tile.Feature> = emptyList()
    ) : VectorTile.Tile {
        val tile = vector_tile.VectorTile.Tile.newBuilder()
        val layer = vector_tile.VectorTile.Tile.Layer.newBuilder()

        layer.version = 2
        layer.name = layerName
        layer.extent = extend

        var countKeys = keys.size
        var countValues = values.size
        values.forEach {
            val valBuilder = vector_tile.VectorTile.Tile.Value.newBuilder()
            when (it.key) {
                is Value.IntValue -> {
                        valBuilder.intValue = (it.key as Value.IntValue).value
                        layer.addValues(valBuilder.build())
                }
                is Value.DoubleValue -> {
                        valBuilder.doubleValue = (it.key as Value.DoubleValue).value
                        layer.addValues(valBuilder.build())
                }
                is Value.StringValue -> {
                        valBuilder.stringValue = (it.key as Value.StringValue).value
                        layer.addValues(valBuilder.build())
                }
            }
        }

        features.forEach { f ->
            f.properties.entries.forEach {
                if (keys.putIfAbsent(it.key, countKeys) == null) countKeys += 1
                val valBuilder = vector_tile.VectorTile.Tile.Value.newBuilder()
                when (it.value) {
                    is Value.IntValue -> {
                        if (values.putIfAbsent(it.value, countValues) == null) {
                            valBuilder.intValue = (it.value as Value.IntValue).value
                            layer.addValues(valBuilder.build())
                            countValues += 1
                        }
                    }
                    is Value.DoubleValue -> {
                        if (values.putIfAbsent(it.value, countValues) == null) {
                            valBuilder.doubleValue = (it.value as Value.DoubleValue).value
                            layer.addValues(valBuilder.build())
                            countValues += 1
                        }
                    }
                    is Value.StringValue -> {
                        if (values.putIfAbsent(it.value, countValues) == null) {
                            valBuilder.stringValue = (it.value as Value.StringValue).value
                            layer.addValues(valBuilder.build())
                            countValues += 1
                        }
                    }
                }
            }
        }

        layer.addAllKeys(keys.map { it.key })

        val encoded = features.map { f ->
            encodeFeature(f, keys, values)
        }

        layer.addAllFeatures(inject)
        layer.addAllFeatures(encoded)
        tile.addLayers(layer.build())

        return tile.build()
    }

    /**
     * Encodes a [Feature] for later serialization.
     * The parameters [keys] and [values] provide tag ids to be used for encoding.
     * @param feature [Feature] to encode
     * @param keys A map of property keys and tag ids to use for encoding
     * @param values A map of property values and tag ids to use for encoding
     * @return A [vector_tile.VectorTile.Tile.Feature] object ready to be serialized
     */
    @JvmOverloads
    fun encodeFeature(feature: Feature,
                      keys: MutableMap<String, Int> = mutableMapOf(),
                      values: MutableMap<Value, Int> = mutableMapOf()
    ) : VectorTile.Tile.Feature {
        val featureBuilder = vector_tile.VectorTile.Tile.Feature.newBuilder()
        featureBuilder.id = feature.id.toLongOrNull() ?: 0
        val tags = mutableListOf<Int>()
        feature.properties.forEach {
            tags.add(keys[it.key] ?: 0)
            val value = when (it.value) {
                is Value.IntValue -> it.value
                is Value.DoubleValue -> it.value
                else -> it.value
            }
            tags.add(values[value] ?: 0)
        }
        featureBuilder.addAllTags(tags)
        val geometry = encodeGeometry(feature.geometry)
        if (geometry.isNotEmpty()) featureBuilder.addAllGeometry(geometry)
        when (feature.geometry) {
            is Geometry.Point -> featureBuilder.type = VectorTile.Tile.GeomType.POINT
            is Geometry.MultiPoint -> featureBuilder.type = VectorTile.Tile.GeomType.POINT
            is Geometry.LineString -> featureBuilder.type = VectorTile.Tile.GeomType.LINESTRING
            is Geometry.MultiLineString -> featureBuilder.type = VectorTile.Tile.GeomType.LINESTRING
            is Geometry.Polygon -> featureBuilder.type = VectorTile.Tile.GeomType.POLYGON
            is Geometry.MultiPolygon -> featureBuilder.type = VectorTile.Tile.GeomType.POLYGON
        }
        return featureBuilder.build()
    }

    /**
     * Encodes a [Geometry] for later serialization.
     * @param geometry [Geometry] to encode
     * @return A [List] of [Int] values representing the geometry
     */
    fun encodeGeometry(geometry: Geometry) = if (geometryValid(geometry)) when (geometry) {
        is Geometry.Point -> encodePoint(geometry).first
        is Geometry.MultiPoint -> encodeMultiPoint(geometry)
        is Geometry.LineString -> encodeLineString(geometry).first
        is Geometry.MultiLineString -> encodeMultiLineString(geometry)
        is Geometry.Polygon -> encodePolygon(geometry).first
        is Geometry.MultiPolygon -> encodeMultiPolygon(geometry)
    } else emptyList()

    /**
     * Serialize one tile
     * @param tile Parsed [VectorTile.Tile]
     * @return Bytes containing an uncompressed tile in vector tile format
     */
    fun serialize(tile: VectorTile.Tile) = tile.toByteArray()

    /**
     * Decode one vector tile
     * @param tile Protobuf encoded vector tile
     * @return Decoded list of [Feature]
     */
    fun decode(tile: VectorTile.Tile) =
            tile.getLayers(0).featuresList.flatMap { f ->
                val properties = decodeProperties(f, tile.getLayers(0).keysList, tile.getLayers(0).valuesList)
                when (f.type) {
                    VectorTile.Tile.GeomType.POINT -> {
                        val coords = decodePoint(f.geometryList)

                        if (coords.size <= 1)
                            listOf(Feature(id = f.id.toString(), geometry = Geometry.Point(coordinates = coords[0].map { it.toDouble() }), properties = properties))
                        else
                            listOf(Feature(id = f.id.toString(), geometry = Geometry.MultiPoint(coordinates = coords.map { p -> p.map { it.toDouble() } }), properties = properties))
                    }
                    VectorTile.Tile.GeomType.POLYGON -> {
                        val coords = decodePolygon(f.geometryList)

                        if (coords.size <= 1)
                            listOf(Feature(id = f.id.toString(), geometry = Geometry.Polygon(coordinates = coords[0].map { ring -> ring.map { p -> p.map { it.toDouble() } } }), properties = properties))
                        else
                        listOf(Feature(id = f.id.toString(), geometry = Geometry.MultiPolygon(coordinates = coords.map { polygon -> polygon.map { ring -> ring.map { p -> p.map { it.toDouble() } } } }), properties = properties))
                    }
                    VectorTile.Tile.GeomType.LINESTRING -> {
                        val coords = decodeLineString(f.geometryList)
                        if (coords.size <= 1)
                            listOf(Feature(id = f.id.toString(), geometry = Geometry.LineString(coordinates = coords[0].map { p -> p.map { it.toDouble() } }), properties = properties))
                        else
                        listOf(Feature(id = f.id.toString(), geometry = Geometry.MultiLineString(coordinates = coords.map { line -> line.map { p -> p.map { it.toDouble() } } }), properties = properties))
                    }
                    VectorTile.Tile.GeomType.UNKNOWN -> emptyList()
                    else -> emptyList()
                }
            }


    /**
     * Deserialize one tile
     * @param tile Bytes containing an uncompressed tile in vector tile format
     * @return A [vector_tile.VectorTile.Tile] object ready to rendered, decoded, ...
     */
    fun deserialize(tile: ByteArray) : VectorTile.Tile {
        return VectorTile.Tile.parseFrom(tile)
    }

    /**
     * Merge two vector tiles
     * @param t1 Bytes containing an uncompressed tile in vector tile format to merge [t2] into
     * @param t2 Bytes containing an uncompressed tile in vector tile format to be merged into [t1]
     * @return A [vector_tile.VectorTile.Tile] object ready to rendered, decoded, ...
     */
    fun merge(t1: ByteArray, t2: ByteArray) =
        merge(vector_tile.VectorTile.Tile.parseFrom(t1), VectorTile.Tile.parseFrom(t2))

    /**
     * Merge two vector tiles
     * @param t1 Tile to merge [t2] into
     * @param t2 Tile to be merged into [t1]
     * @return A [vector_tile.VectorTile.Tile] object ready to rendered, decoded, ...
     */
    fun merge(t1: VectorTile.Tile, t2: VectorTile.Tile) : VectorTile.Tile {
        val tile = t1.toBuilder()
        val mergedLayers = mutableListOf<Int>()
        t1.layersList.forEach { layer ->
            val mergeLayer = findLayer(t2, layer.name)
            if (mergeLayer < 0) {
                tile.addLayers(layer)
            } else {
                val layer1 = layer.toBuilder()
                val layer2 = t2.getLayers(mergeLayer)
                mergedLayers.add(mergeLayer)

                val keySet = (layer1.keysList + layer2.keysList).toHashSet()
                val keyList = keySet.mapIndexed { i, s -> s to i }.toMap()
                val valueSet = (layer1.valuesList + layer2.valuesList).toHashSet()
                val valueList = valueSet.mapIndexed { i, s -> s to i }.toMap()

                val features1 = layer2.featuresList.map { f ->
                    val builder = f.toBuilder()
                    val tagSet = builder.tagsList.chunked(2).map { attr ->
                        keyList[layer2.getKeys(attr[0])] to valueList[layer2.getValues(attr[1])]
                    }.fold(listOf<Int>()) { l, tagEntry ->  l + listOf(tagEntry.first ?: -1, tagEntry.second ?: -1) }
                    builder.clearTags()
                    builder.addAllTags(tagSet)
                    builder.build()
                }

                val features = layer1.featuresList.map { f ->
                    val builder = f.toBuilder()
                    val tagSet = builder.tagsList.chunked(2).map { attr ->
                        keyList[layer1.getKeys(attr[0])] to valueList[layer1.getValues(attr[1])]
                    }.fold(listOf<Int>()) { l, tagEntry ->  l + listOf(tagEntry.first ?: -1, tagEntry.second ?: -1) }
                    builder.clearTags()
                    builder.addAllTags(tagSet)
                    builder.build()
                } + features1

                layer1.clearKeys()
                layer1.addAllKeys(keySet)
                layer1.clearValues()
                layer1.addAllValues(valueSet)

                layer1.clearFeatures()
                tile.setLayers(mergeLayer, layer1.addAllFeatures(features).build())
            }
        }
        t2.layersList.forEachIndexed { i, layer ->
            if (!mergedLayers.contains(i)) tile.addLayers(layer)
        }

        return tile.build()
    }

    private fun geometryValid(geometry: Geometry) = when (geometry) {
        is Geometry.Point -> geometry.coordinates.isNotEmpty()
        is Geometry.MultiPoint -> geometry.coordinates.isNotEmpty() && geometry.coordinates[0].isNotEmpty()
        is Geometry.LineString -> geometry.coordinates.isNotEmpty() && geometry.coordinates[0].isNotEmpty()
        is Geometry.MultiLineString -> geometry.coordinates.isNotEmpty() && geometry.coordinates[0].isNotEmpty() && geometry.coordinates[0][0].isNotEmpty()
        is Geometry.Polygon -> geometry.coordinates.isNotEmpty() && geometry.coordinates[0].isNotEmpty() && geometry.coordinates[0][0].isNotEmpty()
        is Geometry.MultiPolygon -> geometry.coordinates.isNotEmpty() && geometry.coordinates[0].isNotEmpty() && geometry.coordinates[0][0].isNotEmpty() && geometry.coordinates[0][0][0].isNotEmpty()
    }

    private fun encodePoint(point: Geometry.Point, x: Int = 0, y: Int = 0) =
        Triple(listOf(commandEncode(Command.MOVE_TO.cmd, 1),
                zigZagEncode(point.coordinates[0].toInt() - x),
                zigZagEncode(point.coordinates[1].toInt() - y)),
                point.coordinates[0].toInt(),
                point.coordinates[1].toInt())

    private fun encodeMultiPoint(multiPoint: Geometry.MultiPoint) =
        multiPoint.coordinates.fold(listOf(Triple(emptyList<Int>(), 0, 0))) { l, p ->
            l + encodePoint(Geometry.Point(coordinates = p), l.last().second, l.last().third)
        }.fold(listOf<Int>()) { l, point ->
            l + point.first
        }


    private fun encodeLineString(lineString: Geometry.LineString, x: Int = 0, y: Int = 0) : Triple<MutableList<Int>, Int, Int> {
        val commands = mutableListOf<Int>()
        commands.add(commandEncode(Command.MOVE_TO.cmd, 1))
        commands.add(zigZagEncode(lineString.coordinates[0][0].toInt() - x))
        commands.add(zigZagEncode(lineString.coordinates[0][1].toInt() - y))
        var newX = lineString.coordinates[0][0].toInt()
        var newY = lineString.coordinates[0][1].toInt()
        commands.add(commandEncode(Command.LINE_TO.cmd, lineString.coordinates.size-1))
        lineString.coordinates.subList(1, lineString.coordinates.lastIndex+1).forEach { p ->
            commands.add(zigZagEncode(p[0].toInt() - newX))
            commands.add(zigZagEncode(p[1].toInt() - newY))
            newX = p[0].toInt()
            newY = p[1].toInt()
        }
        return Triple(commands, newX, newY)
    }

    private fun encodeMultiLineString(mls: Geometry.MultiLineString) : List<Int> {
        return mls.coordinates.fold(listOf<Triple<List<Int>, Int, Int>>(Triple(emptyList(), 0, 0))) { l, lineString ->
            l + encodeLineString(Geometry.LineString(coordinates = lineString), l.last().second, l.last().third)
        }.fold(listOf()) { l, lineString ->
            l + lineString.first
        }
    }

    //TODO("test for ccw exterior rings")
    private fun encodePolygon(polygon: Geometry.Polygon, x: Int = 0, y: Int = 0) : Triple<MutableList<Int>, Int, Int> {
        val commands = mutableListOf<Int>()
        var newX = x
        var newY = y
        polygon.coordinates.map { it.reversed() }.forEach { ring ->
            commands.add(commandEncode(Command.MOVE_TO.cmd, 1))
            commands.add(zigZagEncode(ring[0][0].toInt() - newX))
            commands.add(zigZagEncode(ring[0][1].toInt() - newY))
            newX = ring[0][0].toInt()
            newY = ring[0][1].toInt()
            commands.add(commandEncode(Command.LINE_TO.cmd, ring.size-1))
            ring.subList(1, ring.lastIndex+1).forEach { p ->
                commands.add(zigZagEncode(p[0].toInt() - newX))
                commands.add(zigZagEncode(p[1].toInt() - newY))
                newX = p[0].toInt()
                newY = p[1].toInt()
            }
            commands.add(commandEncode(Command.CLOSE_PATH.cmd, 1))
        }
        return Triple(commands, newX, newY)
    }

    private fun encodeMultiPolygon(multiPolygon: Geometry.MultiPolygon) =
        multiPolygon.coordinates.fold(listOf<Triple<List<Int>, Int, Int>>(Triple(emptyList(), 0, 0))) { l, polygon ->
            l + encodePolygon(Geometry.Polygon(coordinates = polygon), l.last().second, l.last().third)
        }.fold(listOf<Int>()) { l, polygon ->
            l + polygon.first
        }

    private fun decodePoint(geometry: List<Int>) : List<List<Int>> {
        var length = 0
        var command = 0
        var x = 0
        var y = 0
        var isX = true
        val coords = mutableListOf<List<Int>>()
        var point = mutableListOf<Int>()
        geometry.forEach { cmd ->
            if (length <= 0) {
                val what = commandDecode(cmd)
                command = what.first
                length = what.second

            } else if (command != Command.CLOSE_PATH.cmd){
                if (isX) {
                    x += zigZagDecode(cmd)
                    point.add(x)
                    isX = false
                } else {
                    y += zigZagDecode(cmd)
                    point.add(y)
                    length -= 1
                    isX = true
                }

            }
            if (length <= 0) {
                coords.add(point)
                point = mutableListOf()
            }
        }
        return coords
    }

    private fun decodeLineString(geometry: List<Int>) : List<List<List<Int>>> {
        var length = 0
        var command = 0
        var x = 0
        var y = 0
        var isX = true
        val coords = mutableListOf<List<List<Int>>>()
        var ring = mutableListOf<List<Int>>()
        geometry.forEach { cmd ->
            if (length <= 0) {
                val what = commandDecode(cmd)
                command = what.first
                length = what.second

            } else if (command != Command.CLOSE_PATH.cmd) {
                if (isX) {
                    x += zigZagDecode(cmd)
                    isX = false
                } else {
                    y += zigZagDecode(cmd)
                    ring.add(listOf(x, y))
                    length -= 1
                    isX = true
                }

            }
            if (length <= 0 && command == Command.LINE_TO.cmd) {
                coords.add(ring)
                ring = mutableListOf()
            }
        }
        return coords
    }

    private fun decodePolygon(geometry: List<Int>) : List<List<List<List<Int>>>> {
        var length = 0
        var command = 0
        var x = 0
        var y = 0
        var isX = true
        val polygons = mutableListOf<List<List<List<Int>>>>()
        var coords = mutableListOf<List<List<Int>>>()
        var ring = mutableListOf<List<Int>>()
        geometry.forEach { cmd ->
            if (length <= 0 || command == Command.CLOSE_PATH.cmd) {
                val what = commandDecode(cmd)
                command = what.first
                length = what.second

                if (command == Command.CLOSE_PATH.cmd) {
                    coords.add(ring.reversed())
                    ring = mutableListOf()
                }

            } else if (command != Command.CLOSE_PATH.cmd) {
                if (isX) {
                    x += zigZagDecode(cmd)
                    isX = false
                } else {
                    y += zigZagDecode(cmd)
                    ring.add(listOf(x, y))
                    length -= 1
                    isX = true
                }

            }
            if (length <= 0 && command == Command.LINE_TO.cmd) {
                if (coords.isNotEmpty() && isCCW(ring)) {
                    polygons.add(coords)
                    coords = mutableListOf()
                }
            }

        }
        polygons.add(coords)
        return polygons
    }

    /**
     * Retrieve attribute map from a Feature and a tag dictionary
     * @param f One feature for decoding
     * @param keysList A list of [String] containing the tag keys
     * @param valuesList A list of [VectorTile.Tile.Value] containing the actual property values
     */
    fun decodeProperties(f: VectorTile.Tile.Feature, keysList: List<String>, valuesList: List<VectorTile.Tile.Value>): Map<String, Value> {
        return f.tagsList.chunked(2).map { tag ->
            when {
                valuesList[tag[1]].hasDoubleValue() -> keysList[tag[0]] to Value.DoubleValue(valuesList[tag[1]].doubleValue)
                valuesList[tag[1]].hasIntValue() -> keysList[tag[0]] to Value.IntValue(valuesList[tag[1]].intValue)
                else -> keysList[tag[0]] to Value.StringValue(valuesList[tag[1]].stringValue)
            }
        }.toMap()
    }

    // https://developers.google.com/protocol-buffers/docs/encoding#types
    private fun zigZagEncode(n: Int) = (n shl 1) xor (n shr 31)

    private fun zigZagDecode(n: Int) = (n shr 1) xor (-(n and 1))

    private fun commandEncode(id: Int, n: Int) = (n shl 3) or id

    private fun commandDecode(command: Int) = Pair(command and (1 shl 3) - 1, command shr 3)

    //TODO("not robust, but enough for now")
    /**
     * Implements https://en.wikipedia.org/wiki/Shoelace_formula
     */
    private fun isCCW(ring: List<List<Int>>) =
        ring.subList(1, ring.lastIndex+1).foldIndexed(0) { i, sum, p ->
            sum + (p[0]-ring[i][0])*(p[1]+ring[i][1])
        } < 0

    private fun findLayer(t: VectorTile.Tile, layer: String) : Int {
        t.layersList.forEachIndexed { i, l ->
            if (l.name == layer) return i
        }
        return -1
    }

}