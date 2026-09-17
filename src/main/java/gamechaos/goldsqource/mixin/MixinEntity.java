package gamechaos.goldsqource.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gamechaos.goldsqource.MvPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(Entity.class)
public abstract class MixinEntity {
	@Inject(method = "moveRelative", at = @At("HEAD"), cancellable = true)
	public void updateVelocityInject(float speed, Vec3 movementInput, CallbackInfo ci) {
		Entity player = (Entity)(Object)this;
		if (MvPlayer.INSTANCE.updateVelocity(player, speed, movementInput.x, movementInput.z)) {
			ci.cancel();
		}
	}
}