package gamechaos.goldsqource

import net.minecraft.block.BlockRenderType
import net.minecraft.block.Blocks
import net.minecraft.block.PowderSnowBlock
import net.minecraft.block.LadderBlock
import net.minecraft.entity.Entity
import net.minecraft.entity.Flutterer
import net.minecraft.entity.MovementType
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.particle.BlockStateParticleEffect
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkSectionPos
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Direction
import net.minecraft.text.Text
import net.minecraft.state.property.EnumProperty
import net.minecraft.state.property.Properties
import kotlin.math.*


object MvPlayer
{
	private var baseVelocities = mutableListOf<Pair<Double, Double>>()
	private const val TO_QUAKE = 40.0 // 72 / 1.8 (player height in source divided by player height in minecraft)
	private const val FROM_QUAKE = 1.0 / 40.0
	private const val TICKRATE = 20.0
	private const val FRAMETIME = 1.0 / TICKRATE
	private const val FAKE_FRAMETIME = 1.0 / 100.0 // for simulating 100 tick airacceleration
	private const val MAX_CLIMB_SPEED = 200.0 // ladder climbing speed
	private const val CLIMB_JUMPOFF_SPEED = 270.0
	
	var previousYaw   : Float  = 0.0f
	var previousSpeed : Double  = 0.0
	var currentSpeed  : Double  = 0.0
	var jumping	   : Boolean = false
	var jumped        : Boolean = false
	var swimming	  : Boolean = false
	
	private fun collectSpeed(speed: Double)
	{
		previousSpeed = currentSpeed
		currentSpeed  = speed
	}
	
	// API
	fun updateVelocity(player: Entity, speed: Float, sidemove: Double, forwardmove: Double): Boolean
	{
		// When the bunnyhop is enabled
		if (!MvMod.config.quakeMovementEnabled) return false
		
		if (player !is PlayerEntity)
		{
			return false // We are only interested in players
		}
		if (!player.world.isClient)
		{
			return false // And only in the client player
		}
		
		// And only on land
		if ((player.abilities.flying && player.vehicle == null)
			|| player.isTouchingWater
			|| player.isInLava
			|| player.isClimbing)
		{
			return false
		}
		
		val wishspeed = speed.toDouble() * 2.15
		val wishdir = player.getMovementDirection(sidemove, forwardmove)
		val wishvel = Pair(wishdir.first * wishspeed, wishdir.second * wishspeed)
		baseVelocities.add(wishvel)
		
		return true
	}
	
	fun afterJump(player: PlayerEntity)
	{
		if (player.world.isClient && MvMod.config.quakeMovementEnabled)
		{
			if (player.isSprinting)
			{
				val f = player.yaw * 0.017453292f
				val xVel = player.velocity.x + sin(f) * 0.2
				val zVel = player.velocity.z - cos(f) * 0.2
				player.velocity = Vec3d(xVel, player.velocity.y, zVel)
			}
			
			player.applyHardCap()
			player.spawnBunnyhopParticles(MvMod.config.jumpParticles)
			jumped = true
		}
	}
	
	fun travel(player: PlayerEntity, movementInput: Vec3d): Boolean
	{
		if (!MvMod.config.quakeMovementEnabled
			||  !player.world.isClient
			||   player.abilities.flying
			||   player.isGliding
			||   player.vehicle != null)
		{
			return false
		}
		
		// Update last recorded speed
		val speed = player.getSpeed()
		collectSpeed(speed)
		
		val preX = player.x
		val preY = player.y
		val preZ = player.z
		if (player.travelQuake(movementInput.x, movementInput.z))
		{
			val distance =
				((player.x - preX).pow(2) +
				(player.y - preY).pow(2) +
				(player.z - preZ).pow(2)).pow(1.0 / 2.0)
			
			val flying = (player.abilities.flying || player.isGliding)
			
			// Apparently stats are stored with 2-digit fixed point precision
			if (player is ServerPlayerEntity)
			{
				if (player.isTouchingWater && !flying)
				{
					println((distance * 100.0).roundToInt())
					player.increaseStat(MvStats.SHARK_ONE_CM, (distance * 100.0).roundToInt())
				}
				else
				{
					println((distance * 100.0).roundToInt())
					player.increaseStat(MvStats.BHOP_ONE_CM, (distance * 100.0).roundToInt())
				}
			}
			
			// Swing arms and legs
			player.updateLimbs(player is Flutterer)
			previousYaw = player.yaw
			return true
		}
		previousYaw = player.yaw
		
		jumping = false
		return false
	}
	
