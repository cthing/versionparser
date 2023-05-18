import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.Confidence
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

repositories {
    mavenCentral()
}

plugins {
    `java-library`
    checkstyle
    jacoco
    `maven-publish`
    signing
    alias(libs.plugins.spotbugs)
    alias(libs.plugins.versions)
}

val baseVersion = "2.0.0"
val isSnapshot = true

val isCIServer = System.getenv("CTHING_CI") != null
val buildNumber = if (isCIServer) System.currentTimeMillis().toString() else "0"
version = if (isSnapshot) "$baseVersion-$buildNumber" else baseVersion
group = "org.cthing"
description = "Parses version numbers in a wide range of formats and provides a canonical, comparable version object."

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(libs.versions.java.get()))
    }
}

dependencies {
    testImplementation(libs.junitApi)
    testImplementation(libs.junitParams)
    testImplementation(libs.assertJ)
    testCompileOnly(libs.apiGuardian)
    testRuntimeOnly(libs.junitEngine)

    spotbugsPlugins(libs.spotbugsContrib)
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    isIgnoreFailures = false
    configFile = file("dev/checkstyle/checkstyle.xml")
    configDirectory.set(file("dev/checkstyle"))
    isShowViolations = true
}

spotbugs {
    toolVersion.set(libs.versions.spotbugs)
    ignoreFailures.set(false)
    effort.set(Effort.MAX)
    reportLevel.set(Confidence.MEDIUM)
    excludeFilter.set(file("dev/spotbugs/suppressions.xml"))
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks {
    withType<JavaCompile> {
        options.release.set(libs.versions.java.get().toInt())
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-options", "-Werror"))
    }

    withType<Jar> {
        manifest.attributes(mapOf("Implementation-Title" to project.name,
                                  "Implementation-Vendor" to "C Thing Software",
                                  "Implementation-Version" to project.version))
    }

    withType<Javadoc> {
        val year = SimpleDateFormat("yyyy", Locale.ENGLISH).format(Date())
        with(options as StandardJavadocDocletOptions) {
            breakIterator(false)
            encoding("UTF-8")
            bottom("Copyright &copy; $year C Thing Software")
            memberLevel = JavadocMemberLevel.PUBLIC
            outputLevel = JavadocOutputLevel.QUIET
        }
    }

    spotbugsMain {
        reports.create("html").required.set(true)
    }

    spotbugsTest {
        isEnabled = false
    }

    withType<JacocoReport> {
        dependsOn("test")
        with(reports) {
            xml.required.set(false)
            csv.required.set(false)
            html.required.set(true)
            html.outputLocation.set(File(buildDir, "reports/jacoco"))
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }

    withType<GenerateModuleMetadata> {
        enabled = false
    }

    dependencyUpdates {
        revision = "release"
        gradleReleaseChannel = "current"
        outputFormatter = "plain,xml,html"
        outputDir = File(project.buildDir, "reports/dependencyUpdates").absolutePath

        rejectVersionIf {
            isNonStable(candidate.version)
        }
    }
}

val sourceJar by tasks.registering(Jar::class) {
    from(project.sourceSets["main"].allSource)
    archiveClassifier.set("sources")
}

val javadocJar by tasks.registering(Jar::class) {
    from("javadoc")
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        register("jar", MavenPublication::class) {
            from(components["java"])

            artifact(sourceJar.get())
            artifact(javadocJar.get())

            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("https://github.com/cthing/${project.name}")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("baron")
                        name.set("Baron Roberts")
                        email.set("baron@cthing.com")
                        organization.set("C Thing Software")
                        organizationUrl.set("https://www.cthing.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/cthing/${project.name}.git")
                    developerConnection.set("scm:git:ssh://github.com:cthing/${project.name}")
                    url.set("https://github.com/cthing/${project.name}/src")
                }
                issueManagement {
                    system.set("GitHub")
                    url.set("https://github.com/cthing/${project.name}/issues")
                }
            }
        }
    }

    val repoUrl = if (isSnapshot) property("cthing.nexus.snapshotsUrl") else property("cthing.nexus.candidatesUrl")
    if (repoUrl != null) {
        repositories {
            maven {
                name = "CThingMaven"
                setUrl(repoUrl)
                credentials {
                    username = property("cthing.nexus.user") as String
                    password = property("cthing.nexus.password") as String
                }
            }
        }
    }
}

if (hasProperty("signing.keyId") && hasProperty("signing.password") && hasProperty("signing.secretKeyRingFile")) {
    signing {
        sign(publishing.publications["jar"])
    }
}
