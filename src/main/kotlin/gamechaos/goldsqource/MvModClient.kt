package gamechaos.goldsqource

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.Minecraft
import net.minecraft.client.KeyMapping
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW


object MvModClient : ClientModInitializer {
	private val CATEGORY: KeyMapping.Category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("${MvMod.ID}", "keybinds"))
	private var keyToggle: KeyMapping = KeyMappingHelper.registerKeyMapping(KeyMapping("key.${MvMod.ID}.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY))
	private var keyConfig: KeyMapping = KeyMappingHelper.registerKeyMapping(KeyMapping("key.${MvMod.ID}.config", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY))
	
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
					client.player?.sendSystemMessage(Component.translatable("goldsqource.enabled"))
				}
				else
				{
					client.player?.sendSystemMessage(Component.translatable("goldsqource.disabled"))
				}
			}
		})
	}
}
