package xyz.icetang.lib.icemmand.internal

import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import xyz.icetang.lib.icemmand.IcemmandSource
import xyz.icetang.lib.icemmand.ref.getValue
import xyz.icetang.lib.icemmand.ref.weak
import xyz.icetang.lib.icemmand.wrapper.EntityAnchor
import xyz.icetang.lib.icemmand.wrapper.Position3D
import xyz.icetang.lib.icemmand.wrapper.Rotation
import java.util.*

class IcemmandSourceImpl private constructor(
    handle: CommandSourceStack
) : IcemmandSource {
    companion object {
        private val refs = WeakHashMap<CommandSourceStack, IcemmandSourceImpl>()

        fun wrapSource(source: CommandSourceStack): IcemmandSourceImpl =
            refs.computeIfAbsent(source) {
                IcemmandSourceImpl(source)
            }
    }

    private val handle by weak(handle)

    override val displayName: Component
        get() = handle.sender.name()

    override val sender: CommandSender
        get() = handle.sender

    override val entity: Entity
        get() = handle.executor!!

    override val entityOrNull: Entity?
        get() = handle.executor

    override val player: Player
        get() = handle.sender as Player

    override val playerOrNull: Player?
        get() = handle.sender.takeIf { it is Player } as? Player

    override val position: Position3D
        get() = handle.location.run { Position3D(x, y, z) }

    override val rotation: Rotation
        get() = handle.location.run { Rotation(yaw, pitch) }

    override val anchor: EntityAnchor
        get() = EntityAnchor.FEET // 임시

    override val world: World
        get() = handle.location.world

    override val location: Location
        get() = handle.location

//    override fun hasPermission(level: Int, bukkitPermission: String): Boolean {
//        return handle.hasPermission(level, bukkitPermission)
//    }
}