plugins {
    id("java")
}

group = "io.github.gaming32"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.fabricmc:mapping-io:0.6.1")
    compileOnly("org.jetbrains:annotations:26.0.1")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "io.github.gaming32.awremap.Main"
    }
    from(configurations.runtimeClasspath.get().files.map { if (it.isDirectory) it else zipTree(it) })
}
