package xyz.icetang.lib.icemmand

abstract class AbstractIcemmandSuggestion : IcemmandSuggestion {
    var suggestsDefault = false
        private set

    override fun suggestDefault() {
        suggestsDefault = true
    }
}