package gamechaos.goldsqource.integration

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import gamechaos.goldsqource.generateConfigScreen

@Environment(EnvType.CLIENT)
class ModMenuIntegration : ModMenuApi
{
	override fun getModConfigScreenFactory() = ConfigScreenFactory {generateConfigScreen(it)}
}
