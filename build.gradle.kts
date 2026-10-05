plugins {
	// This plugin applies the correct loom variant based on the Minecraft version
	id("dev.kikugie.loom-back-compat")
}

val requiredJava: JavaVersion = when {
	sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
	sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
	sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
	sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
	else -> JavaVersion.VERSION_1_8
}

version = "${property("mod.version")}+${sc.current.version}"
// base.archivesName = property("mod.id") as String

repositories {
	/**
	 * Restricts dependency search of the given [groups] to the [maven URL][url],
	 * improving the setup speed.
	 */
	fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
		forRepository { maven(url) { name = alias } }
		filter { groups.forEach(::includeGroup) }
	}
	strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
	strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
	strictMaven("https://maven.terraformersmc.com/releases/", "Terraformersmc", "com.terraformersmc")
	strictMaven("https://maven.shedaniel.me/", "Shedaniel", "me.shedaniel.cloth")
}

loom {
	splitEnvironmentSourceSets()

	mods {
		register("goldsqource") {
			sourceSet(sourceSets.main.get())
			sourceSet(sourceSets.getByName("client"))
		}
	}

	fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json") // Useful for interface injection

	decompilerOptions.named("vineflower") {
		options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
	}

	runConfigs.all {
		preferGradleTask = true
		generateRunConfig = true
		runDirectory = rootProject.file("run") // Shares the run directory between versions
		jvmArguments.add("-Dmixin.debug.export=true") // Exports transformed classes for debugging
	}

}

dependencies {
	/**
	 * Fetches only the required Fabric API modules to not waste time downloading all of them for each version.
	 * @see <a href="https://github.com/FabricMC/fabric">List of Fabric API modules</a>
	 */
	fun fapi(vararg modules: String) {
		for (it in modules) modImplementation(fabricApi.module(it, sc.properties["deps.fabric_api"]))
	}

	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${sc.current.version}")
	// Applies Mojang Mappings on obfuscated versions
	loomx.applyMojangMappings()

	// Use `mod{dependency type}` even on 26.1+ - loom-back-compat converts them
	modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
	modApi("me.shedaniel.cloth:cloth-config-fabric:${property("deps.cloth_config")}") {
		exclude("net.fabricmc.fabric-api")
	}
	modApi("com.terraformersmc:modmenu:${property("deps.modmenu")}")
	fapi("fabric-lifecycle-events-v1", "fabric-resource-loader-v0", "fabric-content-registries-v0", "fabric-registry-sync-v0", "fabric-key-binding-api-v1", "fabric-screen-api-v1")
}

/*
tasks.withType(JavaCompile).configureEach {
	it.options.release = 21
}
*/
java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()
	sourceCompatibility = requiredJava
	targetCompatibility = requiredJava
	toolchain {
		vendor = JvmVendorSpec.ADOPTIUM
		languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
	}
}

tasks {
	processResources {
		fun MutableMap<String, String>.register(key: String, value: String) {
			inputs.property(key, value)
			set(key, value)
		}

		val props = buildMap {
			register("id", sc.properties["mod.id"])
			register("name", sc.properties["mod.name"])
			register("version", sc.properties["mod.version"])
			register("minecraft", sc.properties["mod.mc_compat"])
			register("loader", Regex("\\d+\\.\\d+").find(sc.properties.get<String>("deps.fabric_loader"))!!.value)
			register("java", "${requiredJava.majorVersion}")
			register("modmenu", sc.properties["mod.mc_compat"])
		}

		filesMatching("fabric.mod.json") { expand(props) }

		val mixinJava = "JAVA_${requiredJava.majorVersion}"
		filesMatching("*.mixins.json") { expand("java" to mixinJava) }
	}

	withType<Jar> {
		val projectName = project.property("mod.id")
		inputs.property("mod_id", name)
		from("../../LICENSE") { rename { "$it-$name" } }
	}
}
