package xyz.icetang.lib.icemmand.wrapper

import org.bukkit.util.Vector
import java.lang.Math.toRadians
import kotlin.math.cos
import kotlin.math.sin

class Rotation(
    val yaw: Float,
    val pitch: Float
)

fun Rotation.toDirection(): Vector {
    val vector = Vector()

    val rotX: Double = pitch.toDouble()
    val rotY: Double = yaw.toDouble()

    vector.y = -sin(toRadians(rotY))

    val xz = cos(toRadians(rotY))

    vector.x = -xz * sin(toRadians(rotX))
    vector.z = xz * cos(toRadians(rotX))

    return vector
}