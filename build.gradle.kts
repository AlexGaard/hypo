val sl4jVersion = "2.0.7"
val junitVersion = "5.9.2"

plugins {
	`java-library`
	`maven-publish`
}

group = "com.github.alexgaard"
version = project.property("release_version") ?: throw IllegalStateException("release_version is missing")

repositories {
	mavenCentral()
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

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(11))
	}

	withSourcesJar()
	withJavadocJar()
}