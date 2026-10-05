package gamechaos.goldsqource.client;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "goldsqource")
public class GoldsqourceConfig implements ConfigData {
    public boolean movementEnabled = true;
    public GoldsqourceConfigWrapper.MovedataList movedata = GoldsqourceConfigWrapper.MovedataList.DEFAULT;
}
