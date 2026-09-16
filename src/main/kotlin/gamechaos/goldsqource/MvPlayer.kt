package gamechaos.goldsqource

import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.PowderSnowBlock
import net.minecraft.world.level.block.LadderBlock
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.MoverType
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.player.Player
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.phys.Vec3
import net.minecraft.core.Direction
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
		
		if (player !is Player)
		{
			return false // We are only interested in players
		}
		val world = player.level();
		if (!world.isClientSide)
		{
			return false // And only in the client player
		}
		
		// And only on land
		if ((player.abilities.flying && player.vehicle == null)
			|| player.isInWater
			|| player.isInLava
			|| player.onClimbable())
		{
			return false
		}
		
		val wishspeed = speed.toDouble() * 2.15
		val wishdir = player.getMovementDirection(sidemove, forwardmove)
		val wishvel = Pair(wishdir.first * wishspeed, wishdir.second * wishspeed)
		baseVelocities.add(wishvel)
		
		return true
	}
	
	fun afterJump(player: Player)
	{
		val world = player.level();
		if (world.isClientSide && MvMod.config.quakeMovementEnabled)
		{
			if (player.isSprinting)
			{
				val f = player.yRot * 0.017453292f
				val xVel = player.deltaMovement.x + sin(f) * 0.2
				val zVel = player.deltaMovement.z - cos(f) * 0.2
				player.deltaMovement = Vec3(xVel, player.deltaMovement.y, zVel)
			}
			
			player.applyHardCap()
			player.spawnBunnyhopParticles(MvMod.config.jumpParticles)
			jumped = true
		}
	}
	
	fun travel(player: Player, movementInput: Vec3): Boolean
	{
		val world = player.level();
		if (!MvMod.config.quakeMovementEnabled
			||  !world.isClientSide
			||   player.abilities.flying
			||   player.isFallFlying
			||   player.vehicle != null)
		{
			return false
		}
		
		// Update last recorded speed
		val speed = player.getXySpeed()
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
			
			val flying = (player.abilities.flying || player.isFallFlying)
			
			// Apparently stats are stored with 2-digit fixed point precision
			if (player is ServerPlayer)
			{
				if (player.isInWater && !flying)
				{
					println((distance * 100.0).roundToInt())
					player.awardStat(MvStats.SHARK_ONE_CM, (distance * 100.0).roundToInt())
				}
				else
				{
					println((distance * 100.0).roundToInt())
					player.awardStat(MvStats.BHOP_ONE_CM, (distance * 100.0).roundToInt())
				}
			}
			
			// Swing arms and legs
			player.calculateEntityAnimation(false) // TODO: player.omnidirectionalAirMover() somehow?
			previousYaw = player.yRot
			return true
		}
		previousYaw = player.yRot
		
		jumping = false
		return false
	}
	
	fun beforeTick(player: Player)
	{
		val world = player.level();
		if (world.isClientSide && baseVelocities.isNotEmpty())
		{
			baseVelocities.clear()
		}
	}
	
	// Quake movement
	private fun Player.getMovementDirection(sidemoveInitial: Double, forwardmoveInitial: Double): Pair<Double, Double>
	{
		val preSpeed = sidemoveInitial * sidemoveInitial + forwardmoveInitial * forwardmoveInitial
		if (preSpeed >= 0)
		{
			var speed = sqrt(preSpeed)
			
			speed = if (speed <= 0.0) 1.0
			else			 1.0 / speed
			
			val sidemove	= sidemoveInitial	* speed
			val forwardmove = forwardmoveInitial * speed
			val f1 = sin(this.yRot * Math.PI / 180.0)
			val f2 = cos(this.yRot * Math.PI / 180.0)
			
			return Pair(
				(sidemove	* f2 - forwardmove * f1),
				(forwardmove * f2 + sidemove	* f1),
			)
		}
		
		return Pair(0.0, 0.0)
	}
	
	private fun Player.toRadians(degrees: Double): Double = degrees / 180.0f * PI
	private fun Player.toDegrees(radians: Double): Double = radians * 180.0f / PI
	
	private fun Player.anglesToVectors(pitch: Double, yaw: Double): Pair<Vec3, Vec3>
	{
		var radAngles = Vec3(
			this.toRadians(pitch),
			this.toRadians(yaw),
			0.0
		);
		
		var cosPitch = cos(radAngles.x);
		var sinPitch = sin(radAngles.x);
		
		var cosYaw = cos(radAngles.y);
		var sinYaw = sin(radAngles.y);
		
		var forwards = Vec3(cosPitch * -sinYaw, -sinPitch, cosPitch * cosYaw)
		var left = Vec3(cosYaw, 0.0, sinYaw)
		return Pair(forwards, left)
	}

	private const val QUAKE_MOVEMENT_SPEED_MULTIPLIER = 2.15
	private const val QUAKE_SNEAKING_SPEED_MULTIPLIER = 0.65
	private fun Player.getSlipperiness(): Double
	{
		if (this.onGround())
		{
			val world = this.level();
			val groundPos = BlockPos.containing(this.x, this.boundingBox.minY - 1, this.z)
			return world.getBlockState(groundPos).block.friction.toDouble()
		}

		return 0.0
	}
	
	private fun Player.getBaseSpeedCurrent(): Double
	{
		var result: Double = this.speed.toDouble()
			if (!this.isShiftKeyDown() && !this.isCrouching() && !this.isVisuallySwimming())
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
	
	private fun Player.getBaseSpeedMax(): Double
	{
		val baseSpeed = this.speed
		return baseSpeed * QUAKE_MOVEMENT_SPEED_MULTIPLIER
	}
	
	private fun Player.getXySpeed(): Double
	{
		val x = this.deltaMovement.x
		val z = this.deltaMovement.z
		return sqrt((x * x + z * z))
	}
	
	private fun Player.travelQuake(sidemove: Double, forwardmove: Double): Boolean
	{
		val flying = (this.abilities.flying || this.isFallFlying)
		if (this.isInLava && !flying)
		{
			return false // Swimming in lava
		}
		
		// Collect all relevant movement values
		val wishdir = this.getMovementDirection(sidemove, forwardmove)
		val wishspeed = if (sidemove != 0.0 || forwardmove != 0.0) this.getBaseSpeedCurrent() else 0.0
		val onGroundForReal = this.onGround() && !jumping
		
		// Sharking
		if (this.isInWater && !flying)
		{
			return false // Use default minecraft water movement
		}
		else
		{
			swimming = false
		}
		
		var world = this.level();
		if (this.onClimbable())
		{
			// laddermove!
			var blockState = world.getBlockState(this.getLastClimbablePos().orElse(null))
			if (blockState.getBlock() != Blocks.LADDER)
			{
				return false;
			}
			var ladderFacing: Direction = blockState.getValue(LadderBlock.FACING)
			var ladderNormal = ladderFacing.getUnitVec3()
			
			var forward = 0.0
			var left = 0.0
			//var vpn: Vec3;
			//var v_left: Vec3;
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
			val viewVectors = this.anglesToVectors(this.getXRot().toDouble(), this.getYRot().toDouble());
			val vecForward = viewVectors.first;
			val vecLeft = viewVectors.second;
			//this.sendMessage(Component.translatable("%f %f %f   %f %f %f".format(vecForward.x, vecForward.y, vecForward.z, vecLeft.x, vecLeft.y, vecLeft.z)), false)
			//this.sendMessage(Component.translatable("%f %f".format(sidemove, forwardmove)), false)
			//this.sendMessage(Component.translatable("%f %f %f".format(ladderNormal.x, ladderNormal.y, ladderNormal.z)), false)
			
			if (jumping)
			{
				// jump off
				this.deltaMovement = ladderNormal.scale(CLIMB_JUMPOFF_SPEED * FROM_QUAKE * FRAMETIME)
			}
			else
			{
				// move on ladder
				if (forwardmove != 0.0 || sidemove != 0.0)
				{
					var velocity = vecForward.scale(forwardmove * speed);
					velocity = velocity.add(vecLeft.scale(sidemove * speed));
					
					var tmp = Vec3(0.0, 1.0, 0.0)
					var perp = tmp.cross(ladderNormal);
					perp = perp.normalize();
					
					var normal = velocity.dot(ladderNormal);
					var cross = ladderNormal.scale(normal);
					
					var lateral = velocity.subtract(cross);
					
					tmp = ladderNormal.cross(perp);
					// CUSTOM: add a little speed that moves the player into the ladder so that
					//  they don't stick out a whole block
					var wishdirVec = Vec3(wishdir.first, 0.0, wishdir.second)
					var movingAwayFromLadder = ladderNormal.dot(wishdirVec) > 0;
					if (!this.onGround() || !movingAwayFromLadder)
					{
						lateral = lateral.add(ladderNormal.scale(-MAX_CLIMB_SPEED * FROM_QUAKE * FRAMETIME))
					}
					this.deltaMovement = lateral.add(tmp.scale(-normal))
						
					// allow players to move away from ladder when on ground
					if (this.onGround() && movingAwayFromLadder)
					{
						this.deltaMovement = this.deltaMovement.add(ladderNormal.scale(MAX_CLIMB_SPEED * FROM_QUAKE * FRAMETIME))
					}
				}
				else
				{
					this.deltaMovement = Vec3(0.0, 0.0, 0.0)
				}
			}
		}
		else if (onGroundForReal)
		{
			// Ground movement
			val slipperiness = this.getSlipperiness()
			val xVel = this.deltaMovement.x * slipperiness
			val zVel = this.deltaMovement.z * slipperiness
			this.deltaMovement = Vec3(xVel, this.deltaMovement.y, zVel)
			
			if (wishspeed != 0.0)
			{
				// Alter based on the surface friction
				val acceleration = MvMod.config.acceleration * (0.16277136 / (slipperiness * slipperiness * slipperiness))
				this.accelerate(wishspeed, wishdir.first, wishdir.second, acceleration, slipperiness)
			}
			
			if (baseVelocities.isNotEmpty())
			{
				var x = this.deltaMovement.x
				var z = this.deltaMovement.z
				val speedMod = wishspeed / this.getBaseSpeedMax()
				
				// add in base velocities
				for (baseVel in baseVelocities)
				{
					x += baseVel.first  * speedMod
					z += baseVel.second * speedMod
				}
				
				this.deltaMovement = Vec3(x, this.deltaMovement.y, z)
			}
		}
		else // Air movement
		{
			val airAcceleration = MvMod.config.airAcceleration
			// simulate 100 tickrate airacceleration
			val realYaw = this.yRot
			var savedYaw = this.yRot
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
				this.yRot = lerp(previousYaw.toDouble(), savedYaw.toDouble(), i.toDouble() / 5.0).toFloat()
				val wishdirAir = this.getMovementDirection(sidemove, forwardmove)
//				 this.sendMessage(Component.translatable("%.2f %.2f ".format(wishdirAir.first, wishdirAir.second)))
				this.airAccelerate(airWishspeed, wishdirAir.first, wishdirAir.second, airAcceleration)
			}
			this.yRot = realYaw
		}
		
		// Apply velocity
		this.move(MoverType.SELF, this.deltaMovement)
		
		// stick to ground, aka ledgegrab/glidestep
		val list = world.getEntityCollisions(null, this.getBoundingBox().expandTowards(this.getKnownSpeed()))
		val down = -(4.0 * FROM_QUAKE)
		val movement: Vec3 = Entity.collideBoundingBox(this, Vec3(0.0, down, 0.0), this.getBoundingBox(), world, list)
		if (movement.y > down
			&& !this.onGround()
			&& this.deltaMovement.y * TICKRATE * TO_QUAKE < 200.0
			&& this.deltaMovement.y >= 0.0) // don't need to ledgegrab if falling down
		{
			val pos = this.position()
			this.setPos(pos.x, pos.y + movement.y, pos.z)
			this.deltaMovement = Vec3(this.deltaMovement.x, 0.0, this.deltaMovement.z)
			this.setOnGround(true)
		}
		
		if (!this.onClimbable())
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

	private fun Player.applyGravity()
	{
		val levitating = this.hasEffect(MobEffects.LEVITATION)
		
		var yVel = this.deltaMovement.y
		var gravity = -0.08 // gravity
		
		// Powdered snow
		if ((this.horizontalCollision || jumping)
			//&& (this.onClimbable() || getInBlockState().is(Blocks.POWDER_SNOW)
			&& (getInBlockState().getBlock() == Blocks.POWDER_SNOW
			&& PowderSnowBlock.canEntityWalkOnPowderSnow(this)))
		{
			yVel += 0.2
		}
		
		// Slow falling
		if (this.deltaMovement.y <= 0.0 && this.hasEffect(MobEffects.SLOW_FALLING))
		{
			gravity = -0.01
			this.resetFallDistance()
		}
		
		// Levitation
		if (levitating)
		{
			yVel += (FRAMETIME * (this.getEffect(MobEffects.LEVITATION)!!.amplifier + 1).toDouble() - yVel) * 0.2
			resetFallDistance()
		}
		
		var world = this.level();
		// Apply gravity
		if (!world.isClientSide || world.getChunkSource().hasChunk(SectionPos.blockToSectionCoord(this.blockPosition().x), SectionPos.blockToSectionCoord(blockPosition().z)))
		{
			if (!this.isNoGravity() && !levitating)
			{
				yVel += gravity
			}
			
			val airResistance = 0.9800000190734863
			this.deltaMovement = Vec3(this.deltaMovement.x, yVel * airResistance, this.deltaMovement.z)
		}
		else // If chunk is not loaded slowly fall to minY
		{
			yVel = if (this.y > world.minY.toDouble()) -0.1 else 0.0
			this.deltaMovement = Vec3(this.deltaMovement.x, yVel, this.deltaMovement.z)
		}
	}

	private fun Player.accelerate(wishspeed: Double, wishX: Double, wishZ: Double, acceleration: Double, slipperiness: Double)
	{
		// Determine veer amount; this is a dot product
		val currentSpeed = this.deltaMovement.x * wishX + this.deltaMovement.z * wishZ
		// Speed delta
		val addSpeed = wishspeed - currentSpeed
		
		// If not adding any, done
		if (addSpeed <= 0) return
		
		// Determine acceleration speed after acceleration
		var accelSpeed = acceleration * wishspeed / slipperiness * FRAMETIME
		if (accelSpeed > addSpeed) accelSpeed = addSpeed
		
		// Adjust move velocity
		val x = (this.deltaMovement.x + accelSpeed * wishX)
		val z = (this.deltaMovement.z + accelSpeed * wishZ)
		this.deltaMovement = Vec3(x, this.deltaMovement.y, z)
	}
	
	private fun Player.airAccelerate(wishspeedInitial_: Double, wishX: Double, wishZ: Double, accel: Double)
	{
		val maxAirAcceleration = MvMod.config.maxAAccPerTick
		// velocity is per-tick in minecraft, not per-second like in source/quake. AAAAAAAAAAAAAAAAa
		var wishspeedInitial: Double = wishspeedInitial_ * TO_QUAKE * TICKRATE
		val wishspeed = if (wishspeedInitial > maxAirAcceleration) maxAirAcceleration else wishspeedInitial
		
		// Determine veer amount; this is a dot product
		val currentSpeed = (this.deltaMovement.x * TO_QUAKE * TICKRATE) * wishX + (this.deltaMovement.z * TO_QUAKE * TICKRATE) * wishZ
		
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
		val x = (this.deltaMovement.x * TO_QUAKE * TICKRATE) + accelSpeed * wishX
		val z = (this.deltaMovement.z * TO_QUAKE * TICKRATE) + accelSpeed * wishZ
		this.deltaMovement = Vec3(x * FROM_QUAKE * FRAMETIME, this.deltaMovement.y, z * FROM_QUAKE * FRAMETIME)
	}
	
	private fun Player.applyHardCap()
	{
		val hardCap = MvMod.config.hardCapSpeed * FROM_QUAKE * FRAMETIME
		val speed = this.getXySpeed()
		
		if (speed > hardCap && hardCap != 0.0 && MvMod.config.speedCapEnabled)
		{
			val multiplier = hardCap / speed
			val xVel = this.deltaMovement.x * multiplier
			val zVel = this.deltaMovement.z * multiplier
			this.deltaMovement = Vec3(xVel, this.deltaMovement.y, zVel)
		}
	}

	// Particles
	private fun Player.spawnBunnyhopParticles(numParticles: Int)
	{
		if (numParticles < 1)
		{
			return
		}
		
		// taken from sprint
		val i = floor(this.x                      ).toInt()
		val j = floor(this.y - 0.20000000298023224).toInt()
		val k = floor(this.z                      ).toInt()
			
		val world = this.level();
		val blockState = world.getBlockState(BlockPos(i, j, k))
		if (blockState.getRenderShape() != RenderShape.INVISIBLE)
		{
			for (iParticle in 0 until numParticles)
			{
				val x = this.x + (this.random.nextFloat() - 0.5) * this.getBbWidth()
				val z = this.z + (this.random.nextFloat() - 0.5) * this.getBbWidth()
				val y = this.boundingBox.minY + 0.1
				
				val xVel = -this.deltaMovement.x * 4.0
				val zVel = -this.deltaMovement.z
				val yVel = 1.5
				
				val effect = BlockParticleOption(ParticleTypes.BLOCK, blockState)
				world.addParticle(effect, x, y, z, xVel, yVel, zVel)
			}
		}
	}
}
