package gamechaos.goldsqource.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.LivingEntity;
import gamechaos.goldsqource.ReSquakeMod;
import gamechaos.goldsqource.ReSquakePlayer;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity
{
	@Shadow private int jumpingCooldown;
	@Shadow protected boolean jumping;
	public Boolean jumped = false;

	@Inject(method = "tickMovement", at = @At(value = "HEAD"))
	public void tickMovement(CallbackInfo ci)
	{
		if (!((Object)this instanceof PlayerEntity))
		{
			return;
		}
		
		// no jumping cooldown!
		jumpingCooldown = 0;
		
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
		if (!((Object)this instanceof PlayerEntity))
		{
			return;
		}
		
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
	
	@Inject(method = "jump", at = @At(value = "TAIL"))
	public void jumpPost(CallbackInfo ci)
	{
		if (!((Object)this instanceof PlayerEntity))
		{
			return;
		}
		
		PlayerEntity player = (PlayerEntity)(Object)this;
		ReSquakePlayer.INSTANCE.afterJump(player);
	}
}
