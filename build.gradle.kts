import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.LinkMapping
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

version = "0.1.0"
group = "com.dmdirc.edgar"

plugins {
    `maven-publish`
    jacoco
    kotlin("jvm") version "1.3.21"
    id("com.jfrog.bintray") version "1.8.4"
    id("org.jetbrains.dokka") version "0.9.17"
    id("name.remal.check-updates") version "1.0.113"
}

jacoco {
    toolVersion = "0.8.3"
}

configurations {
    all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion("1.3.21")
            }
        }
    }
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8", "1.3.21"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.4.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.4.0")
    testImplementation("io.mockk:mockk:1.9.1")
    testImplementation("com.google.jimfs:jimfs:1.1")
    testRuntime("org.junit.jupiter:junit-jupiter-engine:5.4.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    create<Jar>("sourceJar") {
        description = "Creates a JAR that contains the source code."
        from(sourceSets["main"].allSource)
        archiveClassifier.set("sources")
    }

    create<JacocoReport>("codeCoverageReport") {
        executionData(fileTree(project.rootDir.absolutePath).include("**/build/jacoco/*.exec"))

        sourceSets(sourceSets["main"])

        reports {
            xml.isEnabled = true
            xml.destination = File("$buildDir/reports/jacoco/report.xml")
            html.isEnabled = true
            csv.isEnabled = false
        }

        dependsOn("test")
    }

    withType<Wrapper> {
        gradleVersion = "5.2.1"
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    withType<DokkaTask> {
        moduleName = "edgar"
        linkMappings = arrayListOf(LinkMapping().apply {
            dir = "src/main/edgar"
            url = "https://github.com/csmith/edgar/blob/master/src/main/kotlin"
            suffix = "#L"
        })
    }
}

publishing {
    publications {
        create<MavenPublication>("Publication") {
            groupId = "com.dmdirc"
            artifactId = "edgar"
            version = project.version as String
            artifact(tasks["jar"])
            artifact(tasks["sourceJar"])
            pom.withXml {
                val root = asNode()
                root.appendNode("name", "Edgar")
                root.appendNode("description", "Kotlin library for reading gettext PO files")

                val dependenciesNode = root.appendNode("dependencies")
                configurations.implementation.get().allDependencies.forEach {
                    val dependencyNode = dependenciesNode.appendNode("dependency")
                    dependencyNode.appendNode("groupId", it.group)
                    dependencyNode.appendNode("artifactId", it.name)
                    dependencyNode.appendNode("version", it.version)
                }
            }
        }
    }
}

bintray {
    user = System.getenv("BINTRAY_USER")
    key = System.getenv("BINTRAY_API_KEY")
    setPublications("Publication")
    with(pkg) {
        userOrg = "dmdirc"
        repo = "releases"
        name = "edgar"
        publish = true
        desc = "A kotlin library for reading gettext PO files"
        setLicenses("MIT")
        vcsUrl = "https://github.com/csmith/edgar"
    }
}
