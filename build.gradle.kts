import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

plugins {
    kotlin("jvm") version "1.9.20"
    `maven-publish`
    `kotlin-dsl`
    id("org.jreleaser") version "1.12.0"
}

group = "io.github.perforators"
version = "2.1.2"

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
}

repositories {
    gradlePluginPortal()
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

publishing {
    publications.withType<MavenPublication> {
        groupId = group.toString()
        artifactId = project.name

        artifact(javadocJar)
        artifact(sourcesJar)

        pom {
            name.set("ds")
            description.set("Library of common data structures")
            url.set("https://github.com/perforators/DataStructures")
            inceptionYear.set("2024")

            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("EgorKrivochkov")
                    name.set("Egor Krivochkov")
                    email.set("krivochkov01@mail.ru")
                }
            }
            scm {
                url.set("https://github.com/perforators/DataStructures")
            }
        }
    }

    repositories {
        maven {
            setUrl(layout.buildDirectory.dir("/staging-deploy"))
        }
    }
}

ext["signing.password"] = null
ext["signing.secretKey"] = null
ext["signing.openKey"] = null
ext["ossrhUsername"] = null
ext["ossrhPassword"] = null

val secretPropsFile = project.rootProject.file("local.properties")
if (secretPropsFile.exists()) {
    secretPropsFile.reader().use {
        Properties().apply {
            load(it)
        }
    }.onEach { (name, value) ->
        ext[name.toString()] = value
    }
} else {
    ext["signing.password"] = System.getenv("SIGNING_PASSWORD")
    ext["signing.secretKey"] = System.getenv("SIGNING_SECRET_KEY")
    ext["signing.openKey"] = System.getenv("SIGNING_OPEN_KEY")
    ext["ossrhUsername"] = System.getenv("OSSRH_USERNAME")
    ext["ossrhPassword"] = System.getenv("OSSRH_PASSWORD")
}

fun getExtraString(name: String) = ext[name]?.toString()

jreleaser {
    signing {
        setActive("ALWAYS")
        armored = true
        setMode("FILE")
        publicKey.set(getExtraString("signing.openKey"))
        secretKey.set(getExtraString("signing.secretKey"))
        passphrase.set(getExtraString("signing.password"))
    }
    deploy.maven.mavenCentral.register("sonatype") {
        username.set(getExtraString("ossrhUsername"))
        password.set(getExtraString("ossrhPassword"))
        setActive("ALWAYS")
        url.set("https://central.sonatype.com/api/v1/publisher")
        stagingRepository("build/staging-deploy")
    }
}

afterEvaluate {
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            apiVersion = "1.9"
            languageVersion = "1.9"
        }
    }
}
