package gamechaos.goldsqource

import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory

object MvMod : ModInitializer
{
	const val ID   = "goldsqource"
	const val NAME = "goldsqource"
	
	val logger = LoggerFactory.getLogger(ID)
	
	lateinit var config: MvConfig
	
	override fun onInitialize()
	{
		val configDir = FabricLoader.getInstance().configDir
		config = MvConfig.load(configDir.resolve("$ID.json"))
		
		MvStats.register();
		
		logger.info("goldsqource initialized!")
	}
}
