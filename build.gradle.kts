plugins {
    kotlin("jvm") version "1.7.21"
    id("kr.entree.spigradle") version "2.4.3"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "pe.chalk.bukkit"
version = "1.0.4"

java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("org.spigotmc", "spigot-api", "1.19.3-R0.1-SNAPSHOT")
    compileOnly("com.github.chalkpe", "ChestOverflow", "v2.3.12")
    implementation("org.bstats", "bstats-bukkit", "3.0.0")
    implementation("io.javalin", "javalin", "5.3.2")
    implementation("com.fasterxml.jackson.core", "jackson-databind", "2.14.0")
}

spigot {
    apiVersion = "1.19"
    description = "Inventory overlay"
    main = "pe.chalk.bukkit.inveitoryoverlay.InventoryOverlay"
    softDepends = listOf("ChestOverflow")
    commands {
        create("overlay") { description = "Open web link of inventory overlay" }
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("org.bstats", "pe.chalk.bukkit.bstats")
}
