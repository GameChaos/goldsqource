package gamechaos.goldsqource

import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.stat.StatFormatter
import net.minecraft.stat.Stats
import net.minecraft.util.Identifier

object MvStats
{
	val BHOP_ONE_CM = Identifier.of(MvMod.ID, "bhop_one_cm")
	val SHARK_ONE_CM = Identifier.of(MvMod.ID, "shark_one_cm")
	val TRIMPS = Identifier.of(MvMod.ID, "trimps")
	
	fun register()
	{
		// Apparently, stats don't work. I will fix them one day.
		// registerStat(BHOP_ONE_CM,  "bhop_one_cm",  StatFormatter.DISTANCE)
		// registerStat(SHARK_ONE_CM, "shark_one_cm", StatFormatter.DISTANCE)
		// registerStat(TRIMPS,       "trimps",       StatFormatter.DEFAULT)
	}
	
	private fun registerStat(key: Identifier, id: String, formatter: StatFormatter)
	{
		Registry.register(Registries.CUSTOM_STAT, id, key)
		Stats.CUSTOM.getOrCreateStat(key, formatter)
	}
}
