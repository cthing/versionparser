import com.github.spotbugs.SpotBugsTask
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// This project is consumed by infrastructure bootstrap code. Therefore it does not use any
// C Thing Software Gradle plugins and is in the org.cthing domain so it can be consumed as
// a third party dependency.

plugins {
    id("java")
    id("checkstyle")
    id("jacoco")
    id("maven-publish")
    id("signing")
    id("com.github.spotbugs").version("1.6.2")
}

val isCIServer = System.getenv("CTHING_CI") != null
val isSnapshot = property("buildType") == "snapshot"

val buildNumber = if (isCIServer) System.currentTimeMillis().toString() else "0"
val semver = this.property("semanticVersion")
version = if (isSnapshot) "$semver-$buildNumber" else semver!!
group = "org.cthing"
description = "Parses versions in a wide range of formats and provides a canonical, comparable version object."

dependencies {
    testCompile("org.junit.jupiter:junit-jupiter-api:5.2.0")
    testCompile("org.junit.jupiter:junit-jupiter-params:5.2.0")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:5.2.0")
    testCompileOnly("org.apiguardian:apiguardian-api:1.0.0")
    testCompile("org.assertj:assertj-core:3.10.0")

    spotbugsPlugins("com.mebigfatguy.fb-contrib:fb-contrib:7.4.2.sb")
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
    options.compilerArgs = listOf("-Xlint:all", "-Xlint:-options", "-Werror")
}

tasks.withType<Jar> {
    manifest.attributes(mapOf("Implementation-Title" to project.name,
                              "Implementation-Vendor" to "C Thing Software",
                              "Implementation-Version" to project.version))
}

tasks.withType<Javadoc> {
    with (options as StandardJavadocDocletOptions) {
        breakIterator(false)
        encoding("UTF-8")
        bottom("Copyright &copy; ${SimpleDateFormat("yyyy", Locale.ENGLISH).format(Date())} C Thing Software. All rights reserved.")
        memberLevel = JavadocMemberLevel.PUBLIC
        outputLevel = JavadocOutputLevel.QUIET
    }
}

checkstyle {
    toolVersion = "8.10.1"
    isIgnoreFailures = false
    configFile = project.file("dev/checkstyle/checkstyle.xml")
    configDir = project.file("dev/checkstyle")
    isShowViolations = true
}

spotbugs {
    toolVersion = "3.1.5"
    isIgnoreFailures = false
    effort = "max"
    reportLevel = "medium"
    excludeFilter = project.file("dev/spotbugs/suppressions.xml")
    sourceSets = listOf(convention.getPlugin<JavaPluginConvention>().sourceSets["main"])
}

tasks.withType<SpotBugsTask> {
    with (reports) {
        xml.isEnabled = false
        html.isEnabled = true
    }
}

jacoco {
    toolVersion = "0.8.1"
}

tasks.withType<JacocoReport> {
    dependsOn("test")
    with (reports) {
        xml.isEnabled = false
        csv.isEnabled = false
        html.isEnabled = true
        html.destination = File(buildDir, "reports/jacoco")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()

    extensions.getByType(JacocoTaskExtension::class.java).isAppend = false
}

val sourceJar by tasks.creating(Jar::class) {
    from(java.sourceSets["main"].allSource)
    classifier = "sources"
}

val javadocJar by tasks.creating(Jar::class) {
    from("javadoc")
    classifier = "javadoc"
}

publishing {
    publications.create<MavenPublication>("mavenJava") {
        from(components["java"])

        artifact(sourceJar)
        artifact(javadocJar)

        pom {
            name.set(project.name)
            description.set(project.description)
            url.set("https://bitbucket.org/cthing/versionparser")
            licenses {
                license {
                    name.set("Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2")
                }
            }
            developers {
                developer {
                    id.set("baron")
                    name.set("Baron Roberts")
                    email.set("baron@cthing.com")
                    organization.set("C Thing Software")
                    organizationUrl.set("http://www.cthing.com")
                }
            }
            scm {
                connection.set("scm:git:git://bitbucket.org/cthing/versionparser.git")
                developerConnection.set("scm:git:ssh://bitbucket.org:cthing/versionparser")
                url.set("https://bitbucket.org/cthing/versionparser/src")
            }
        }
    }

    val repoUrl = if (isSnapshot) project.property("nexusSnapshotsUrl") else project.property("nexusCandidatesUrl")
    if (repoUrl != null) {
        repositories.maven {
            setUrl(repoUrl)
            credentials {
                username = project.properties["nexusUser"].toString()
                password = project.properties["nexusPassword"].toString()
            }
        }
    }
}

if (project.hasProperty("signing.keyId")
        && project.hasProperty("signing.password")
        && project.hasProperty("signing.secretKeyRingFile")) {
    signing {
        sign(publishing.publications["mavenJava"])
    }
}
