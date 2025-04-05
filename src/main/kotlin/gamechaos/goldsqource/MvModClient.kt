package gamechaos.goldsqource

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
import gamechaos.goldsqource.integration.ModMenuIntegration
import kotlin.math.roundToInt
import net.minecraft.client.render.RenderTickCounter


object MvModClient : ClientModInitializer {
	private var keyToggle: KeyBinding = KeyBindingHelper.registerKeyBinding(KeyBinding("key.${MvMod.ID}.toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.${MvMod.ID}"))
	private var keyConfig: KeyBinding = KeyBindingHelper.registerKeyBinding(KeyBinding("key.${MvMod.ID}.config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.${MvMod.ID}"))
	
	override fun onInitializeClient()
	{
		ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick{ client: MinecraftClient ->
			if (client.player != null)
			{
				MvPlayer.jumping = client.player!!.input.playerInput.jump()
				if (MvMod.config.bufferedJump)
				{
					if (!client.player!!.input.playerInput.jump())
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
			
			if (keyConfig.wasPressed())
			{
				client.setScreen(generateConfigScreen(client.currentScreen))
			}
			
			while (keyToggle.wasPressed())
			{
				MvMod.config.quakeMovementEnabled = !MvMod.config.quakeMovementEnabled
				if (MvMod.config.quakeMovementEnabled)
				{
					client.player?.sendMessage(Text.translatable("goldsqource.enabled"), true)
				}
				else
				{
					client.player?.sendMessage(Text.translatable("goldsqource.disabled"), true)
				}
			}
		})
	}
}
