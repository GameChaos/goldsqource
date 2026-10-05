package gamechaos.goldsqource.client.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import gamechaos.goldsqource.client.GoldsqourceConfigWrapper;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity {
    @Shadow
    private int noJumpDelay;
    @Shadow protected boolean jumping;
    public Boolean jumped = false;

    @Inject(method = "aiStep", at = @At(value = "HEAD"))
    public void tickMovement(CallbackInfo ci)
    {
        if (!((Object)this instanceof Player))
        {
            return;
        }

        if (!GoldsqourceConfigWrapper.getInstance().config.movementEnabled)
        {
            return;
        }

        noJumpDelay = 0;
        if (GoldsqourceConfigWrapper.getInstance().config.movedata.value.bufferedJump)
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

    @Inject(method = "jumpFromGround", at = @At(value = "HEAD"), cancellable = true)
    public void jump(CallbackInfo ci)
    {
        if (!((Object)this instanceof Player))
        {
            return;
        }

        if (GoldsqourceConfigWrapper.getInstance().config.movementEnabled)
        {
            return;
        }

        if (GoldsqourceConfigWrapper.getInstance().config.movedata.value.bufferedJump)
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

    @Inject(method = "jumpFromGround", at = @At(value = "TAIL"))
    public void jumpPost(CallbackInfo ci)
    {
        if (!((Object)this instanceof Player))
        {
            return;
        }

        if (GoldsqourceConfigWrapper.getInstance().config.movementEnabled)
        {
            return;
        }

        Player player = (Player)(Object)this;
        // MvPlayer.INSTANCE.afterJump(player);
    }
}
