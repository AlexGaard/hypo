val sl4jVersion = "2.0.7"
val junitVersion = "5.9.2"

plugins {
	jacoco
	`java-library`
	`maven-publish`
	id("org.sonarqube") version "4.0.0.2929"
}

group = "com.github.alexgaard"
version = project.property("release_version") ?: throw IllegalStateException("release_version is missing")

repositories {
	mavenCentral()
}

sonarqube {
	properties {
		property("sonar.projectKey", "hypo")
		property("sonar.organization", "alexgaard")
		property("sonar.host.url", "https://sonarcloud.io")
	}
}

dependencies {
	implementation("org.slf4j:slf4j-api:$sl4jVersion")

	testImplementation("org.slf4j:slf4j-simple:$sl4jVersion")
	testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
	testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks.getByName<Test>("test") {
	useJUnitPlatform()
}

tasks.register("getVersion") {
	print(version)
}

tasks.test {
	finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
}

tasks.jacocoTestReport {
	reports {
		xml.required.set(true)
		html.outputLocation.set(layout.buildDirectory.dir("jacocoHtml"))
	}
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(11))
	}

	withSourcesJar()
	withJavadocJar()
}

publishing {
	publications {
		create<MavenPublication>("maven") {
			groupId = "com.github.alexgaard"
			artifactId = "hypo"
			version = project.version.toString()

			from(components["java"])

			pom {
				name.set("Hypo")
				description.set("Hypo - zero-overhead dependency injection without reflection")
				url.set("https://github.com/AlexGaard/hypo")
				developers {
					developer {
						id.set("alexgaard")
						name.set("Alexander Gård")
						email.set("alexander.olav.gaard@gmail.com")
					}
				}
				licenses {
					license {
						name.set("MIT License")
						url.set("https://github.com/AlexGaard/hypo/blob/main/LICENSE")
					}
				}
				scm {
					connection.set("scm:git:git://github.com/alexgaard/hypo.git")
					developerConnection.set("scm:git:ssh://github.com/alexgaard/hypo.git")
					url.set("https://github.com/AlexGaard/hypo")
				}
			}
		}
	}
}