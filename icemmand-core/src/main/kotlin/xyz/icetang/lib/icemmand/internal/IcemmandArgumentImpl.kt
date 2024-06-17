@file:Suppress("UnstableApiUsage")

package xyz.icetang.lib.icemmand.internal

import com.destroystokyo.paper.profile.PlayerProfile
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.*
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.SuggestionProvider
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.MessageComponentSerializer
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.predicate.ItemStackPredicate
import io.papermc.paper.command.brigadier.argument.range.DoubleRangeProvider
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver
import io.papermc.paper.command.brigadier.argument.resolvers.PlayerProfileListResolver
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import io.papermc.paper.entity.LookAnchor
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.*
import org.bukkit.advancement.Advancement
import org.bukkit.block.Block
import org.bukkit.block.BlockState
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
import xyz.icetang.lib.icemmand.*
import xyz.icetang.lib.icemmand.wrapper.*
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.CompletableFuture

open class IcemmandArgumentImpl<T>(
    val type: ArgumentType<*>,
    private val provider: (IcemmandContextImpl, name: String) -> T,
    private val defaultSuggestionProvider: SuggestionProvider<CommandSourceStack>? = null,
    var suggestionProvider: (IcemmandSuggestion.(IcemmandContext) -> Unit)? = null
) : IcemmandArgument<T> {
    private companion object {
        private val originalMethod: Method = ArgumentType::class.java.declaredMethods.find { method ->
            val parameterTypes = method.parameterTypes

            parameterTypes.count() == 2
                    && parameterTypes[0] == CommandContext::class.java
                    && parameterTypes[1] == SuggestionsBuilder::class.java
        } ?: error("Not found listSuggestion")
        private val overrideSuggestions = hashMapOf<Class<*>, Boolean>()

        private fun checkOverrideSuggestions(type: Class<*>): Boolean = overrideSuggestions.computeIfAbsent(type) {
            originalMethod.declaringClass != type.getMethod(
                originalMethod.name,
                *originalMethod.parameterTypes
            ).declaringClass
        }
    }

    private val hasOverrideSuggestion: Boolean by lazy {
        checkOverrideSuggestions(type.javaClass)
    }

    override fun suggests(provider: IcemmandSuggestion.(IcemmandContext) -> Unit) {
        this.suggestionProvider = provider
    }

    fun from(context: IcemmandContextImpl, name: String): T {
        return provider(context, name)
    }

    fun listSuggestions(
        context: IcemmandContextImpl,
        builder: SuggestionsBuilder
    ): CompletableFuture<Suggestions> {
        this.suggestionProvider?.let {
            val suggestion = IcemmandSuggestionImpl(builder)
            it(suggestion, context)
            if (!suggestion.suggestsDefault) return builder.buildFuture()
        }

        defaultSuggestionProvider?.let { return it.getSuggestions(context.handle, builder) }
        if (hasOverrideSuggestion) return type.listSuggestions(context.handle, builder)
        return builder.buildFuture()
    }
}

infix fun <T> ArgumentType<*>.provideDynamic(
    provider: (context: IcemmandContextImpl, name: String) -> T
): IcemmandArgumentImpl<T> {
    return IcemmandArgumentImpl(this, provider)
}

infix fun <T> ArgumentType<*>.provide(
    provider: (context: CommandContext<CommandSourceStack>, name: String) -> T
): IcemmandArgumentImpl<T> {
    return IcemmandArgumentImpl(this, { context, name ->
        provider(context.handle, name)
    })
}

infix fun <T> Pair<ArgumentType<*>, SuggestionProvider<CommandSourceStack>>.provide(
    provider: (context: CommandContext<CommandSourceStack>, name: String) -> T
): IcemmandArgumentImpl<T> {
    return IcemmandArgumentImpl(first, { context, name ->
        provider(context.handle, name)
    }, defaultSuggestionProvider = second)
}

class IcemmandArgumentSupportImpl : IcemmandArgumentSupport {
    // com.mojang.brigadier.arguments

    override fun bool(): IcemmandArgument<Boolean> {
        return BoolArgumentType.bool() provide BoolArgumentType::getBool
    }

