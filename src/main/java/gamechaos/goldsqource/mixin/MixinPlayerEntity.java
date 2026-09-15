package gamechaos.goldsqource.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gamechaos.goldsqource.MvPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(Player.class)
public abstract class MixinPlayerEntity extends LivingEntity {
	protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, Level world) {
		super(entityType, world);
	}
	
	@Inject(method = "travel", at = @At("HEAD"), cancellable = true)
	public void travelInject(Vec3 movementInput, CallbackInfo ci) {
		Player player = (Player)(Object)this;
		if (MvPlayer.INSTANCE.travel(player, movementInput)) {
			ci.cancel();
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	public void tickInject(CallbackInfo ci) {
		Player player = (Player)(Object)this;
		MvPlayer.INSTANCE.beforeTick(player);
	}
}
