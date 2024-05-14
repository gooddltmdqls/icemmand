package xyz.icetang.lib.icemmand.wrapper

data class BlockPosition2D(val x: Int, val z: Int) {
    fun withY(y: Int): BlockPosition3D {
        return BlockPosition3D(x, y, z)
    }
}