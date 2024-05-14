package xyz.icetang.lib.icemmand.internal

import xyz.icetang.lib.icemmand.IcemmandArgument
import xyz.icetang.lib.icemmand.node.ArgumentNode

class ArgumentNodeImpl : AbstractIcemmandNode(), ArgumentNode {
    lateinit var argument: IcemmandArgument<*>

    internal fun initialize(icemmand: IcemmandDispatcherImpl, name: String, argument: IcemmandArgument<*>) {
        initialize0(icemmand, name)
        this.argument = argument
    }
}