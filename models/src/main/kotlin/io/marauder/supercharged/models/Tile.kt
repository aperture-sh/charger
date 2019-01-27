package io.marauder.supercharged.models

class Tile(val geojson: GeoJSON, val z: Int, val x: Int, val y: Int) {
    fun toID() = (((1 shl z) * y + x) * 32) + z
    companion object {
        @JvmStatic
        fun toID(z: Int, x: Int, y: Int) = (((1 shl z) * y + x) * 32) + z
    }
}

