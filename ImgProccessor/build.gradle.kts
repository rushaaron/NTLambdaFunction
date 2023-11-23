plugins {
    id("java")
    id ("com.github.johnrengelman.shadow") version ("8.1.1")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation ("com.amazonaws:aws-java-sdk-lambda:1.12.592")
    implementation ("com.amazonaws:aws-lambda-java-core:1.2.3")
    implementation ("com.amazonaws:aws-lambda-java-events:3.11.3")
    implementation ("com.amazonaws:aws-java-sdk-core:1.12.592")
    implementation ("com.amazonaws:aws-java-sdk-s3:1.12.592")
    implementation ("software.amazon.awssdk:bom:2.21.28")
    implementation ("javax.xml.bind:jaxb-api:2.2.4")
    implementation ("com.fasterxml.jackson.core:jackson-databind:2.16.0")
}

buildscript {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("com.github.johnrengelman:shadow:8.1.1")
    }
}

apply(plugin = "com.github.johnrengelman.shadow")

tasks.test {
    useJUnitPlatform()
}
