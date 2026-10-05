package gamechaos.goldsqource.client;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

public final class GoldsqourcePlayer
{
    private static GoldsqourcePlayer INSTANCE;
    private GoldsqourcePlayer() {
    }

    public static GoldsqourcePlayer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GoldsqourcePlayer();
        }

        return INSTANCE;
    }
    public final double TO_QUAKE = 40.0; // 72 / 1.8 (player height in source divided by player height in minecraft)
    public final double FROM_QUAKE = 1.0 / 40.0;
    public final double TICKRATE = 20.0;
    public final double FRAMETIME = 1.0 / TICKRATE;
    public final double FAKE_FRAMETIME = 1.0 / 100.0; // for simulating 100 tick airacceleration
    public final double MAX_CLIMB_SPEED = 200.0; // ladder climbing speed
    public final double CLIMB_JUMPOFF_SPEED = 270.0;

    List<Vec3> baseVelocities = new ArrayList<>();
    float previousYaw      = 0.0f;
    double previousSpeed   = 0.0;
    double currentSpeed    = 0.0;
    boolean jumping	       = false;
    boolean jumped         = false;
    boolean swimming	   = false;

    public boolean travel(Player player, Vec3 vec3) {
        return false;
    }

    public void tick(Player player) {

    }
}