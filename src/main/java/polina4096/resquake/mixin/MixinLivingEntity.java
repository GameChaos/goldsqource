package polina4096.resquake.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.LivingEntity;
import polina4096.resquake.ReSquakeMod;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity
{
	@Shadow private int jumpingCooldown;
	@Shadow protected boolean jumping;
	public Boolean jumped = false;

	@Inject(method = "tickMovement", at = @At(value = "HEAD"))
	public void tickMovement(CallbackInfo ci)
	{
		if (ReSquakeMod.config.getNoJumpCooldown())
		{
			jumpingCooldown = 0;
		}
		
		if (ReSquakeMod.config.getBufferedJump())
		{
			if (!jumping)
			{
				jumped = false;
			}
			else if (jumped)
			{
				jumping = false;
			}
		}
		else
		{
			jumped = false;
		}
	}
	
	@Inject(method = "jump", at = @At(value = "HEAD"), cancellable = true)
	public void jump(CallbackInfo ci)
	{
		if (!ReSquakeMod.config.getBufferedJump())
		{
			return;
		}
		
		if (jumped)
		{
			ci.cancel();
		}
		else
		{
			jumped = true;
		}
	}
}
