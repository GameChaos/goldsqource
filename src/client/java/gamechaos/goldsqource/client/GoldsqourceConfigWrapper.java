package gamechaos.goldsqource.client;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

public final class GoldsqourceConfigWrapper
{
    private static GoldsqourceConfigWrapper INSTANCE;
    public GoldsqourceConfig config;
    private GoldsqourceConfigWrapper() {
        AutoConfig.register(GoldsqourceConfig.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(GoldsqourceConfig.class).getConfig();
    }

    public static GoldsqourceConfigWrapper getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GoldsqourceConfigWrapper();
        }

        return INSTANCE;
    }

    public static class Movedata {
        public boolean speedcapEnabled = true;
        public boolean bufferedJump = false;
        public double hardCapSpeed = 300;
        public double acceleration = 10;
        public double airacceleration = 10;
        public double airMaxWishspeed = 30;
        public Movedata(boolean speedcapEnabled,
                        boolean bufferedJump,
                        double hardCapSpeed,
                        double acceleration,
                        double airacceleration,
                        double airMaxWishspeed)
        {
            this.speedcapEnabled = speedcapEnabled;
            this.bufferedJump = bufferedJump;
            this.hardCapSpeed = hardCapSpeed;
            this.acceleration = acceleration;
            this.airacceleration = airacceleration;
            this.airMaxWishspeed = airMaxWishspeed;
        }
        public Movedata() {}
    }

    public enum MovedataList
    {
        // TODO: tickrate multiplier
        DEFAULT(),
        BHOP(false, false, 0.0, 10.0, 100.0, 30.0),
        CS16(true, false, 300.0, 10.0, 10.0, 30.0),
        VQ3(true, true, 300.0, 10.0, 10.0, 300.0),
        CUSTOM(),
        ;

        public final Movedata value;
        private MovedataList(
                boolean speedcapEnabled,
                boolean bufferedJump,
                double hardCapSpeed,
                double acceleration,
                double airacceleration,
                double airMaxWishspeed
        ) {
            value = new Movedata(speedcapEnabled, bufferedJump, hardCapSpeed, acceleration, airacceleration, airMaxWishspeed);
        }
        private MovedataList() {
            value = new Movedata();
        }
    }

}