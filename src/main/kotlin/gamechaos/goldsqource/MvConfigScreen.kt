package gamechaos.goldsqource

import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder
import dev.isxander.yacl3.api.controller.ColorControllerBuilder
import dev.isxander.yacl3.api.controller.DoubleFieldControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder
import net.minecraft.text.Text
import net.minecraft.client.gui.screen.Screen
import java.awt.Color

fun generateConfigScreen(parent: Screen?): Screen
	= YetAnotherConfigLib.createBuilder()
		.save(MvMod.config::save)
		.title(Text.of(MvMod.NAME))
		.category(
			ConfigCategory.createBuilder()
			.name(Text.of("General"))
			.tooltip(Text.of("Changes to minecraft made by this mod"))
			.group(
				OptionGroup.createBuilder()
				.name(Text.of("Movement"))
				.collapsed(false)
				.option(
					Option.createBuilder<Boolean>()
					.name(Text.of("Quake-style movement"))
					.description(OptionDescription.of(Text.of("Enables/disables all movement changes made by this mod")))
					.binding(MvConfig.DEFAULT_QUAKE_MOVEMENT_ENABLED,
						{ MvMod.config.quakeMovementEnabled },
						{ MvMod.config.quakeMovementEnabled = it })
					.controller(BooleanControllerBuilder::create)
					.build())
				.build())
			
			.group(
				OptionGroup.createBuilder()
				.name(Text.of("Miscellaneous"))
				.collapsed(false)
				
				.option(
						Option.createBuilder<Boolean>()
					.name(Text.of("Buffered jump"))
					.description(OptionDescription.of(Text.of("Requires you to repress your jump key after doing a jump, like in Quake.")))
					.binding(MvConfig.DEFAULT_BUFFERED_JUMP,
						{ MvMod.config.bufferedJump },
						{ MvMod.config.bufferedJump = it })
					.controller(BooleanControllerBuilder::create)
					.build())
				
				.option(
					Option.createBuilder<Int>()
					.name(Text.of("Jump particles"))
					.description(OptionDescription.of(Text.of("Amount of particles that spawn when you hit the ground (0 to disable)")))
					.binding(MvConfig.DEFAULT_JUMP_PARTICLES,
						{ MvMod.config.jumpParticles },
						{ MvMod.config.jumpParticles = it })
					.controller(IntegerFieldControllerBuilder::create)
					.build())
				.build())
			
			.group(
				OptionGroup.createBuilder()
				.name(Text.of("Speed indicator"))
				.collapsed(false)
				.option(
					Option.createBuilder<Boolean>()
					.name(Text.of("Delta indicator"))
					.description(OptionDescription.of(Text.of("Enables/disables the display of change in speed")))
					.binding(MvConfig.DEFAULT_SPEED_DELTA_INDICATOR_ENABLED,
						{ MvMod.config.speedDeltaIndicatorEnabled },
						{ MvMod.config.speedDeltaIndicatorEnabled = it })
					.controller(BooleanControllerBuilder::create)
					.build())
				
				.option(
					Option.createBuilder<Boolean>()
					.name(Text.of("Difference indicator"))
					.description(OptionDescription.of(Text.of("Enables/disables the display of +/- speed from last hop")))
					.binding(MvConfig.DEFAULT_SPEED_DIFF_INDICATOR_ENABLED,
							 { MvMod.config.speedDiffIndicatorEnabled },
							 { MvMod.config.speedDiffIndicatorEnabled = it })
					.controller(BooleanControllerBuilder::create)
					.build())
				
				.option(
					Option.createBuilder<Double>()
					.name(Text.of("Speed delta threshold"))
					.description(OptionDescription.of(Text.of("Minimum speed needed for indicator to appear")))
					.binding(MvConfig.DEFAULT_SPEED_DELTA_THRESHOLD,
						{ MvMod.config.speedDeltaThreshold },
						{ MvMod.config.speedDeltaThreshold = it })
					.controller(DoubleFieldControllerBuilder::create)
					.build())
				
				.option(
					Option.createBuilder<Color>()
					.name(Text.of("Speed gain color"))
					.description(OptionDescription.of(Text.of("Color of speed delta indicator when you gain additional speed")))
					.binding(MvConfig.SPEED_GAIN_COLOR,
						{ Color(MvMod.config.speedGainColor) },
						{ MvMod.config.speedGainColor = it.rgb })
					.controller(ColorControllerBuilder::create)
					.build())
				
				.option(
						Option.createBuilder<Color>()
					.name(Text.of("Speed loss color"))
					.description(OptionDescription.of(Text.of("Color of speed delta indicator when you lose gained speed")))
					.binding(MvConfig.SPEED_LOSS_COLOR,
						{ Color(MvMod.config.speedLossColor) },
						{ MvMod.config.speedLossColor = it.rgb })
					.controller(ColorControllerBuilder::create)
					.build())
				
				.option(
					Option.createBuilder<Color>()
					.name(Text.of("Speed unchanged color"))
					.description(OptionDescription.of(Text.of("Color of speed delta indicator when your speed remains the same")))
					.binding(MvConfig.SPEED_UNCHANGED_COLOR,
						{ Color(MvMod.config.speedUnchangedColor) },
						{ MvMod.config.speedUnchangedColor = it.rgb })
					.controller(ColorControllerBuilder::create)
					.build())
				.build())
			.build())
		.build()
		.generateScreen(parent)
