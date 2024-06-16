package xyz.icetang.lib.icemmand.internal

import org.bukkit.plugin.Plugin
import xyz.icetang.lib.icemmand.IcemmandDispatcher

class IcemmandDispatcherImpl : IcemmandDispatcher {
    internal var immutable = false

    lateinit var root: RootNodeImpl
        private set

    internal fun initialize(plugin: Plugin, name: String) {
        this.root = RootNodeImpl().apply {
            initialize(this@IcemmandDispatcherImpl, name, plugin.name, "A ${plugin.name} provided icemmand")
        }
    }

    fun checkState() {
        require(!immutable) { "DSL Error!" }
    }
}