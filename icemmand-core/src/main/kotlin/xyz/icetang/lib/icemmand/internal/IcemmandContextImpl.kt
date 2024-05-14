package xyz.icetang.lib.icemmand.internal

import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import xyz.icetang.lib.icemmand.IcemmandContext
import xyz.icetang.lib.icemmand.IcemmandSource
import xyz.icetang.lib.icemmand.internal.IcemmandSourceImpl.Companion.wrapSource
import xyz.icetang.lib.icemmand.node.IcemmandNode
import xyz.icetang.lib.icemmand.ref.getValue
import xyz.icetang.lib.icemmand.ref.weak
import java.util.*

class IcemmandContextImpl private constructor(
    private val node: AbstractIcemmandNode,
    handle: CommandContext<CommandSourceStack>
) : IcemmandContext {
    companion object {
        private val refs = WeakHashMap<CommandContext<CommandSourceStack>, IcemmandContextImpl>()

        fun AbstractIcemmandNode.wrapContext(context: CommandContext<CommandSourceStack>): IcemmandContextImpl =
            refs.computeIfAbsent(context) {
                IcemmandContextImpl(this, context)
            }
    }

    internal val handle by weak(handle)

    override val source: IcemmandSource by lazy { wrapSource(handle.source) }

    override val input: String
        get() = handle.input

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(name: String): T {
        val argumentNode = node.findArgumentNode(name) ?: error("Not found argument node $name")
        val argument = argumentNode.argument as IcemmandArgumentImpl<*>

        return argument.from(this, name) as T
    }
}

private fun AbstractIcemmandNode.findArgumentNode(name: String): ArgumentNodeImpl? {
    var node: AbstractIcemmandNode? = this

    while (node != null) {
        if (node is ArgumentNodeImpl && node.name == name) return node
        node = node.parent
    }

    return null
}