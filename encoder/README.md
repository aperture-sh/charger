# The Marauder's SuperCharged Vector Tile Encoder

A Encoder and Decoder for Mapbox Vector Tiles. We implement the specification version 2.1
(https://github.com/mapbox/vector-tile-spec/tree/master/2.1).

## Features

* Fast vector tile encoding from simple GeoJSON like Objects
* Fast Vector tile decoding
* Efficient & flexible vector tile merging
   * Directly from two BLOBs
   * By injecting one tile in another's encoding function call
* Runs without JTS or GeoTools dependencies
* Allows to merge tiles without completely decoding them

## Usage

### Gradle

Add our artifactory repository. Keep in mind to set the credentials either in the `gradle.properties` file or as environment variables.
```groovy
repositories {
    jcenter()
    maven {
        url = rootProject.hasProperty("art_url") ? "$art_url" : "$System.env.artifactory_contextUrl"
        credentials {
            username = rootProject.hasProperty("art_username") ? "$art_username" : "$System.env.artifactory_user"
            password = rootProject.hasProperty("art_password") ? "$art_password" : "$System.env.artifactory_password"
        }
    }

}
```
`gradle.properties` file exeample:
```
art_username=
art_password=
art_url=https://geocode.igd.fraunhofer.de:8081/artifactory/libs-snapshot
```

Gradle dependency:
```groovy
compile group: 'io.marauder', name: 'sc-encoder', version: '0.0.2'
```

### Encoding Example

```kotlin
val f1 = Feature(
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
val f1 = JSON.plain.parse<List<Feature>>(File("features.geojson").readText())
val encoder = Encoder()
val tile : vector_tile.VectorTile.Tile = encoder.encode(listOf(f1), "test1")       
val tile : vector_tile.VectorTile.Tile = encoder.encode(listOf(f2), "test2")       
```

### Decoding Example

```kotlin
val bytes = File("file.pbf").readBytes()
val encoder = Encoder()
val features: List<Feature> = encoder.decode(bytes)
```

### Current Limitations

* Only one layer per tile supported, decoder reads only layer 0

## Hints

* As intended our Encoder/Decoder does no coordinate transformation and clipping
