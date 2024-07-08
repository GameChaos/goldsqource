package polina4096.resquake

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.minecraft.client.MinecraftClient
import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import polina4096.resquake.integration.ModMenuIntegration
import kotlin.math.roundToInt
import net.minecraft.client.render.RenderTickCounter


object ReSquakeModClient : ClientModInitializer {
	private var keyToggle: KeyBinding = KeyBindingHelper.registerKeyBinding(KeyBinding("key.${ReSquakeMod.ID}.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.${ReSquakeMod.ID}"))
	private var keyConfig: KeyBinding = KeyBindingHelper.registerKeyBinding(KeyBinding("key.${ReSquakeMod.ID}.config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.${ReSquakeMod.ID}"))
	
	override fun onInitializeClient()
	{
		ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick{ client: MinecraftClient ->
			if (client.player != null)
			{
				ReSquakePlayer.jumping = client.player!!.input.jumping
				if (ReSquakeMod.config.bufferedJump)
				{
					if (!client.player!!.input.jumping)
					{
						ReSquakePlayer.jumped = false
					}
					else if (ReSquakePlayer.jumped)
					{
						// this is so that you don't airaccelerate on the ground
						ReSquakePlayer.jumping = false
					}
				}
			}
			
			if (keyConfig.wasPressed())
			{
				client.setScreen(generateConfigScreen(client.currentScreen))
			}
			
			while (keyToggle.wasPressed())
			{
				ReSquakeMod.config.quakeMovementEnabled = !ReSquakeMod.config.quakeMovementEnabled
				if (ReSquakeMod.config.quakeMovementEnabled)
				{
					client.player?.sendMessage(Text.translatable("resquake.enabled"), true)
				}
				else
				{
					client.player?.sendMessage(Text.translatable("resquake.disabled"), true)
				}
			}
		})
	}
}