    override fun int(minimum: Int, maximum: Int): IcemmandArgument<Int> {
        return IntegerArgumentType.integer(minimum, maximum) provide IntegerArgumentType::getInteger
    }

    override fun float(minimum: Float, maximum: Float): IcemmandArgument<Float> {
        return FloatArgumentType.floatArg(minimum, maximum) provide FloatArgumentType::getFloat
    }

    override fun double(minimum: Double, maximum: Double): IcemmandArgument<Double> {
        return DoubleArgumentType.doubleArg(minimum, maximum) provide DoubleArgumentType::getDouble
    }

    override fun long(minimum: Long, maximum: Long): IcemmandArgument<Long> {
        return LongArgumentType.longArg(minimum, maximum) provide LongArgumentType::getLong
    }

    override fun string(type: StringType): IcemmandArgument<String> {
        return type.createType() provide StringArgumentType::getString
    }

    // net.minecraft.commands.arguments

    override fun angle(): IcemmandArgument<Float> {
        return FloatArgumentType.floatArg(-180.0f, 180.0f) provide FloatArgumentType::getFloat
    }

    override fun color(): IcemmandArgument<TextColor> {
        return ArgumentTypes.namedColor() provide { context, name ->
            context.getArgument(name, TextColor::class.java)
        }
    }

    override fun component(): IcemmandArgument<Component> {
        return ArgumentTypes.component() provide { context, name ->
            context.getArgument(name, Component::class.java)
        }
    }

    override fun compoundTag(): IcemmandArgument<JsonObject> {
        return StringArgumentType.greedyString() provide { context, name ->
            val string = StringArgumentType.getString(context, name)
            JsonParser.parseString(string) as JsonObject
        }
    }

    override fun dimension(): IcemmandArgument<World> {
        return ArgumentTypes.world() provide { context, name ->
            context.getArgument(name, World::class.java)
        }
    }

    override fun entityAnchor(): IcemmandArgument<EntityAnchor> {
        return ArgumentTypes.entityAnchor() provide { context, name ->
            when (context.getArgument(name, LookAnchor::class.java)) {
                LookAnchor.EYES -> EntityAnchor.EYES
                LookAnchor.FEET -> EntityAnchor.FEET
            }
        }
    }

    override fun entity(): IcemmandArgument<Entity> {
        return ArgumentTypes.entity() provide { context, name ->
            context.getArgument(name, EntitySelectorArgumentResolver::class.java).resolve(context.source).first()
        }

//        return EntityArgument.entity() provide { context, name ->
//            EntityArgument.getEntity(context, name).bukkitEntity
//        }
    }

    override fun entities(): IcemmandArgument<Collection<Entity>> {
        return ArgumentTypes.entities() provide { context, name ->
            context.getArgument(name, EntitySelectorArgumentResolver::class.java).resolve(context.source)
        }

//        return EntityArgument.entities() provide { context, name ->
//            EntityArgument.getEntities(context, name).map { it.bukkitEntity }
//        }
    }

    override fun player(): IcemmandArgument<Player> {
        return ArgumentTypes.player() provide { context, name ->
            context.getArgument(name, PlayerSelectorArgumentResolver::class.java).resolve(context.source).first()
        }

//        return EntityArgument.player() provide { context, name ->
//            EntityArgument.getPlayer(context, name).bukkitEntity
//        }
    }

    override fun players(): IcemmandArgument<Collection<Player>> {
        return ArgumentTypes.players() provide { context, name ->
            context.getArgument(name, PlayerSelectorArgumentResolver::class.java).resolve(context.source)
        }

//        return EntityArgument.players() provide { context, name ->
//            EntityArgument.getPlayers(context, name).map { it.bukkitEntity }
//        }
    }

    // 이건 직접 구현해야 할듯
    override fun summonableEntity(): IcemmandArgument<NamespacedKey> {
        return dynamicByMap(Registry.ENTITY_TYPE.map { it.key }.associateBy { it.asString() })
    }

    override fun profile(): IcemmandArgument<Collection<PlayerProfile>> {
        return ArgumentTypes.playerProfiles() provide { context, name ->
            context.getArgument(name, PlayerProfileListResolver::class.java).resolve(context.source)
        }

//        return GameProfileArgument.gameProfile() provide { context, name ->
//            val nms = GameProfileArgument.getGameProfiles(context, name)
//            nms.map { CraftPlayerProfile.asBukkitMirror(it) }
//        }
    }