	fun beforeTick(player: PlayerEntity)
	{
		if (player.world.isClient && baseVelocities.isNotEmpty())
		{
			baseVelocities.clear()
		}
	}
	
	// Quake movement
	private fun PlayerEntity.getMovementDirection(sidemoveInitial: Double, forwardmoveInitial: Double): Pair<Double, Double>
	{
		val preSpeed = sidemoveInitial * sidemoveInitial + forwardmoveInitial * forwardmoveInitial
		if (preSpeed >= 0)
		{
			var speed = sqrt(preSpeed)
			
			speed = if (speed <= 0.0) 1.0
			else			 1.0 / speed
			
			val sidemove	= sidemoveInitial	* speed
			val forwardmove = forwardmoveInitial * speed
			val f1 = sin(this.yaw * Math.PI / 180.0)
			val f2 = cos(this.yaw * Math.PI / 180.0)
			
			return Pair(
				(sidemove	* f2 - forwardmove * f1),
				(forwardmove * f2 + sidemove	* f1),
			)
		}
		
		return Pair(0.0, 0.0)
	}
	
	private fun PlayerEntity.toRadians(degrees: Double): Double = degrees / 180.0f * PI
	private fun PlayerEntity.toDegrees(radians: Double): Double = radians * 180.0f / PI
	
	private fun PlayerEntity.anglesToVectors(pitch: Double, yaw: Double): Pair<Vec3d, Vec3d>
	{
		var radAngles = Vec3d(
			this.toRadians(pitch),
			this.toRadians(yaw),
			0.0
		);
		
		var cosPitch = cos(radAngles.x);
		var sinPitch = sin(radAngles.x);
		
		var cosYaw = cos(radAngles.y);
		var sinYaw = sin(radAngles.y);
		
		var forwards = Vec3d(cosPitch * -sinYaw, -sinPitch, cosPitch * cosYaw)
		var left = Vec3d(cosYaw, 0.0, sinYaw)
		return Pair(forwards, left)
	}

	private const val QUAKE_MOVEMENT_SPEED_MULTIPLIER = 2.15
	private const val QUAKE_SNEAKING_SPEED_MULTIPLIER = 0.65
	private fun PlayerEntity.getSlipperiness(): Double
	{
		if (this.isOnGround)
		{
			val groundPos = BlockPos.ofFloored(this.x, this.boundingBox.minY - 1, this.z)
			return this.world.getBlockState(groundPos).block.slipperiness.toDouble()
		}

		return 0.0
	}
	
	private fun PlayerEntity.getBaseSpeedCurrent(): Double
	{
		var result: Double = this.movementSpeed.toDouble()
		if (!this.isSneaking && !this.isInSneakingPose && !this.isInSwimmingPose)
		{
			result *= QUAKE_MOVEMENT_SPEED_MULTIPLIER
		}
		else
		{
			result *= QUAKE_SNEAKING_SPEED_MULTIPLIER
		}
		
		if (this.isUsingItem())
		{
			result *= 0.2
		}
		return result
	}
	
	private fun PlayerEntity.getBaseSpeedMax(): Double
	{
		val baseSpeed = this.movementSpeed
		return baseSpeed * QUAKE_MOVEMENT_SPEED_MULTIPLIER
	}
	
	private fun PlayerEntity.getSpeed(): Double
	{
		val x = this.velocity.x
		val z = this.velocity.z
		return sqrt((x * x + z * z))
	}
	
