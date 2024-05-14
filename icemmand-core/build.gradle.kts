plugins {
    alias(libs.plugins.dokka)
}

dependencies {
    implementation(projectApi)
}

tasks {
    create<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    create<Jar>("dokkaJar") {
        archiveClassifier.set("javadoc")
        dependsOn("dokkaHtml")

        from("${layout.buildDirectory.asFile.get()}/dokka/html/") {
            include("**")
        }
    }
}