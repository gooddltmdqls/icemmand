package xyz.icetang.lib.icemmand

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.GameRule
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import xyz.icetang.lib.icemmand.wrapper.EntityAnchor
import xyz.icetang.lib.icemmand.wrapper.Position3D
import xyz.icetang.lib.icemmand.wrapper.Rotation

// 명령 발신자 정보
@IcemmandDsl
interface IcemmandSource {
    val displayName: Component
    val sender: CommandSender
    val entity: Entity
    val entityOrNull: Entity?
    val player: Player
    val playerOrNull: Player?
    val position: Position3D
    val rotation: Rotation
    val anchor: EntityAnchor
    val world: World
    val location: Location

    val isPlayer
        get() = playerOrNull != null

    val isConsole
        get() = sender == Bukkit.getConsoleSender()

    val isOp
        get() = sender.isOp

    // NMS 없이는 구현 안되는듯
//    fun hasPermission(level: Int): Boolean
//
//    fun hasPermission(level: Int, bukkitPermission: String): Boolean

    fun hasPermission(bukkitPermission: String) = player.hasPermission(bukkitPermission)

    fun feedback(message: ComponentLike) {
        val sender = sender

        if (sender !is Entity || world.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK) == true) {
            sender.sendMessage(message)
        }
    }

    fun broadcast(message: ComponentLike, isAudience: CommandSender.() -> Boolean = { isOp }) {
        feedback(message)

        val sender = sender
        val broadcast =
            text().decorate(TextDecoration.ITALIC).color(NamedTextColor.GRAY).content("[").append(displayName)
                .append(text().content(": ")).append(
                    text().decoration(TextDecoration.ITALIC, false)
                        .append(message)
                ).append(text().content("]"))

        Bukkit.getOnlinePlayers().forEach { player ->
            if (player !== sender && player.isAudience() && player.world.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK) == true) {
                player.sendMessage(broadcast)
            }
        }

        Bukkit.getConsoleSender().let { console ->
            if (console !== sender && console.isAudience()) {
                console.sendMessage(broadcast)
            }
        }
    }
}