	private fun PlayerEntity.travelQuake(sidemove: Double, forwardmove: Double): Boolean
	{
		val flying = (this.abilities.flying || this.isGliding)
		if (this.isInLava && !flying)
		{
			return false // Swimming in lava
		}
		
		// Collect all relevant movement values
		val wishdir = this.getMovementDirection(sidemove, forwardmove)
		val wishspeed = if (sidemove != 0.0 || forwardmove != 0.0) this.getBaseSpeedCurrent() else 0.0
		val onGroundForReal = this.isOnGround && !jumping
		
		// Sharking
		if (this.isTouchingWater && !flying)
		{
			return false // Use default minecraft water movement
		}
		else
		{
			swimming = false
		}
		
		if (this.isClimbing())
		{
			// laddermove!
			var blockState = this.getWorld().getBlockState(this.getClimbingPos().orElse(null))
			if (blockState == null || !blockState.isOf(Blocks.LADDER))
			{
				return false;
			}
			var ladderFacing: Direction = blockState.get(LadderBlock.FACING)
			var ladderNormal = ladderFacing.getDoubleVector()
				
			var forward = 0.0
			var left = 0.0
			//var vpn: Vec3d;
			//var v_left: Vec3d;
			var speed = MAX_CLIMB_SPEED * FROM_QUAKE * FRAMETIME
			
			if (speed > this.getBaseSpeedMax())
			{
				speed = this.getBaseSpeedMax();
			}
			/*
			if (forwardmove < 0.0)
			{
				forward -= speed;
			}
			if (forwardmove > 0.0)
			{
				forward += speed
			}
			if (sidemove < 0.0)
			{
				left -= speed
			}
			if (sidemove > 0.0)
			{
				left += speed
			}
			*/
			val viewVectors = this.anglesToVectors(this.getPitch().toDouble(), this.getYaw().toDouble());
			val vecForward = viewVectors.first;
			val vecLeft = viewVectors.second;
			//this.sendMessage(Text.translatable("%f %f %f   %f %f %f".format(vecForward.x, vecForward.y, vecForward.z, vecLeft.x, vecLeft.y, vecLeft.z)), false)
			//this.sendMessage(Text.translatable("%f %f".format(sidemove, forwardmove)), false)
			//this.sendMessage(Text.translatable("%f %f %f".format(ladderNormal.x, ladderNormal.y, ladderNormal.z)), false)
			
			if (jumping)
			{
				// jump off
				this.velocity = ladderNormal.multiply(CLIMB_JUMPOFF_SPEED * FROM_QUAKE * FRAMETIME)
			}
			else
			{
				// move on ladder
				if (forwardmove != 0.0 || sidemove != 0.0)
				{
					var velocity = vecForward.multiply(forwardmove * speed);
					velocity = velocity.add(vecLeft.multiply(sidemove * speed));
					
					var tmp = Vec3d(0.0, 1.0, 0.0)
					var perp = tmp.crossProduct(ladderNormal);
					perp = perp.normalize();
					
					var normal = velocity.dotProduct(ladderNormal);
					var cross = ladderNormal.multiply(normal);
					
					var lateral = velocity.subtract(cross);
					
					tmp = ladderNormal.crossProduct(perp);
					// CUSTOM: add a little speed that moves the player into the ladder so that
					//  they don't stick out a whole block
					var wishdirVec = Vec3d(wishdir.first, 0.0, wishdir.second)
					var movingAwayFromLadder = ladderNormal.dotProduct(wishdirVec) > 0;
					if (!this.isOnGround || !movingAwayFromLadder)
					{
						lateral = lateral.add(ladderNormal.multiply(-MAX_CLIMB_SPEED * FROM_QUAKE * FRAMETIME))
					}
					this.velocity = lateral.add(tmp.multiply(-normal))
						
					// allow players to move away from ladder when on ground
					if (this.isOnGround && movingAwayFromLadder)
					{
						this.velocity = this.velocity.add(ladderNormal.multiply(MAX_CLIMB_SPEED * FROM_QUAKE * FRAMETIME))
					}
				}
				else
				{
					this.velocity = Vec3d(0.0, 0.0, 0.0)
				}
			}
		}
		else if (onGroundForReal)
		{
			// Ground movement
			val slipperiness = this.getSlipperiness()
			val xVel = this.velocity.x * slipperiness
			val zVel = this.velocity.z * slipperiness
			this.velocity = Vec3d(xVel, this.velocity.y, zVel)
			
			if (wishspeed != 0.0)
			{
				// Alter based on the surface friction
				val acceleration = MvMod.config.acceleration * (0.16277136 / (slipperiness * slipperiness * slipperiness))
				this.accelerate(wishspeed, wishdir.first, wishdir.second, acceleration, slipperiness)
			}
			
			if (baseVelocities.isNotEmpty())
			{
				var x = this.velocity.x
				var z = this.velocity.z
				val speedMod = wishspeed / this.getBaseSpeedMax()
				
				// add in base velocities
				for (baseVel in baseVelocities)
				{
					x += baseVel.first  * speedMod
					z += baseVel.second * speedMod
				}
				
				this.velocity = Vec3d(x, this.velocity.y, z)
			}
		}
		else // Air movement
		{
			val airAcceleration = MvMod.config.airAcceleration
			// simulate 100 tickrate airacceleration
			val realYaw = this.yaw
			var savedYaw = this.yaw
			if (savedYaw - previousYaw > 180.0f)
			{
				savedYaw -= 360.0f
			}
			else if (savedYaw - previousYaw < -180)
			{
				savedYaw += 360.0f
			}
			
			var airWishspeed = wishspeed * (250.0 / 176.0)
			// fix higher airaccel with w/wd/wa airstrafing
			if (airWishspeed > 250.0 * FROM_QUAKE * FRAMETIME)
			{
				airWishspeed = 250.0 * FROM_QUAKE * FRAMETIME
			}
			
			for (i in 1..5)
			{
				this.yaw = lerp(previousYaw.toDouble(), savedYaw.toDouble(), i.toDouble() / 5.0).toFloat()
				val wishdirAir = this.getMovementDirection(sidemove, forwardmove)
//				 this.sendMessage(Text.translatable("%.2f %.2f ".format(wishdirAir.first, wishdirAir.second)))
				this.airAccelerate(airWishspeed, wishdirAir.first, wishdirAir.second, airAcceleration)
			}
			this.yaw = realYaw
		}
		
		// Apply velocity
		this.move(MovementType.SELF, this.velocity)
		
		// stick to ground, aka ledgegrab/glidestep
		val list = this.getWorld().getEntityCollisions(null, this.getBoundingBox().stretch(movement))
		val down = -(4.0 * FROM_QUAKE)
		val movement: Vec3d = Entity.adjustMovementForCollisions(null, Vec3d(0.0, down, 0.0), this.getBoundingBox(), this.getWorld(), list)
		if (movement.y > down
			&& !this.isOnGround
			&& this.velocity.y * TICKRATE * TO_QUAKE < 200.0
			&& this.velocity.y >= 0.0) // don't need to ledgegrab if falling down
		{
			val pos = this.getPos()
			this.setPosition(pos.x, pos.y + movement.y, pos.z)
			this.velocity = Vec3d(this.velocity.x, 0.0, this.velocity.z)
			this.setOnGround(true)
		}
		
		if (!this.isClimbing)
		{
			// HL2 code applies half gravity before acceleration and half after acceleration, but this seems to work fine
			this.applyGravity()
		}
		
		// Cancel default minecraft movement behavior
		return true
	}
	
