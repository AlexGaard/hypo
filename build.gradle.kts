val sl4jVersion = "2.0.7"
val junitVersion = "5.9.2"

plugins {
	id("java")
}

group = "com.github.alexgaard"
version = "1.0-SNAPSHOT"

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

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(11))
	}
}