    private val enchantmentMap = Registry.ENCHANTMENT.associateBy { it.key.asString() }

    override fun enchantment(): IcemmandArgument<Enchantment> {
        return dynamicByMap(enchantmentMap)
    }

    // 이건 component랑 뭐가 다른지 모르겠음.
    override fun message(): IcemmandArgument<Component> {
        return StringArgumentType.greedyString() provide { context, name ->
            val string = StringArgumentType.getString(context, name)
            MessageComponentSerializer.message().deserialize(LiteralMessage(string))
        }
    }

    private val mobEffectMap = Registry.POTION_EFFECT_TYPE.associateBy { it.key.asString() }

    override fun mobEffect(): IcemmandArgument<PotionEffectType> {
        return dynamicByMap(mobEffectMap)
    }

    override fun objective(): IcemmandArgument<Objective> {
        return dynamic { context, input ->
            val objectives = Bukkit.getScoreboardManager().mainScoreboard.objectives.associateBy { it.name }

            objectives.filter { it.key.lowercase() == input.lowercase() }.values.firstOrNull()
        }.apply {
            suggests {
                val objectives = Bukkit.getScoreboardManager().mainScoreboard.objectives.map { it.name }

                suggest(objectives)
            }
        }
    }

    // kommand에서는 반환값이 Criteria가 아닌 String이었지만, 임의로 Criteria로 변경
    override fun objectiveCriteria(): IcemmandArgument<Criteria> {
        return ArgumentTypes.objectiveCriteria() provide { context, name ->
            context.getArgument(name, Criteria::class.java)
        }
    }

    override fun particle(): IcemmandArgument<Particle> {
        return dynamicByMap(Particle.entries.associateBy { it.key.asString() })
    }

    // range 관련 부분은 누군가가 파싱 구현 바람
    override fun intRange(): IcemmandArgument<IntRange> {
        return ArgumentTypes.integerRange() provide { context, name ->
            val range = ArgumentTypes.integerRange().parse(StringReader(name)).range()
            val min = if (range.hasLowerBound()) range.lowerEndpoint() else Int.MIN_VALUE
            val max = if (range.hasUpperBound()) range.upperEndpoint() else Int.MAX_VALUE

            min..max
        }

//        return RangeArgument.intRange() provide { context, name ->
//            val nms = RangeArgument.Ints.getRange(context, name)
//            val min = nms.min.orElse(Int.MIN_VALUE)
//            val max = nms.max.orElse(Int.MAX_VALUE)
//            min..max
//        }
    }
//
//    //float
    override fun doubleRange(): IcemmandArgument<ClosedFloatingPointRange<Double>> {
        return ArgumentTypes.doubleRange() provide { context, name ->
            val range = context.getArgument(name, DoubleRangeProvider::class.java).range()
            val min = if (range.hasLowerBound()) range.lowerEndpoint() else -Double.MAX_VALUE
            val max = if (range.hasUpperBound()) range.upperEndpoint() else Double.MAX_VALUE

            min..max
        }

//        return RangeArgument.floatRange() provide { context, name ->
//            val nms = RangeArgument.Floats.getRange(context, name)
//            val min = nms.min.orElse(-Double.MAX_VALUE)
//            val max = nms.max.orElse(Double.MAX_VALUE)
//            min..max
//        }
    }

    override fun advancement(): IcemmandArgument<Advancement> {
        return dynamicByMap(Registry.ADVANCEMENT.associateBy { it.key.asString() })
    }

    override fun recipe(): IcemmandArgument<Recipe> {
        val recipes = Bukkit.recipeIterator()

        val map = mutableMapOf<String, Recipe>()

        while (recipes.hasNext()) {
            val recipe = recipes.next()

            if (recipe !is Keyed) continue

            map[recipe.key.asString()] = recipe
        }

        return dynamicByMap(map)
    }

