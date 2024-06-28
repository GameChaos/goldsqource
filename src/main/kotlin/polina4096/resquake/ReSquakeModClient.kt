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

    override fun onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { client: MinecraftClient ->
            if (client.player != null)
                ReSquakePlayer.jumping = client.player!!.input.jumping

            if (keyConfig.wasPressed()) {
                client.setScreen(generateConfigScreen(client.currentScreen))
            }

            while (keyToggle.wasPressed()) {
                ReSquakeMod.config.quakeMovementEnabled = !ReSquakeMod.config.quakeMovementEnabled
                if (ReSquakeMod.config.quakeMovementEnabled)
                     client.player?.sendMessage(Text.translatable("resquake.enabled"), true)
                else client.player?.sendMessage(Text.translatable("resquake.disabled"), true)
            }
        })

        val mc = MinecraftClient.getInstance()
        HudRenderCallback.EVENT.register { ctx: DrawContext, _: RenderTickCounter ->
            val speed = ReSquakePlayer.currentSpeed * 20 * 40
            if (!ReSquakeMod.config.speedDeltaIndicatorEnabled)
                return@register

            val posX = mc.window.scaledWidth  / 2.0f
            val posY = mc.window.scaledHeight / 2.0f
            val text = "%.2f".format(speed)

            val centerOffset = mc.textRenderer.getWidth(text) / 2.0f
            ctx.drawTextWithShadow(mc.textRenderer, text, (posX - centerOffset).roundToInt(), (posY + 15).roundToInt(), ReSquakeMod.config.speedUnchangedColor)
        }
    }
}
