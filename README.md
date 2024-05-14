# icemmand

[![Java](https://img.shields.io/badge/Java-21-ED8B00.svg?logo=openjdk)](https://www.azul.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.23-585DEF.svg?logo=kotlin)](http://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-8.5-02303A.svg?logo=gradle)](https://gradle.org)
[![Maven Central](https://img.shields.io/maven-central/v/xyz.icetang.lib/icemmand-api)](https://search.maven.org/artifact/xyz.icetang.lib/icemmand-api)
[![GitHub](https://img.shields.io/github/license/monun/kommand)](https://www.gnu.org/licenses/gpl-3.0.html)
[![YouTube](https://img.shields.io/badge/YouTube-Icetang0123-red.svg?logo=youtube)](https://www.youtube.com/channel/UCDrAR1OWC2MD4s0JLetN0MA)

### Command DSL for PaperMC Plugin (Brigadier)

---

* #### Features
    * 명령어 분기 지원
    * 인수 분석(Parsing)
    * 명령어 제안 지원 (TabComplete)
* #### Tested minecraft versions
    * 1.20.6

---

기존 Paper는 tabComplete가 자동화되지 않았습니다.

따라서 아래와 같이 코드를 작성해야만 했습니다.

```kotlin
class CommandDispatcher : CommandExecutor, TabCompleter {
    ...
    override fun onCommand(...) {
        val commandName = args[0]

        if ("first".equals(commandName, true)) {
            // first 명령 처리
        } else if ("second".equals(commandName, true)) {
            // second 명령 처리
        } else {
            // Error
        }
        // when 문으로도 구현 가능
    }
    
    override fun onTabComplete(...) {
        val commandName = args[0]
        if ("first".startsWith(commandName, true)) {
            // first 명령어 제안
        } else if ("second".startsWith(commandName, true)) {
            // second 명령어 제안
        }
    }
}
...
```

그러나 2024년 5월 13일, Paper에 brigadier가 공식 도입되었습니다.

*icemmand*는 brigadier를 사용하여 명령어를 쉽게 작성할 수 있도록 도와줍니다.

---
아래와 같은 명령 체계가 있다고 생각해봅시다.

* user
    * create \<username>
    * modify \<user>
        * name \<newName>
        * tag \<newTag>

다음 코드는 icemmand의 DSL을 사용한 코드입니다.

```kotlin
// in JavaPlugin
icemmand {
    register("user") {
        then("create") {
            then("name" to string()) { // "name"이라는 이름의 String을 요청합니다.
                executes { context ->
                    val name: String by context
                    createUser(name) // 명령어 실행 함수를 통해 실행
                }
            }
        }
        // 빠른 명령 작성 - then 함수와 동일
        "modify"("user" to dynamic { ... }) { // 동적 인수를 요청합니다.
            "name"("newName" to string()) {
                executes { context ->
                    val user: User by context
                    val newName: String = it["newName"]
                    setUserName(user, newName)
                }
            }
            "tag"("newTag" to string()) {
                executes {
                    // 함수 인수에 의한 타입 추론
                    setUserTag(it["user"], it["newTag"])
                }
            }
        }
    }
}
```

brigadier와 거의 동일한 문법을 이용하나, 가독성이 더욱 좋은 코드가 완성되었습니다.

---

### Gradle

```kotlin
repositories {
    mavenCentral()
}
```

```kotlin
dependencies {
    implementation("xyz.icetang.lib:icemmand-api:<version>")
}
```

### plugin.yml

```yaml
name: ...
version: ...
main: ...
libraries:
  - xyz.icetang.lib:icemmand-core:<version>
```

---

### NOTE

* 라이센스는 GPL-3.0이며 변경 혹은 삭제를 금합니다.
