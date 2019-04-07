# Supercharger

[![Apache License, Version 2.0](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0) [![Build Status](https://travis-ci.org/marauder-io/supercharger.svg?branch=master)](https://travis-ci.org/marauder-io/supercharger)

## A marauder's supercharged collection of Vector Tile Tools

The [Encoder](#encoder) is the main module to work with vector tiles.
When talking about vector tiles, it refers to a standard developed by MapBox (https://github.com/mapbox/vector-tile-spec).

The collection consists of following supercharged components:
* [Encoder](#encoder)
* [Clipper](#geometry-clipper)
* [Models](#geometry-models)
* [Projector](#geometry-projector)
* [Utils](#geometry-utils)

# Encoder

A Encoder and Decoder for Mapbox Vector Tiles. We implement the specification version 2.1
(https://github.com/mapbox/vector-tile-spec/tree/master/2.1).

## Features

* Fast vector tile encoding from simple GeoJSON like Objects
* Fast Vector tile decoding
* Efficient & flexible vector tile merging
   * Directly from two BLOBs
   * By injecting one tile in another's encoding function call
* Runs without JTS or GeoTools dependencies
   * Also JTS Geometry support is implemented
* Allows to merge tiles without completely decoding them

## Usage

### Gradle

Artifacts are published on OSS Sonatype and Maven Central.

Gradle dependency:
```groovy
compile group: 'io.marauder.supercharger', name: 'encoder', version: '0.0.3'
compile group: 'io.marauder.supercharger', name: 'models', version: '0.0.3'
compile group: 'io.marauder.supercharger', name: 'projector', version: '0.0.3'
compile group: 'io.marauder.supercharger', name: 'utils', version: '0.0.3'
compile group: 'io.marauder.supercharger', name: 'clipper', version: '0.0.3'
```

For snapshots use this repository:
```groovy
maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
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

### Hints

* As intended our Encoder/Decoder does no coordinate transformation and clipping

# Geometry Clipping

Provides functionality to clip functions in `x` or `y` direction on given boundaries.

```kotlin
val clipper = Clipper()
clipper.clip(fc = featureCollection, ...)

```

# Geometry Models

## Available models
* `GeoJson` - Feature collection
* `Feature` - A features
* `Geometry` - A geometry, sub classes used to instantiate
* `Value` - Holds a feature's property value, sub classes used to instantiate

## Geometry JTS/WKT support

```kotlin
val g1 = Geometry.fromJTS(jtsObject)
val g2 = Geometry.fromWKT(wktString)

val jts = g1.toJTS()
val wkt = g1.toWKT()

```

# Geometry Projector

```kotlin
val projector = Projector(extend = 4096)
projector.transformGeometry(...)

```

# Geometry Utils

Util functions for geometry intersection testing and geometry simplifications.

License
-------

SuperCharger is licensed under the
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.