package xyz.icetang.lib.icemmand.internal

import xyz.icetang.lib.icemmand.IcemmandArgument
import xyz.icetang.lib.icemmand.node.IcemmandNode
import xyz.icetang.lib.icemmand.IcemmandArgumentSupport
import xyz.icetang.lib.icemmand.IcemmandContext
import xyz.icetang.lib.icemmand.IcemmandSource
import kotlin.properties.ObservableProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class AbstractIcemmandNode : IcemmandNode, IcemmandArgumentSupport by IcemmandArgumentSupport.INSTANCE {
    protected fun <T> icemmandField(initialValue: T): ReadWriteProperty<Any?, T> =
        object : ObservableProperty<T>(initialValue) {
            private var initialized = false

            override fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T): Boolean {
                require(!icemmand.immutable) { "Cannot redefine ${property.name} after registration" }
                require(!initialized) { "Cannot redefine ${property.name} after initialization" }

                return true
            }

            override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) {
                initialized = true
            }
        }

    lateinit var icemmand: IcemmandDispatcherImpl
    lateinit var name: String

    var parent: AbstractIcemmandNode? = null

    var requires: (IcemmandSource.() -> Boolean) by icemmandField { true }
        private set

    var executes: (IcemmandSource.(context: IcemmandContext) -> Unit)? by icemmandField(null)
        private set

    protected fun initialize0(icemmand: IcemmandDispatcherImpl, name: String) {
        this.icemmand = icemmand
        this.name = name
    }

    val nodes = arrayListOf<AbstractIcemmandNode>()

    override fun requires(requires: IcemmandSource.() -> Boolean) {
        this.requires = requires
    }

    override fun executes(executes: IcemmandSource.(context: IcemmandContext) -> Unit) {
        this.executes = executes
    }

    override fun then(name: String, vararg arguments: Pair<String, IcemmandArgument<*>>, init: IcemmandNode.() -> Unit) {
        icemmand.checkState()

        then(LiteralNodeImpl().apply {
            parent = this@AbstractIcemmandNode
            initialize(this@AbstractIcemmandNode.icemmand, name)
        }.also {
            nodes += it
        }, arguments, init)
    }

    override fun then(
        argument: Pair<String, IcemmandArgument<*>>,
        vararg arguments: Pair<String, IcemmandArgument<*>>,
        init: IcemmandNode.() -> Unit
    ) {
        icemmand.checkState()

        then(ArgumentNodeImpl().apply {
            parent = this@AbstractIcemmandNode
            initialize(this@AbstractIcemmandNode.icemmand, argument.first, argument.second)
        }.also {
            nodes += it
        }, arguments, init)
    }

    private fun then(
        node: AbstractIcemmandNode,
        arguments: Array<out Pair<String, IcemmandArgument<*>>>,
        init: IcemmandNode.() -> Unit
    ) {
        var tail = node

        for ((subName, subArgument) in arguments) {
            val child = ArgumentNodeImpl().apply {
                parent = tail
                initialize(tail.icemmand, subName, subArgument)
            }.also { tail.nodes += it }
            tail = child
        }

        tail.init()
    }
}