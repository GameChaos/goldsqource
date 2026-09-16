package gamechaos.goldsqource

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.KeyMapping
import net.minecraft.network.chat.Component
//import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW


object MvModClient : ClientModInitializer {
	private var keyToggle: KeyMapping = KeyBindingHelper.registerKeyBinding(KeyMapping("key.${MvMod.ID}.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, "${MvMod.ID}"))
	private var keyConfig: KeyMapping = KeyBindingHelper.registerKeyBinding(KeyMapping("key.${MvMod.ID}.config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "${MvMod.ID}"))
	
	override fun onInitializeClient()
	{
		ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick{ client: Minecraft ->
			if (client.player != null)
			{
				MvPlayer.jumping = client.player!!.input.keyPresses.jump()
				if (MvMod.config.bufferedJump)
				{
					if (!client.player!!.input.keyPresses.jump())
					{
						MvPlayer.jumped = false
					}
					else if (MvPlayer.jumped)
					{
						// this is so that you don't airaccelerate on the ground
						MvPlayer.jumping = false
					}
				}
			}
			
			if (keyConfig.consumeClick())
			{
				client.setScreen(generateConfigScreen(client.screen))
			}
			
			while (keyToggle.consumeClick())
			{
				MvMod.config.quakeMovementEnabled = !MvMod.config.quakeMovementEnabled
				if (MvMod.config.quakeMovementEnabled)
				{
					client.player?.displayClientMessage(Component.translatable("goldsqource.enabled"), true)
				}
				else
				{
					client.player?.displayClientMessage(Component.translatable("goldsqource.disabled"), true)
				}
			}
		})
	}
}
