package gamechaos.goldsqource

import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

object ReSquakeMod : ModInitializer
{
	const val ID   = "goldsqource"
	const val NAME = "goldsqource"
	
	val logger = LoggerFactory.getLogger(ID)
	
	lateinit var config: ReSquakeConfig
	
	override fun onInitialize()
	{
		val configDir = FabricLoader.getInstance().configDir
		config = ReSquakeConfig.load(configDir.resolve("$ID.json"))
		
		ReSquakeStats.register();
		
		logger.info("goldsqource initialized!")
	}
}
