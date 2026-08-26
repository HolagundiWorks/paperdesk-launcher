plugins {
    kotlin("jvm") version "2.0.21"
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

application {
    mainClass.set("com.paperdesk.companion.MainKt")
}

dependencies {
    implementation("org.json:json:20240303")
}

tasks.named<JavaExec>("run") {
    workingDir = layout.projectDirectory.asFile
    standardInput = System.`in`
}
