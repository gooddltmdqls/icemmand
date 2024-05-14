package xyz.icetang.lib.icemmand.wrapper

import xyz.icetang.lib.icemmand.loader.IcemmandLoader

interface WrapperSupport {
    companion object : WrapperSupport by IcemmandLoader.loadImpl(WrapperSupport::class.java)

    fun entityAnchorFeet(): EntityAnchor

    fun entityAnchorEyes(): EntityAnchor
}