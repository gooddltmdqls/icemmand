package xyz.icetang.lib.icemmand.node

import xyz.icetang.lib.icemmand.*

// 커맨드 노드
@IcemmandDsl
interface IcemmandNode : IcemmandArgumentSupport {

    /**
     * 명령 실행에 필요한 권한을 체크합니다.
     *
     * 이 함수는 개별 명령에만 적용되며 하위 노드에 영향을 주지 않습니다.
     *
     * ```kotlin
     * kommand {
     *   register("mycmd") {
     *     then("first") { // mycmd first
     *       requires { hasPermission(4) }
     *       executes {
     *       // 4 레벨 권한이 있어야 실행됨
     *       }
     *       then("second") { // /mycmd first second
     *         executes {
     *         // 권한 필요 없음
     *       }
     *     }
     *   }
     * }
     * ```
     *
     * @see RootNode.requires
     */
    fun requires(requires: IcemmandSource.() -> Boolean)

    fun executes(executes: IcemmandSource.(IcemmandContext) -> Unit)

    fun then(
        argument: Pair<String, IcemmandArgument<*>>,
        vararg arguments: Pair<String, IcemmandArgument<*>>,
        init: IcemmandNode.() -> Unit
    )

    fun then(
        name: String,
        vararg arguments: Pair<String, IcemmandArgument<*>>,
        init: IcemmandNode.() -> Unit
    )

    operator fun String.invoke(vararg arguments: Pair<String, IcemmandArgument<*>>, init: IcemmandNode.() -> Unit) =
        then(this, *arguments, init = init)
}