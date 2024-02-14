plugins {
    `java-library`
    checkstyle
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

dependencies {
    implementation(rootProject)
    implementation(libs.assertJ)
}

configurations.checkstyle {
    resolutionStrategy.capabilitiesResolution.withCapability("com.google.collections:google-collections") {
        select("com.google.guava:guava:0")
    }
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    isIgnoreFailures = false
    configFile = file("../dev/checkstyle/checkstyle.xml")
    configDirectory = file("../dev/checkstyle")
    isShowViolations = true
}

tasks {
    withType<JavaCompile> {
        options.release = libs.versions.java.get().toInt()
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-options", "-Werror"))
    }
}

listOf("CalVerExample", "GradleExample", "MavenExample", "NpmExample", "RubyGemsExample").forEach { example ->
    tasks.register("run$example", JavaExec::class) {
        group = "Example"
        description = "Run $example program"
        mainClass = "org.cthing.versionparser.examples.$example"
        classpath = sourceSets["main"].runtimeClasspath
    }
}
