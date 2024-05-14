package xyz.icetang.lib.icemmand.internal

import xyz.icetang.lib.icemmand.node.RootNode

class RootNodeImpl : AbstractIcemmandNode(), RootNode {
    override var fallbackPrefix: String by icemmandField("")
    override var description: String by icemmandField("")
    override var usage: String by icemmandField("")

    internal fun initialize(
        dispatcher: IcemmandDispatcherImpl,
        name: String,
        fallbackPrefix: String,
        description: String
    ) {
        super.initialize0(dispatcher, name)

        this.fallbackPrefix = fallbackPrefix
        this.description = description
        this.usage = "/$name"
    }
}