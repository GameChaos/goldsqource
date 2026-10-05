package gamechaos.goldsqource.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class GoldsqourceClient implements ClientModInitializer {
	private KeyMapping keyToggle = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.${Goldsqource.ID}.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "${Goldsqource.ID}"));
	private KeyMapping keyConfig = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.${Goldsqource.ID}.config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "${Goldsqource.ID}"));

	@Override
	public void onInitializeClient() {
		// initialise config
		GoldsqourceConfigWrapper.getInstance();

		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player != null) {
				//? if >=1.21.4 {
				GoldsqourcePlayer.getInstance().jumping = client.player.input.keyPresses.jump();
				//?} else
				//GoldsqourcePlayer.getInstance().jumping = client.player.input.jumping;
			}
		});
	}
}