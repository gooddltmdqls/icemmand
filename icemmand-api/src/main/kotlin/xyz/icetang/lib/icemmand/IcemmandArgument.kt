package xyz.icetang.lib.icemmand

import com.destroystokyo.paper.profile.PlayerProfile
import com.google.gson.JsonObject
import xyz.icetang.lib.icemmand.loader.IcemmandLoader
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Axis
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.advancement.Advancement
import org.bukkit.block.Block
import org.bukkit.block.BlockState
import org.bukkit.block.data.BlockData
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.potion.PotionEffectType
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Team
import xyz.icetang.lib.icemmand.wrapper.*
import java.util.*

// 인수
@IcemmandDsl
interface IcemmandArgument<T> {
    companion object : IcemmandArgumentSupport by IcemmandArgumentSupport.INSTANCE

    fun suggests(provider: IcemmandSuggestion.(context: IcemmandContext) -> Unit)
}

interface IcemmandArgumentSupport {
    companion object {
        val INSTANCE = IcemmandLoader.loadImpl(IcemmandArgumentSupport::class.java)
    }

    // com.mojang.brigadier.arguments

    fun bool(): IcemmandArgument<Boolean>

    fun int(minimum: Int = Int.MIN_VALUE, maximum: Int = Int.MAX_VALUE): IcemmandArgument<Int>

    fun float(minimum: Float = -Float.MAX_VALUE, maximum: Float = Float.MAX_VALUE): IcemmandArgument<Float>

    fun double(minimum: Double = -Double.MAX_VALUE, maximum: Double = Double.MAX_VALUE): IcemmandArgument<Double>

    fun long(minimum: Long = Long.MIN_VALUE, maximum: Long = Long.MAX_VALUE): IcemmandArgument<Long>

    fun string(type: StringType = StringType.SINGLE_WORD): IcemmandArgument<String>

    // net.minecraft.commands.arguments

    fun angle(): IcemmandArgument<Float>

    fun color(): IcemmandArgument<TextColor>

    fun component(): IcemmandArgument<Component>

    fun compoundTag(): IcemmandArgument<JsonObject>

    fun dimension(): IcemmandArgument<World>

    fun entityAnchor(): IcemmandArgument<EntityAnchor>

    fun entity(): IcemmandArgument<Entity>

    fun entities(): IcemmandArgument<Collection<Entity>>

    fun player(): IcemmandArgument<Player>

    fun players(): IcemmandArgument<Collection<Player>>

    fun summonableEntity(): IcemmandArgument<NamespacedKey>

    fun profile(): IcemmandArgument<Collection<PlayerProfile>>

    fun enchantment(): IcemmandArgument<Enchantment>

    fun message(): IcemmandArgument<Component>

    fun mobEffect(): IcemmandArgument<PotionEffectType>

    //    fun nbtPath(): IcemmandArgument<*> [NbtTagArgument]

    fun objective(): IcemmandArgument<Objective>

    fun objectiveCriteria(): IcemmandArgument<Criteria>

    //    fun operation(): IcemmandArgument<*> [OperationArgument]

    fun particle(): IcemmandArgument<Particle>

    fun intRange(): IcemmandArgument<IntRange>

    fun doubleRange(): IcemmandArgument<ClosedFloatingPointRange<Double>>

    fun advancement(): IcemmandArgument<Advancement>

    fun recipe(): IcemmandArgument<Recipe>

    //    ResourceLocationArgument#getPredicate()

    //    ResourceLocationArgument#getItemModifier()

    fun displaySlot(): IcemmandArgument<DisplaySlot>

    fun score(): IcemmandArgument<String>

    fun scores(): IcemmandArgument<Collection<String>>

    fun slot(): IcemmandArgument<Int>

    fun team(): IcemmandArgument<Team>

    fun time(): IcemmandArgument<Int>

    fun uuid(): IcemmandArgument<UUID>

    // net.minecraft.commands.arguments.blocks

    fun blockPredicate(): IcemmandArgument<(Block) -> Boolean>

    fun blockState(): IcemmandArgument<BlockState>

    // net.minecraft.commands.arguments.coordinates

    fun blockPosition(type: PositionLoadType = PositionLoadType.LOADED): IcemmandArgument<BlockPosition3D>

    fun blockPosition2D(): IcemmandArgument<BlockPosition2D>

    fun position(): IcemmandArgument<Position3D>

    fun position2D(): IcemmandArgument<Position2D>

    fun rotation(): IcemmandArgument<Rotation>

    fun swizzle(): IcemmandArgument<EnumSet<Axis>>

    // net.minecraft.commands.arguments.item

    fun function(): IcemmandArgument<() -> Unit>

    fun item(): IcemmandArgument<ItemStack>

    fun itemPredicate(): IcemmandArgument<(ItemStack) -> Boolean>

    // dynamic

    fun <T> dynamic(
        type: StringType = StringType.SINGLE_WORD,
        function: IcemmandSource.(context: IcemmandContext, input: String) -> T?
    ): IcemmandArgument<T>

    fun <T> dynamicByMap(
        map: Map<String, T>,
        type: StringType = StringType.SINGLE_WORD,
        tooltip: ((T) -> ComponentLike)? = null
    ): IcemmandArgument<T> {
        return dynamic(type) { _, input ->
            map[input]
        }.apply {
            suggests {
                if (tooltip == null) {
                    suggest(map.keys)
                } else {
                    suggest(map, tooltip)
                }
            }
        }
    }

    fun <T : Enum<T>> dynamicByEnum(
        set: EnumSet<T>,
        tooltip: ((T) -> ComponentLike)? = null
    ): IcemmandArgument<T> {
        return dynamic(StringType.SINGLE_WORD) { _, input ->
            set.find { it.name == input }
        }.apply {
            suggests {
                suggest(set, { it.name }, tooltip)
            }
        }
    }
}

enum class StringType {
    SINGLE_WORD,
    QUOTABLE_PHRASE,
    GREEDY_PHRASE
}

enum class PositionLoadType {
    LOADED,
    SPAWNABLE
}