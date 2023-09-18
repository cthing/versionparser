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