    override fun displaySlot(): IcemmandArgument<DisplaySlot> {
        return ArgumentTypes.scoreboardDisplaySlot() provide { context, name ->
            context.getArgument(name, DisplaySlot::class.java)
        }
    }

    // selector 관련 부분은 파싱 구현 필요
    override fun score(): IcemmandArgument<String> {
        TODO()

//        return ScoreHolderArgument.scoreHolder() provide { context, name ->
//            ScoreHolderArgument.getName(context, name)
//        }
    }

    override fun scores(): IcemmandArgument<Collection<String>> {
        TODO()

//        return ScoreHolderArgument.scoreHolders() provide { context, name ->
//            ScoreHolderArgument.getNames(context, name)
//        }
    }

    // 중괄호별로 접을 수 있는 익스텐션 있는데 ㄹㅇ 편함
    private val slotsMap: Map<String, Int> = mutableMapOf<String, Int>().also { map ->
        for (i in 0..53) {
            map["container.$i"] = i
        }
        for (j in 0..8) {
            map["hotbar.$j"] = j
        }

        for (k in 0..26) {
            map["inventory.$k"] = 9 + k
        }

        for (l in 0..26) {
            map["enderchest.$l"] = 200 + l
        }

        for (m in 0..7) {
            map["villager.$m"] = 300 + m
        }

        for (n in 0..14) {
            map["horse.$n"] = 500 + n
        }

        map["weapon"] = 98
        map["weapon.mainhand"] = 98
        map["weapon.offhand"] = 99
        map["armor.head"] = 100
        map["armor.chest"] = 101
        map["armor.legs"] = 102
        map["armor.feet"] = 103
        map["horse.saddle"] = 400
        map["horse.armor"] = 401
        map["horse.chest"] = 499
    }

    override fun slot(): IcemmandArgument<Int> {
        return dynamicByMap(slotsMap)
    }

//    new SimpleCommandExceptionType(Component.translatable("commands.team.option.seeFriendlyInvisibles.alreadyEnabled"));

    override fun team(): IcemmandArgument<Team> {
        val teams = Bukkit.getScoreboardManager().mainScoreboard.teams.associateBy { it.name }

        return dynamicByMap(teams)
    }

    override fun time(): IcemmandArgument<Int> {
        return ArgumentTypes.time() provide { context, name ->
            context.getArgument(name, Int::class.java)
        }
    }

    override fun uuid(): IcemmandArgument<UUID> {
        return ArgumentTypes.uuid() provide { context, name ->
            context.getArgument(name, UUID::class.java)
        }
    }

    // NMS 개같다 진짜
//    companion object {
//        private val commandBuildContext: CommandBuildContext = ReflectionSupport.getFieldInstance(
//            MinecraftServer.getServer().resources.managers,
//            "commandBuildContext",
//            "c"
//        )
//    }

    // net.minecraft.commands.arguments.blocks

    // 일해라 페이퍼
    override fun blockPredicate(): IcemmandArgument<(Block) -> Boolean> {
        TODO()

//        return BlockPredicateArgument.blockPredicate(commandBuildContext) provide { context, name ->
//            { block ->
//                BlockPredicateArgument.getBlockPredicate(context, name)
//                    .test(BlockInWorld(context.source.level, (block as CraftBlock).position, true))
//            }
//        }
    }

    // 이름은 BlockState인데 리턴값은 BlockData인 이상한 argument. 임시로 BlockState로 변경
    override fun blockState(): IcemmandArgument<BlockState> {
        return ArgumentTypes.blockState() provide { context, name ->
            context.getArgument(name, BlockState::class.java)
        }

//        return BlockStateArgument.block(commandBuildContext) provide { context, name ->
//            CraftBlockData.fromData(BlockStateArgument.getBlock(context, name).state)
//        }
    }

    // net.minecraft.commands.arguments.coordinates

    override fun blockPosition(type: PositionLoadType): IcemmandArgument<BlockPosition3D> {
        return ArgumentTypes.blockPosition() provide { context, name ->
            val blockPosition = context.getArgument(name, BlockPositionResolver::class.java).resolve(context.source)

            BlockPosition3D(blockPosition.blockX(), blockPosition.blockY(), blockPosition.blockZ())
        }

//        return BlockPosArgument.blockPos() provide { context, name ->
//            val blockPosition: Vec3i = when (type) {
//                PositionLoadType.LOADED -> BlockPosArgument.getLoadedBlockPos(context, name)
//                PositionLoadType.SPAWNABLE -> BlockPosArgument.getSpawnablePos(context, name)
//            }
//
//            BlockPosition3D(blockPosition.x, blockPosition.y, blockPosition.z)
//        }
    }

