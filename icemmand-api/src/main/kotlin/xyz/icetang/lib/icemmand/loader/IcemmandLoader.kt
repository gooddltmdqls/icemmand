package xyz.icetang.lib.icemmand.loader

import java.lang.reflect.InvocationTargetException

object IcemmandLoader {
    @Suppress("UNCHECKED_CAST")
    fun <T> loadImpl(type: Class<T>, vararg args: Any? = emptyArray()): T {
        val parameterTypes = args.map {
            it?.javaClass
        }.toTypedArray()

        val packageName = "${type.`package`.name}.internal"

        return try {
            val clazz = Class.forName("${packageName}.${type.simpleName}Impl")

            clazz.getConstructor(*parameterTypes).newInstance(*args) as T
        } catch (exception: ClassNotFoundException) {
            throw UnsupportedOperationException(
                "Unknown class name ${type.name}",
                exception
            )
        } catch (exception: IllegalAccessException) {
            throw UnsupportedOperationException("${type.name} constructor is not visible")
        } catch (exception: InstantiationException) {
            throw UnsupportedOperationException("${type.name} is abstract class")
        } catch (exception: InvocationTargetException) {
            throw UnsupportedOperationException(
                "${type.name} has an error occurred while creating the instance",
                exception
            )
        }
    }
}