	private fun lerp(a: Double, b: Double, t: Double): Double
	{
		return (1.0 - t) * a + b * t
	}

	private fun PlayerEntity.applyGravity()
	{
		val levitating = hasStatusEffect(StatusEffects.LEVITATION)
		
		var yVel = this.velocity.y
		var gravity = -0.08 // gravity
		
		// Powdered snow
		if ((this.horizontalCollision || jumping)
			//&& (this.isClimbing || blockStateAtPos.isOf(Blocks.POWDER_SNOW)
			&& (blockStateAtPos.isOf(Blocks.POWDER_SNOW)
			&& PowderSnowBlock.canWalkOnPowderSnow(this)))
		{
			yVel += 0.2
		}
		
		// Slow falling
		if (velocity.y <= 0.0 && hasStatusEffect(StatusEffects.SLOW_FALLING))
		{
			gravity = -0.01
			this.onLanding()
		}
		
		// Levitation
		if (levitating)
		{
			yVel += (FRAMETIME * (getStatusEffect(StatusEffects.LEVITATION)!!.amplifier + 1).toDouble() - yVel) * 0.2
			onLanding()
		}
		
		// Apply gravity
		if (!world.isClient || world.chunkManager.isChunkLoaded(ChunkSectionPos.getSectionCoord(blockPos.x), ChunkSectionPos.getSectionCoord(blockPos.z)))
		{
			if (!hasNoGravity() && !levitating)
			{
				yVel += gravity
			}
			
			val airResistance = 0.9800000190734863
			this.velocity = Vec3d(this.velocity.x, yVel * airResistance, this.velocity.z)
		}
		else // If chunk is not loaded slowly fall to bottomY
		{
			yVel = if (this.y > world.bottomY.toDouble()) -0.1 else 0.0
			this.velocity = Vec3d(this.velocity.x, yVel, this.velocity.z)
		}
	}

