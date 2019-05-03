# Changelog
All notable changes to this project will be documented in this file.

## Current Snapshot

* Travis Badge Fix

## [1.0.1] - 2019-05-03

* Renaming to `charger` and `charged` namespace
* Split encoder and models into separate modules
* Bug fixes

## [0.0.2] - 2018-01-20

###Added 

* Feature ids are encoded and decoded now (vector tiles only support long ids)
* More Testing

### Changes

* Bug Fixes
* Renaming of components to `supercharged` namespace. The `Encoder` now can be found in package `io.marauder.supercharged.Encoder`.

## [0.0.1] - 2018-01-10

First release under the CodeName Engine providing main functionality in encoding and decoding Mapbox VectorTiles.
We implement Version 2.1 of the MapBox Vector Tiles Specification.

### Added

* Reading geojson like features
    * Have to be in tile coordinates already
    * Have to be clipped as one needs already
* Encoding a list of features to one vector tile with one layer including the whole bunch of features
* Decoding a vector tile's first layer to a geojson like object
* Serialize decoded features to a geojson like format
* Merging two tiles in two ways
    * Actual direct merging of two protobuf tile objects
    * Injecting protobuf feature objects and their feature attribute tag dictionary into a tile encoding function
* First simple decoding and encoding tests
