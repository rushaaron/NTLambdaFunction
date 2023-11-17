plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation ("com.amazonaws:aws-java-sdk-s3:1.12.583")
    implementation ("com.amazonaws:aws-java-sdk-lambda:1.12.583")
    implementation ("com.amazonaws:aws-lambda-java-core:1.2.1")
    implementation ("com.amazonaws:aws-lambda-java-events:3.11.1")
    implementation ("software.amazon.awssdk:s3:2.21.17")
}

tasks.test {
    useJUnitPlatform()
}