	private fun PlayerEntity.accelerate(wishspeed: Double, wishX: Double, wishZ: Double, acceleration: Double, slipperiness: Double)
	{
		// Determine veer amount; this is a dot product
		val currentSpeed = this.velocity.x * wishX + this.velocity.z * wishZ
		// Speed delta
		val addSpeed = wishspeed - currentSpeed
		
		// If not adding any, done
		if (addSpeed <= 0) return
		
		// Determine acceleration speed after acceleration
		var accelSpeed = acceleration * wishspeed / slipperiness * FRAMETIME
		if (accelSpeed > addSpeed) accelSpeed = addSpeed
		
		// Adjust move velocity
		val x = (this.velocity.x + accelSpeed * wishX)
		val z = (this.velocity.z + accelSpeed * wishZ)
		this.velocity = Vec3d(x, this.velocity.y, z)
	}
	
	private fun PlayerEntity.airAccelerate(wishspeedInitial_: Double, wishX: Double, wishZ: Double, accel: Double)
	{
		val maxAirAcceleration = MvMod.config.maxAAccPerTick
		// velocity is per-tick in minecraft, not per-second like in source/quake. AAAAAAAAAAAAAAAAa
		var wishspeedInitial: Double = wishspeedInitial_ * TO_QUAKE * TICKRATE
		val wishspeed = if (wishspeedInitial > maxAirAcceleration) maxAirAcceleration else wishspeedInitial
		
		// Determine veer amount; this is a dot product
		val currentSpeed = (this.velocity.x * TO_QUAKE * TICKRATE) * wishX + (this.velocity.z * TO_QUAKE * TICKRATE) * wishZ
		
		// Speed delta
		val addSpeed = wishspeed - currentSpeed
		
		// If not adding any, done
		if (addSpeed <= 0)
		{
			return
		}
		
		// Determine acceleration speed after acceleration
		var accelSpeed = accel * wishspeedInitial * FAKE_FRAMETIME
		if (accelSpeed > addSpeed) accelSpeed = addSpeed
		
		// Adjust move velocity
		val x = (this.velocity.x * TO_QUAKE * TICKRATE) + accelSpeed * wishX
		val z = (this.velocity.z * TO_QUAKE * TICKRATE) + accelSpeed * wishZ
		this.velocity = Vec3d(x * FROM_QUAKE * FRAMETIME, this.velocity.y, z * FROM_QUAKE * FRAMETIME)
	}
	
	private fun PlayerEntity.applyHardCap()
	{
		val hardCap = MvMod.config.hardCapSpeed * FROM_QUAKE * FRAMETIME
		val speed = this.getSpeed()
		
		if (speed > hardCap && hardCap != 0.0)
		{
			val multiplier = hardCap / speed
			val xVel = this.velocity.x * multiplier
			val zVel = this.velocity.z * multiplier
			this.velocity = Vec3d(xVel, this.velocity.y, zVel)
		}
	}

	// Particles
	private fun PlayerEntity.spawnBunnyhopParticles(numParticles: Int)
	{
		if (numParticles < 1)
		{
			return
		}
		
		// taken from sprint
		val i = floor(this.x                      ).toInt()
		val j = floor(this.y - 0.20000000298023224).toInt()
		val k = floor(this.z                      ).toInt()
		
		val blockState = this.world.getBlockState(BlockPos(i, j, k))
		if (blockState.renderType != BlockRenderType.INVISIBLE)
		{
			for (iParticle in 0 until numParticles)
			{
				val x = this.x + (this.random.nextFloat() - 0.5) * this.width
				val z = this.z + (this.random.nextFloat() - 0.5) * this.width
				val y = this.boundingBox.minY + 0.1
				
				val xVel = -this.velocity.x * 4.0
				val zVel = -this.velocity.z
				val yVel = 1.5
				
				val effect = BlockStateParticleEffect(ParticleTypes.BLOCK, blockState)
				this.world.addParticle(effect, x, y, z, xVel, yVel, zVel)
			}
		}
	}
}
