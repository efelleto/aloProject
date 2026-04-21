import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.3.20"
    id("net.fabricmc.fabric-loom") version "1.16.1"
    id("maven-publish")
}

fun prop(name: String): String = project.property(name).toString()

version = prop("mod_version")
group = prop("maven_group")

base {
    archivesName.set(prop("archives_base_name"))
}

val targetJavaVersion = 25
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("aloM0d") {
            sourceSet("main")
            sourceSet("client")
        }
    }
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}

repositories {
    // Add repositories to retrieve artifacts from in here.
    // You should only use this when depending on other mods because
    // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
    // See https://docs.gradle.org/current/userguide/declaring_repositories.html
    // for more information about repositories.
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${prop("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${prop("loader_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${prop("kotlin_loader_version")}")

    implementation("net.fabricmc.fabric-api:fabric-api:${prop("fabric_version")}")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", prop("minecraft_version"))
    inputs.property("loader_version", prop("loader_version"))
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version.toString(),
            "minecraft_version" to prop("minecraft_version"),
            "loader_version" to prop("loader_version"),
            "kotlin_loader_version" to prop("kotlin_loader_version")
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

val prismModsDir = providers
    .gradleProperty("prismModsDir")
    .orElse(
        providers.environmentVariable("PRISM_MODS_DIR")
    )
    .orElse(
        providers.provider {
            "${System.getProperty("user.home")}/.local/share/PrismLauncher/instances/${prop("minecraft_version")}/minecraft/mods"
        }
    )

tasks.register<Copy>("installPrismMod") {
    group = "distribution"
    description = "Builds and installs ${project.base.archivesName.get()} ${project.version} into the configured PrismLauncher mods folder."

    dependsOn(tasks.jar)

    from(tasks.jar.flatMap { it.archiveFile })
    into(prismModsDir)

    doFirst {
        val destination = prismModsDir.get()
        val prefix = "${project.base.archivesName.get()}-"
        val currentJar = tasks.jar.flatMap { it.archiveFileName }.get()
        val modsDir = file(destination)

        require(modsDir.isDirectory) {
            "Prism mods directory does not exist: $destination. Set -PprismModsDir=/path/to/mods or PRISM_MODS_DIR."
        }

        modsDir.listFiles { file ->
            file.isFile && file.name.startsWith(prefix) && file.name.endsWith(".jar") && file.name != currentJar
        }?.forEach { file ->
            logger.lifecycle("Removing old Prism mod jar: ${file.name}")
            file.delete()
        }

        logger.lifecycle("Installing $currentJar to $destination")
    }
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = prop("archives_base_name")
            from(components["java"])
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}