    override fun blockPosition2D(): IcemmandArgument<BlockPosition2D> {
        TODO()

//        return ColumnPosArgument.columnPos() provide { context, name ->
//            val columnPosition: ColumnPos = ColumnPosArgument.getColumnPos(context, name)
//            BlockPosition2D(columnPosition.x, columnPosition.z)
//        }
    }

    override fun position(): IcemmandArgument<Position3D> {
        TODO()

//        return Vec3Argument.vec3() provide { context, name ->
//            val vec3 = Vec3Argument.getVec3(context, name)
//            Position3D(vec3.x, vec3.y, vec3.z)
//        }
    }

    override fun position2D(): IcemmandArgument<Position2D> {
        TODO()

//        return Vec2Argument.vec2() provide { context, name ->
//            val vec2 = Vec2Argument.getVec2(context, name)
//            Position2D(vec2.x.toDouble(), vec2.y.toDouble())
//        }
    }

    override fun rotation(): IcemmandArgument<xyz.icetang.lib.icemmand.wrapper.Rotation> {
        TODO()

//        return RotationArgument.rotation() provide { context, name ->
//            val rotation = RotationArgument.getRotation(context, name).getRotation(context.source)
//            Rotation(rotation.x, rotation.y)
//        }
    }

    override fun swizzle(): IcemmandArgument<EnumSet<Axis>> {
        TODO("Not yet implemented")

//        return SwizzleArgument.swizzle() provide { context, name ->
//            EnumSet.copyOf(SwizzleArgument.getSwizzle(context, name).map { axis ->
//                Axis.valueOf(axis.getName().uppercase())
//            })
//        }
    }

    // net.minecraft.commands.arguments.item

    // NMS 없이 함수를 불러올 방법이 없는 걸로 보임. 있다면 pr 부탁드려요
    override fun function(): IcemmandArgument<() -> Unit> {
        TODO()

//        return FunctionArgument.functions() provide { context, name ->
//            {
//                FunctionArgument.getFunctions(context, name).map { function ->
//                    context.source.server.functions.execute(function, context.source)
//                }
//            }
//        }
    }

    override fun item(): IcemmandArgument<ItemStack> {
        return ArgumentTypes.itemStack() provide { context, name ->
            context.getArgument(name, ItemStack::class.java)
        }

//        return ItemArgument.item(commandBuildContext) provide { context, name ->
//            CraftItemStack.asBukkitCopy(ItemArgument.getItem(context, name).createItemStack(1, false))
//        }
    }

    override fun itemPredicate(): IcemmandArgument<(ItemStack) -> Boolean> {
        return ArgumentTypes.itemPredicate() provide { context, name ->
            context.getArgument(name, ItemStackPredicate::class.java)::test
        }

//        return ItemPredicateArgument.itemPredicate(commandBuildContext) provide { context, name ->
//            { itemStack ->
//                ItemPredicateArgument.getItemPredicate(context, name).test(CraftItemStack.asNMSCopy(itemStack))
//            }
//        }
    }

    private val unknownArgument =
        SimpleCommandExceptionType(LiteralMessage("unknown argument"))

    override fun <T> dynamic(
        type: StringType,
        function: IcemmandSource.(context: IcemmandContext, input: String) -> T?
    ): IcemmandArgument<T> {
        return type.createType() provideDynamic { context, name ->
            context.source.function(context, StringArgumentType.getString(context.handle, name))
                ?: throw unknownArgument.create()
        }
    }
}

fun StringType.createType(): StringArgumentType {
    return when (this) {
        StringType.SINGLE_WORD -> StringArgumentType.word()
        StringType.QUOTABLE_PHRASE -> StringArgumentType.string()
        StringType.GREEDY_PHRASE -> StringArgumentType.greedyString()
    }
}