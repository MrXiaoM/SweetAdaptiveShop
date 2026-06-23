import top.mrxiaom.gradle.LibraryHelper

plugins {
    java
    `maven-publish`
    id ("com.gradleup.shadow") version "9.3.0"
    id ("com.github.gmazzo.buildconfig") version "5.6.7"
}

buildscript {
    repositories.mavenCentral()
    dependencies.classpath("top.mrxiaom:LibrariesResolver-Gradle:1.7.27")
}

group = "top.mrxiaom.sweet.adaptiveshop"
version = "1.2.2"

val base = LibraryHelper(project)
val targetJavaVersion = 8
val shadowGroup = "top.mrxiaom.sweet.adaptiveshop.libs"
val pluginBaseModules = base.modules.run { listOf(library, gui, actions, l10n, temporaryData, paper) }
val shadowLink = configurations.create("shadowLink")

repositories {
    mavenCentral()
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.helpch.at/releases/")
    maven("https://jitpack.io")
    maven("https://repo.rosewooddev.io/repository/public/")
    maven("https://mvn.lumine.io/repository/maven/")
    maven("https://repo.momirealms.net/releases/")
    maven("https://repo.nightexpressdev.com/releases")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20-R0.1-SNAPSHOT")
    // compileOnly("org.spigotmc:spigot:1.20") // NMS

    compileOnly("net.milkbowl.vault:VaultAPI:1.7")
    compileOnly("org.black_ixx:playerpoints:3.2.7")
    compileOnly(files("libs/MPoints-1.2.2.jar"))
    compileOnly("su.nightexpress.nightcore:main:2.16.2")
    compileOnly("su.nightexpress.excellenteconomy:ExcellentEconomy:2.8.0")
    compileOnly("me.clip:placeholderapi:2.12.2")

    compileOnly("net.momirealms:craft-engine-core:26.6.2")
    compileOnly("net.momirealms:craft-engine-bukkit:26.6.2")

    compileOnly(files("libs/api-itemsadder-3.6.3-beta-14.jar"))
    compileOnly("io.lumine:Mythic-Dist:4.13.0")
    compileOnly("io.lumine:Mythic:5.6.2")
    compileOnly("io.lumine:LumineUtils:1.20-SNAPSHOT")
    compileOnly(base.depend.annotations)

    base.library(LibraryHelper.adventure("4.25.0"))
    base.library(base.depend.HikariCP)
    base.collectPluginHolders()

    implementation(base.depend.nbtapi)
    implementation(base.depend.EvalEx)
    implementation("com.github.technicallycoded:FoliaLib:0.4.4") { isTransitive = false }
    for (artifact in pluginBaseModules) {
        implementation(artifact)
    }
    implementation(base.resolver.lite)
}
buildConfig {
    className("BuildConstants")
    packageName("top.mrxiaom.sweet.adaptiveshop")

    base.doResolveLibraries()

    buildConfigField("String", "VERSION", "\"${project.version}\"")
    buildConfigField("java.time.Instant", "BUILD_TIME", "java.time.Instant.ofEpochSecond(${System.currentTimeMillis() / 1000L}L)")
    buildConfigField("String[]", "RESOLVED_LIBRARIES", base.join())
}

LibraryHelper.initJava(project, base, targetJavaVersion, true)
LibraryHelper.initPublishing(project)

tasks {
    shadowJar {
        configurations.add(project.configurations.runtimeClasspath.get())
        configurations.add(shadowLink)
        mapOf(
            "top.mrxiaom.pluginbase" to "base",
            "de.tr7zw.changeme.nbtapi" to "nbtapi",
            "com.ezylang.evalex" to "evalex",
            "com.tcoded.folialib" to "folialib",
        ).forEach { (original, target) ->
            relocate(original, "$shadowGroup.$target")
        }
    }
}
