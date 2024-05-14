package xyz.icetang.lib.icemmand.internal

import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import net.kyori.adventure.text.ComponentLike
import xyz.icetang.lib.icemmand.AbstractIcemmandSuggestion
import xyz.icetang.lib.icemmand.ref.getValue
import xyz.icetang.lib.icemmand.ref.weak
import java.util.*

class IcemmandSuggestionImpl(
    handle: SuggestionsBuilder
) : AbstractIcemmandSuggestion() {
    private val handle by weak(handle)

    override fun suggest(value: Int, tooltip: (() -> ComponentLike)?) {
        if (tooltip == null) handle.suggest(value)
        else handle.suggest(value, MessageComponentSerializer.message().serialize(tooltip().asComponent()))
    }

    override fun suggest(text: String, tooltip: (() -> ComponentLike)?) {
        if (tooltip == null) handle.suggest(text)
        else handle.suggest(text, MessageComponentSerializer.message().serialize(tooltip().asComponent()))
    }

    override fun suggest(candidates: Iterable<String>, tooltip: ((String) -> ComponentLike)?) {
        val handle = handle
        val input: String = handle.remaining.lowercase(Locale.ROOT)

        candidates.forEach { candidate ->
            val lowerCandidate = candidate.lowercase(Locale.ROOT)

            if (matchesSubStr(input, lowerCandidate)) {
                if (tooltip == null) handle.suggest(candidate)
                else handle.suggest(candidate, MessageComponentSerializer.message().serialize(tooltip(candidate).asComponent()))
            }
        }
    }

    override fun <T> suggest(
        candidates: Iterable<T>,
        transform: (T) -> String,
        tooltip: ((T) -> ComponentLike)?
    ) {
        val handle = handle
        val input: String = handle.remaining.lowercase(Locale.ROOT)

        candidates.forEach {
            val candidate = transform(it)
            val lowerCandidate = transform(it).lowercase(Locale.ROOT)

            if (matchesSubStr(input, lowerCandidate)) {
                if (tooltip == null) handle.suggest(candidate)
                else handle.suggest(candidate, MessageComponentSerializer.message().serialize(tooltip(it).asComponent()))
            }
        }
    }

    override fun <T> suggest(
        candidates: Map<String, T>,
        tooltip: ((T) -> ComponentLike)?
    ) {
        val handle = handle
        val input: String = handle.remaining.lowercase(Locale.ROOT)

        candidates.forEach { (key, value) ->
            val lowerCandidate = key.lowercase(Locale.ROOT)

            if (matchesSubStr(input, lowerCandidate)) {
                if (tooltip == null) handle.suggest(key)
                else handle.suggest(key, MessageComponentSerializer.message().serialize(tooltip(value).asComponent()))
            }
        }
    }

    fun matchesSubStr(remaining: String?, candidate: String): Boolean {
        var i = 0

        while (!candidate.startsWith(remaining!!, i)) {
            i = candidate.indexOf(95.toChar(), i)
            if (i < 0) {
                return false
            }
            i++
        }

        return true
    }
}