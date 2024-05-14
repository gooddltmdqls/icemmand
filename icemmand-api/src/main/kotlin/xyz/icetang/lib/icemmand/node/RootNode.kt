package xyz.icetang.lib.icemmand.node

import xyz.icetang.lib.icemmand.IcemmandDsl
import xyz.icetang.lib.icemmand.IcemmandSource

@IcemmandDsl
interface RootNode : IcemmandNode {
    /**
     * 명령의 접두사
     *
     * 기본값 = <플러그인의 이름>
     */
    var fallbackPrefix: String

    /**
     * 명령의 설명
     *
     * 기본값 = A <플러그인의 이름> provided command
     */
    var description: String

    /**
     * 명령어 사용법
     *
     * 기본값 = /<명령>
     */
    var usage: String


    /**
     * 명령 실행에 필요한 권한을 체크합니다.
     *
     * 이 함수는 모든 하위 노드에 영향을 줍니다.
     *
     * ```kotlin
     * kommand {
     *   register("mycmd") {
     *   requires { hasPermission(4) }
     *     then("first") { // mycmd first
     *       executes {
     *       // 4 레벨 권한이 있어야 실행됨
     *       }
     *       then("second") { // /mycmd first second
     *         executes {
     *         // 4 레벨 권한이 있어야 실행됨
     *         }
     *       }
     *     }
     *   }
     * }
     * ```
     *
     * @see IcemmandNode.requires
     */
    override fun requires(requires: IcemmandSource.() -> Boolean)

}