package gamechaos.goldsqource

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.stats.StatFormatter
import net.minecraft.stats.Stats
import net.minecraft.resources.Identifier

object MvStats
{
	val BHOP_ONE_CM = Identifier.fromNamespaceAndPath(MvMod.ID, "bhop_one_cm")
	val SHARK_ONE_CM = Identifier.fromNamespaceAndPath(MvMod.ID, "shark_one_cm")
	val TRIMPS = Identifier.fromNamespaceAndPath(MvMod.ID, "trimps")
	
	fun register()
	{
		// Apparently, stats don't work. I will fix them one day.
		// registerStat(BHOP_ONE_CM,  "bhop_one_cm",  StatFormatter.DISTANCE)
		// registerStat(SHARK_ONE_CM, "shark_one_cm", StatFormatter.DISTANCE)
		// registerStat(TRIMPS,       "trimps",       StatFormatter.DEFAULT)
	}
	
	private fun registerStat(key: Identifier, id: String, formatter: StatFormatter)
	{
		Registry.register(BuiltInRegistries.CUSTOM_STAT, id, key)
		Stats.CUSTOM.get(key, formatter)
	}
}
