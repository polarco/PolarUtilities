plugins {
    java
}

group = "br.com.polarutilities"
version = providers.fileContents(layout.projectDirectory.file("VERSION"))
    .asText
    .map { it.trim() }
    .get()

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.60-stable")
}

java {
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveBaseName.set("PolarUtilities")
}
