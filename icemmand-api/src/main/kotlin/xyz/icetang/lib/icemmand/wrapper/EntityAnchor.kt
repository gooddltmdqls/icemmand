package xyz.icetang.lib.icemmand.wrapper

import org.bukkit.entity.Entity
import org.bukkit.util.Vector
import xyz.icetang.lib.icemmand.IcemmandSource

interface EntityAnchor {
    companion object {
        val FEET by lazy { WrapperSupport.entityAnchorFeet() }
        val EYES by lazy { WrapperSupport.entityAnchorEyes() }
    }

    val name: String

    fun applyTo(entity: Entity): Vector

    fun applyTo(source: IcemmandSource): Vector
}