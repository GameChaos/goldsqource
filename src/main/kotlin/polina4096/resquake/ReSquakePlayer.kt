package polina4096.resquake

import net.minecraft.block.BlockRenderType
import net.minecraft.block.Blocks
import net.minecraft.block.PowderSnowBlock
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
import net.minecraft.text.Text
import kotlin.math.*


object ReSquakePlayer {
    private var baseVelocities = mutableListOf<Pair<Double, Double>>()
    private const val TO_QUAKE = 40.0 // 72 / 1.8 (player height in source divided by player height in minecraft)
    private const val FROM_QUAKE = 1.0 / 40.0
    private var TICKRATE = 20.0
    private var FRAMETIME = 1.0 / TICKRATE
    private var FAKE_FRAMETIME = 1.0 / 100.0 // for simulating 100 tick airacceleration

    var previousYaw : Float  = 0.0f
    var previousSpeed : Double  = 0.0
    var currentSpeed  : Double  = 0.0
    var jumping       : Boolean = false
    var swimming      : Boolean = false

    private fun collectSpeed(speed: Double) {
        previousSpeed = currentSpeed
        currentSpeed  = speed
    }

    // API
    fun updateVelocity(player: Entity, speed: Float, sidemove: Double, forwardmove: Double): Boolean {
        // When the bunnyhop is enabled
        if (!ReSquakeMod.config.quakeMovementEnabled) return false

        if (player !is PlayerEntity) return false // We are only interested in players
        if (!player.world.isClient)  return false // And only in the client player

        // And only on land
        if((player.abilities.flying && player.vehicle == null) || player.isTouchingWater || player.isInLava || player.isClimbing)
            return false

        val wishspeed = speed.toDouble() * 2.15
        val wishdir = player.getMovementDirection(sidemove, forwardmove)
        val wishvel = Pair(wishdir.first * wishspeed, wishdir.second * wishspeed)
        baseVelocities.add(wishvel)

        return true
    }
    fun afterJump(player: PlayerEntity) {
        if (player.world.isClient && ReSquakeMod.config.quakeMovementEnabled) {
            if (player.isSprinting) {
                val f = player.yaw * 0.017453292f
                val xVel = player.velocity.x + sin(f) * 0.2
                val zVel = player.velocity.z - cos(f) * 0.2
                player.velocity = Vec3d(xVel, player.velocity.y, zVel)
            }

            // Update last recorded speed
            val speed = player.getSpeed()
            collectSpeed(speed)

            player.applyHardCap()
            player.spawnBunnyhopParticles(ReSquakeMod.config.jumpParticles)
        }
    }
    fun travel(player: PlayerEntity, movementInput: Vec3d): Boolean {
        if (!ReSquakeMod.config.quakeMovementEnabled
            ||  !player.world.isClient
            ||   player.abilities.flying
            ||   player.isFallFlying
            ||   player.vehicle != null)
            return false
        
        val preX = player.x
        val preY = player.y
        val preZ = player.z
        if (player.travelQuake(movementInput.x, movementInput.z)) {
            val distance =
               ((player.x - preX).pow(2) +
                (player.y - preY).pow(2) +
                (player.z - preZ).pow(2)).pow(1.0 / 2.0)

            val flying = (player.abilities.flying || player.isFallFlying)

            // Apparently stats are stored with 2-digit fixed point precision
            if (player is ServerPlayerEntity) {
                if (player.isTouchingWater && !flying) {
                    println((distance * 100.0).roundToInt())
                    player.increaseStat(ReSquakeStats.SHARK_ONE_CM, (distance * 100.0).roundToInt())
                } else {
                    println((distance * 100.0).roundToInt())
                    player.increaseStat(ReSquakeStats.BHOP_ONE_CM, (distance * 100.0).roundToInt())
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
    fun beforeTick(player: PlayerEntity) {
        if (player.world.isClient && baseVelocities.isNotEmpty())
            baseVelocities.clear()
    }

    // Quake movement
    private fun PlayerEntity.getMovementDirection(sidemoveInitial: Double, forwardmoveInitial: Double): Pair<Double, Double> {
        val preSpeed = sidemoveInitial * sidemoveInitial + forwardmoveInitial * forwardmoveInitial
        if (preSpeed >= 0) {
            var speed = sqrt(preSpeed)

            speed = if (speed < 1.0) 1.0
            else             1.0 / speed

            val sidemove    = sidemoveInitial    * speed
            val forwardmove = forwardmoveInitial * speed
            val f1 = sin(this.yaw * Math.PI / 180.0)
            val f2 = cos(this.yaw * Math.PI / 180.0)

            return Pair(
                (sidemove    * f2 - forwardmove * f1),
                (forwardmove * f2 + sidemove    * f1),
            )
        }

        return Pair(0.0, 0.0)
    }

    private const val QUAKE_MOVEMENT_SPEED_MULTIPLIER = 2.15
    private const val QUAKE_SNEAKING_SPEED_MULTIPLIER = 1.10
    private fun PlayerEntity.getSlipperiness(): Double {
        if (this.isOnGround) {
            val groundPos = BlockPos.ofFloored(this.x, this.boundingBox.minY - 1, this.z)
            return this.world.getBlockState(groundPos).block.slipperiness.toDouble()
        }

        return 0.0
    }
    private fun PlayerEntity.getBaseSpeedCurrent(): Double {
        val baseSpeed = this.movementSpeed
        return (if (!this.isSneaking) baseSpeed * QUAKE_MOVEMENT_SPEED_MULTIPLIER else baseSpeed * QUAKE_SNEAKING_SPEED_MULTIPLIER)
    }
    private fun PlayerEntity.getBaseSpeedMax(): Double {
        val baseSpeed = this.movementSpeed
        return baseSpeed * QUAKE_MOVEMENT_SPEED_MULTIPLIER
    }
    private fun PlayerEntity.getSpeed(): Double {
        val x = this.velocity.x
        val z = this.velocity.z
        return sqrt((x * x + z * z))
    }

    private fun PlayerEntity.travelQuake(sidemove: Double, forwardmove: Double): Boolean {
        // Fallback to default minecraft movement
        if (this.isClimbing || this.isInSwimmingPose) return false

        val flying = (this.abilities.flying || this.isFallFlying)
        if (this.isInLava && !flying) return false // Swimming in lava

        // Collect all relevant movement values
        val wishdir = this.getMovementDirection(sidemove, forwardmove)
        val wishspeed = if (sidemove != 0.0 || forwardmove != 0.0) this.getBaseSpeedCurrent() else 0.0
        val onGroundForReal = this.isOnGround && !jumping
        
//         this.sendMessage(Text.translatable("%.2f %.2f".format(this.yaw, previousYaw)))

        // Sharking
        if (this.isTouchingWater && !flying) {
            if (ReSquakeMod.config.sharkingEnabled)
                 return this.travelWaterQuake(wishspeed, wishdir.first, wishdir.second, sidemove, forwardmove)
            else return false // Use default minecraft water movement if disabled (https://github.com/polina4096/resquake/issues/4)
        } else swimming = false

        // Ground movement
        if (onGroundForReal) {
            val slipperiness = this.getSlipperiness()
            val xVel = this.velocity.x * slipperiness
            val zVel = this.velocity.z * slipperiness
            this.velocity = Vec3d(xVel, this.velocity.y, zVel)

            if (wishspeed != 0.0) { // Alter based on the surface friction
                val acceleration = ReSquakeMod.config.acceleration * (0.16277136 / (slipperiness * slipperiness * slipperiness))
                this.accelerate(wishspeed, wishdir.first, wishdir.second, acceleration, slipperiness)
            }

            if (baseVelocities.isNotEmpty()) {
                var x = this.velocity.x
                var z = this.velocity.z
                val speedMod = wishspeed / this.getBaseSpeedMax()

                // add in base velocities
                for (baseVel in baseVelocities) {
                    x += baseVel.first  * speedMod
                    z += baseVel.second * speedMod
                }

                this.velocity = Vec3d(x, this.velocity.y, z)
            }
        }

        // Air movement
        else {
            val airAcceleration = ReSquakeMod.config.airAcceleration
            // simulate 100 tickrate airacceleration
            val realYaw = this.yaw;
            var savedYaw = this.yaw;
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
                this.airAccelerate(airWishspeed, wishdirAir.first, wishdirAir.second, airAcceleration)
            }
            this.yaw = realYaw

            // Movement on top of water
            if (ReSquakeMod.config.sharkingEnabled && ReSquakeMod.config.sharkingSurfaceTension > 0.0 && jumping && this.velocity.y < 0.0) {
                val isFallingIntoWater = this.world.containsFluid(this.boundingBox.offset(this.velocity))
                if (isFallingIntoWater) this.velocity = Vec3d(this.velocity.x, this.velocity.y * ReSquakeMod.config.sharkingSurfaceTension, this.velocity.z)
            }
        }

        // Apply velocity
        this.move(MovementType.SELF, this.velocity)

        // HL2 code applies half gravity before acceleration and half after acceleration, but this seems to work fine
        this.applyGravity()

        // Cancel default minecraft movement behavior
        return true
    }
    
    private fun lerp(a: Double, b: Double, t: Double): Double
    {
        return (1.0 - t) * a + b * t;
    }

    private fun PlayerEntity.applyGravity() {
        val levitating = hasStatusEffect(StatusEffects.LEVITATION)

        var yVel = this.velocity.y
        var gravity = -0.08 // gravity

        // Powdered snow
        if ((this.horizontalCollision || jumping)
        && (this.isClimbing || blockStateAtPos.isOf(Blocks.POWDER_SNOW)
        && PowderSnowBlock.canWalkOnPowderSnow(this))) {
            yVel += 0.2
        }

        // Slow falling
        if (velocity.y <= 0.0 && hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            gravity = -0.01
            this.onLanding()
        }

        // Levitation
        if (levitating) {
            yVel += (FRAMETIME * (getStatusEffect(StatusEffects.LEVITATION)!!.amplifier + 1).toDouble() - yVel) * 0.2
            onLanding()
        }

        // Apply gravity
        if (!world.isClient || world.chunkManager.isChunkLoaded(ChunkSectionPos.getSectionCoord(blockPos.x), ChunkSectionPos.getSectionCoord(blockPos.z))) {
            if (!hasNoGravity() && !levitating)
                yVel += gravity

            val airResistance = 0.9800000190734863
            this.velocity = Vec3d(this.velocity.x, yVel * airResistance, this.velocity.z)
        }

        else { // If chunk is not loaded slowly fall to bottomY
            yVel = if (this.y > world.bottomY.toDouble()) -0.1 else 0.0
            this.velocity = Vec3d(this.velocity.x, yVel, this.velocity.z)
        }

    }

    private fun PlayerEntity.accelerate(wishspeed: Double, wishX: Double, wishZ: Double, acceleration: Double, slipperiness: Double) {
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
    private fun PlayerEntity.airAccelerate(wishspeedInitial_: Double, wishX: Double, wishZ: Double, accel: Double) {
        val maxAirAcceleration = ReSquakeMod.config.maxAAccPerTick
        // velocity is per-tick in minecraft, not per-second like in source/quake. AAAAAAAAAAAAAAAAa
        var wishspeedInitial: Double = wishspeedInitial_ * TO_QUAKE * TICKRATE
        val wishspeed = if (wishspeedInitial > maxAirAcceleration) maxAirAcceleration else wishspeedInitial

        // Determine veer amount; this is a dot product
        val currentSpeed = (this.velocity.x * TO_QUAKE * TICKRATE) * wishX + (this.velocity.z * TO_QUAKE * TICKRATE) * wishZ

        // Speed delta
        val addSpeed = wishspeed - currentSpeed

        // If not adding any, done
        if (addSpeed <= 0) return

        // Determine acceleration speed after acceleration
        var accelSpeed = accel * wishspeedInitial * FAKE_FRAMETIME
        if (accelSpeed > addSpeed) accelSpeed = addSpeed

        // Adjust move velocity
        val x = (this.velocity.x * TO_QUAKE * TICKRATE) + accelSpeed * wishX
        val z = (this.velocity.z * TO_QUAKE * TICKRATE) + accelSpeed * wishZ
        this.velocity = Vec3d(x * FROM_QUAKE * FRAMETIME, this.velocity.y, z * FROM_QUAKE * FRAMETIME)
    }
    private fun PlayerEntity.applyHardCap() {
        if (ReSquakeMod.config.uncappedBunnyhop) return

        val hardCap = ReSquakeMod.config.hardCapSpeed * FROM_QUAKE * FRAMETIME
        val speed = this.getSpeed()

        if (speed > hardCap && hardCap != 0.0) {
            val multiplier = hardCap / speed
            val xVel = this.velocity.x * multiplier
            val zVel = this.velocity.z * multiplier
            this.velocity = Vec3d(xVel, this.velocity.y, zVel)
        }
    }

    // Sharking
    private fun PlayerEntity.travelWaterQuake(wishspeed: Double, wishX: Double, wishZ: Double, sidemove: Double, forwardmove: Double): Boolean {
        // Collect all relevant movement values
        val speed = this.getSpeed()

        // Move in water
        if (!jumping || !this.doesNotCollide(0.0, 1.0, 0.0) || speed < 0.078f) {
            swimming = true
            return false
        }

        // Swim in water
        else {
            swimming = false

            // Update last recorded speed
            collectSpeed(speed)

            // Apply friction
            if (speed > 0.090) this.velocity = this.velocity.multiply(ReSquakeMod.config.sharkingFriction)

            // Accelerate
            if (speed > 0.098) this.airAccelerate(wishspeed, wishX, wishZ, ReSquakeMod.config.acceleration)
                          else this.accelerate(wishspeed, wishX, wishZ, ReSquakeMod.config.acceleration, 1.0)

            this.move(MovementType.SELF, this.velocity)
            this.velocity = Vec3d(velocity.x, if (velocity.y >= 0) velocity.y else 0.0, velocity.z)
        }

        return true
    }

    // Particles
    private fun PlayerEntity.spawnBunnyhopParticles(numParticles: Int) {
        if (numParticles < 1) return

        // taken from sprint
        val i = floor(this.x                      ).toInt()
        val j = floor(this.y - 0.20000000298023224).toInt()
        val k = floor(this.z                      ).toInt()

        val blockState = this.world.getBlockState(BlockPos(i, j, k))
        if (blockState.renderType != BlockRenderType.INVISIBLE) {
            for (iParticle in 0 until numParticles) {
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
