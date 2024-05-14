package xyz.icetang.lib.icemmand

import org.bukkit.plugin.Plugin
import xyz.icetang.lib.icemmand.loader.IcemmandLoader
import xyz.icetang.lib.icemmand.node.RootNode

@IcemmandDsl
interface Icemmand {
    companion object : Icemmand by IcemmandLoader.loadImpl(Icemmand::class.java)

    fun register(
        plugin: Plugin,
        name: String,
        vararg aliases: String,
        init: RootNode.() -> Unit
    ): IcemmandDispatcher
}

@DslMarker
annotation class IcemmandDsl

@IcemmandDsl
class PluginIcemmand internal constructor(private val plugin: Plugin) {
    fun register(
        name: String,
        vararg aliases: String,
        init: RootNode.() -> Unit
    ) = Icemmand.register(plugin, name, *aliases) { init() }

    operator fun String.invoke(
        vararg aliases: String,
        init: RootNode.() -> Unit
    ) = register(this, *aliases, init = init)
}

fun Plugin.icemmand(init: PluginIcemmand.() -> Unit) {
    PluginIcemmand(this).init()
}