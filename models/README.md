# The Marauder's SuperCharged Simple Geometry Models

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