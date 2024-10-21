plugins {
    `java-library`
    checkstyle
    alias(libs.plugins.dependencyAnalysis)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

dependencies {
    api(project(":"))

    implementation(libs.assertJ)
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

listOf("CalVerExample",
       "GradleExample",
       "JavaVersionExample",
       "MavenExample",
       "NpmExample",
       "RubyGemsExample").forEach { example ->
    tasks.register("run$example", JavaExec::class) {
        group = "Example"
        description = "Run $example program"
        mainClass = "org.cthing.versionparser.examples.$example"
        classpath = sourceSets["main"].runtimeClasspath
    }
}
