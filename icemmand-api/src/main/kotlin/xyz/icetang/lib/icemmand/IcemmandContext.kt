package xyz.icetang.lib.icemmand

import kotlin.reflect.KProperty

// 컨텍스트
@IcemmandDsl
interface IcemmandContext {
    val source: IcemmandSource

    val input: String

    operator fun <T> get(name: String): T
}

operator fun <T> IcemmandContext.getValue(
    thisRef: Any?,
    property: KProperty<*>
): T {
    return this[property.name]
}