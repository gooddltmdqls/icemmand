package xyz.icetang.lib.icemmand.internal

import xyz.icetang.lib.icemmand.node.LiteralNode

open class LiteralNodeImpl : AbstractIcemmandNode(), LiteralNode {
    internal fun initialize(icemmand: IcemmandDispatcherImpl, name: String) {
        initialize0(icemmand, name)
    }
}