package polina4096.resquake

import kotlinx.serialization.*
import kotlinx.serialization.Transient
import kotlinx.serialization.json.*
import java.awt.Color
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.writeText

private val json = Json { prettyPrint = true }

@Serializable
class ReSquakeConfig(@Transient var path: Path? = null)
{
	/* General */
	// Movement
	var quakeMovementEnabled       : Boolean = DEFAULT_QUAKE_MOVEMENT_ENABLED
	
	// Miscellaneous
	var bufferedJump               : Boolean = DEFAULT_BUFFERED_JUMP
	var jumpParticles              : Int     = DEFAULT_JUMP_PARTICLES
	
	// Speed indicator
	var speedDeltaIndicatorEnabled : Boolean = DEFAULT_SPEED_DELTA_INDICATOR_ENABLED
	var speedDiffIndicatorEnabled  : Boolean = DEFAULT_SPEED_DIFF_INDICATOR_ENABLED
	var speedDeltaThreshold        : Double  = DEFAULT_SPEED_DELTA_THRESHOLD
	var speedGainColor             : Int     = SPEED_GAIN_COLOR      . rgb
	var speedLossColor             : Int     = SPEED_LOSS_COLOR      . rgb
	var speedUnchangedColor        : Int     = SPEED_UNCHANGED_COLOR . rgb
	
	/* Movement constants */
	// Bunnyhop
	var hardCapSpeed           : Double = DEFAULT_HARD_CAP_SPEED
	var acceleration           : Double = DEFAULT_ACCELERATION
	var airAcceleration        : Double = DEFAULT_AIR_ACCELERATION
	var maxAAccPerTick         : Double = DEFAULT_MAX_AACEL_PER_TICK
	
	fun save()
	{
		path!!.writeText(json.encodeToString(this))
	}
	
	companion object
	{
		/* General */
		// Movement
		const val DEFAULT_QUAKE_MOVEMENT_ENABLED        = true
		
		// Miscellaneous
		const val DEFAULT_UNCAPPED_BUNNYHOP             = true
		const val DEFAULT_NO_JUMP_COOLDOWN              = true
		const val DEFAULT_BUFFERED_JUMP                 = false
		const val DEFAULT_JUMP_PARTICLES                = 4
		
		// Speed indicator
		const val DEFAULT_SPEED_DELTA_INDICATOR_ENABLED = true
		const val DEFAULT_SPEED_DIFF_INDICATOR_ENABLED  = true
		const val DEFAULT_SPEED_DELTA_THRESHOLD         = 6.0
		/* s */ val SPEED_GAIN_COLOR                      = Color(0xFF_00FF00.toInt())
		/* a */ val SPEED_LOSS_COLOR                      = Color(0xFF_FF0000.toInt())
		/* d */ val SPEED_UNCHANGED_COLOR                 = Color(0xFF_FFFFFF.toInt())
		
		/* Movement constants */
		// Bunnyhop
		const val DEFAULT_HARD_CAP_SPEED           = 300.0
		const val DEFAULT_ACCELERATION             =  10.000
		const val DEFAULT_AIR_ACCELERATION         =  10.000
		const val DEFAULT_MAX_AACEL_PER_TICK       =   30.0
		
		fun load(path: Path): ReSquakeConfig
		{
			if (!path.exists())
			{
				return ReSquakeConfig(path)
			}
			
			val inputStream = Files.newInputStream(path)
			val inputString = inputStream.bufferedReader().use { it.readText() }
			return Json.decodeFromString<ReSquakeConfig>(inputString).also { it.path = path }
		}
	}
}
