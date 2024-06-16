@file:Suppress("UnstableApiUsage")

package xyz.icetang.lib.icemmand.internal

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import com.mojang.brigadier.tree.RootCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.server.PluginDisableEvent
import org.bukkit.plugin.Plugin
import xyz.icetang.lib.icemmand.Icemmand
import xyz.icetang.lib.icemmand.IcemmandDispatcher
import xyz.icetang.lib.icemmand.internal.IcemmandContextImpl.Companion.wrapContext
import xyz.icetang.lib.icemmand.internal.IcemmandSourceImpl.Companion.wrapSource
import xyz.icetang.lib.icemmand.node.RootNode

class IcemmandImpl : Icemmand {
    private lateinit var commands: Commands
    private lateinit var root: RootCommandNode<CommandSourceStack>
    private lateinit var dispatcher: CommandDispatcher<CommandSourceStack>
    private lateinit var children: MutableMap<String, CommandNode<CommandSourceStack>>
    private lateinit var literals: MutableMap<String, LiteralCommandNode<CommandSourceStack>>

    override fun register(
        plugin: Plugin,
        name: String,
        vararg aliases: String,
        init: RootNode.() -> Unit
    ): IcemmandDispatcher {
        require(plugin.isEnabled) { "Plugin disabled!" }

        return IcemmandDispatcherImpl().apply {
            initialize(plugin, name)
            root.init()
            immutable = true
        }.also { dispatcher ->
//            register(plugin, dispatcher, aliases.toList())

            val lifecycle = plugin.lifecycleManager

            lifecycle.registerEventHandler(LifecycleEvents.COMMANDS) {
                commands = it.registrar()
                this.dispatcher = commands.dispatcher
                root = this.dispatcher.root

                children = root["children"]
                literals = root["literals"]

                require(test(name, aliases)) { "Command already exists!" }

                val node = this.dispatcher.register(dispatcher.root.convert() as LiteralArgumentBuilder<CommandSourceStack>)
//                aliases.forEach {
//                    commands.register(
//                        this.dispatcher.register(literal(it).redirect(node))
//                    )
//                }

                commands.register(
                    node,
                    dispatcher.root.description,
                    aliases.toList()
                )
            }

            plugin.server.pluginManager.registerEvents(
                object : Listener {
                    @EventHandler(priority = EventPriority.LOWEST)
                    fun onPluginDisable(event: PluginDisableEvent) {
                        if (event.plugin === plugin) {
                            unregister(name)
                            aliases.forEach { unregister(it) }
                        }
                    }
                },
                plugin
            )
        }
    }

//    fun register(plugin: Plugin, dispatcher: IcemmandDispatcherImpl, aliases: List<String>) {
//
//        commandMap.register(
//            root.fallbackPrefix,
//            VanillaCommandWrapper(vanillaCommands, node).apply {
//                description = root.description
//                usage = root.usage
//                permission = null
//
//                setAliases(aliases.toList())
//            }
//        )
//    }

    fun unregister(name: String) {
        if (!::children.isInitialized) {
            throw IllegalStateException("Not registered yet")
        }

        literals.remove(name)
        children.remove(name)
    }

    fun test(name: String, aliases: Array<out String>): Boolean {
        if (!::literals.isInitialized) return true

        return literals[name] == null && aliases.all { literals[it] == null }
    }
}

@Suppress("UNCHECKED_CAST")
private operator fun <T> CommandNode<*>.get(name: String): T {
    val field = CommandNode::class.java.getDeclaredField(name).apply { isAccessible = true }
    return field.get(this) as T
}

private fun AbstractIcemmandNode.convert(): ArgumentBuilder<CommandSourceStack, *> {
    return when (this) {
        is RootNodeImpl, is LiteralNodeImpl -> literal(name)
        is ArgumentNodeImpl -> {
            val icemmandArgument = argument as IcemmandArgumentImpl<*>
            val type = icemmandArgument.type
            argument(name, type).apply {
                suggests { context, suggestionsBuilder ->
                    icemmandArgument.listSuggestions(wrapContext(context), suggestionsBuilder)
                }
            }
        }

        else -> error("Unknown node type ${javaClass.name}")
    }.apply {
        requires { source ->
            /**
             * 권한 테스트 순서
             * requirement -> permission
             */
            kotlin.runCatching {
                requires(wrapSource(source))
            }.onFailure {
                if (it !is CommandSyntaxException) it.printStackTrace()
            }.getOrThrow()
        }

        executes?.let { executes ->
            executes { context ->
                wrapSource(context.source).runCatching {
                    executes(this@convert.wrapContext(context))
                }.onFailure {
                    if (it !is CommandSyntaxException) it.printStackTrace()
                }.getOrThrow()
                1
            }
        }

        nodes.forEach { node ->
            then(node.convert())
        }
    }
}

private fun literal(name: String): LiteralArgumentBuilder<CommandSourceStack> {
    return LiteralArgumentBuilder.literal(name)
}

private fun argument(name: String, argumentType: ArgumentType<*>): RequiredArgumentBuilder<CommandSourceStack, *> {
    return RequiredArgumentBuilder.argument(name, argumentType)
}