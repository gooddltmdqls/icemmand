package xyz.icetang.lib.icemmand.wrapper

data class Position2D(val x: Double, val z: Double) {
    fun withY(y: Double): Position3D {
        return Position3D(x, y, z)
    }
}