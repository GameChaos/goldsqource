package gamechaos.goldsqource

import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder
import dev.isxander.yacl3.api.controller.ColorControllerBuilder
import dev.isxander.yacl3.api.controller.DoubleFieldControllerBuilder
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder
import net.minecraft.network.chat.Component
import net.minecraft.client.gui.screens.Screen
import java.awt.Color

fun generateConfigScreen(parent: Screen?): Screen
	= YetAnotherConfigLib.createBuilder()
		.save(MvMod.config::save)
		.title(Component.nullToEmpty(MvMod.NAME))
		.category(
			ConfigCategory.createBuilder()
			.name(Component.nullToEmpty("General"))
			.tooltip(Component.nullToEmpty("Changes to minecraft made by this mod"))
			.group(
				OptionGroup.createBuilder()
				.name(Component.nullToEmpty("Movement"))
				.collapsed(false)
				.option(
					Option.createBuilder<Boolean>()
					.name(Component.nullToEmpty("Quake-style movement"))
					.description(OptionDescription.of(Component.nullToEmpty("Enables/disables all movement changes made by this mod")))
					.binding(MvConfig.DEFAULT_QUAKE_MOVEMENT_ENABLED,
						{ MvMod.config.quakeMovementEnabled },
						{ MvMod.config.quakeMovementEnabled = it })
					.controller(BooleanControllerBuilder::create)
					.build())

				.option(
					Option.createBuilder<Boolean>()
					.name(Component.nullToEmpty("Enable speed cap"))
					.description(OptionDescription.of(Component.nullToEmpty("Enables/disables hard speed cap")))
					.binding(MvConfig.DEFAULT_QUAKE_MOVEMENT_ENABLED,
						{ MvMod.config.speedCapEnabled },
						{ MvMod.config.speedCapEnabled = it })
					.controller(BooleanControllerBuilder::create)
					.build())
				.build())
			
			.group(
				OptionGroup.createBuilder()
				.name(Component.nullToEmpty("Miscellaneous"))
				.collapsed(false)
				
				.option(
						Option.createBuilder<Boolean>()
					.name(Component.nullToEmpty("Buffered jump"))
					.description(OptionDescription.of(Component.nullToEmpty("Requires you to repress your jump key after doing a jump, like in Quake.")))
					.binding(MvConfig.DEFAULT_BUFFERED_JUMP,
						{ MvMod.config.bufferedJump },
						{ MvMod.config.bufferedJump = it })
					.controller(BooleanControllerBuilder::create)
					.build())
				
				.option(
					Option.createBuilder<Int>()
					.name(Component.nullToEmpty("Jump particles"))
					.description(OptionDescription.of(Component.nullToEmpty("Amount of particles that spawn when you hit the ground (0 to disable)")))
					.binding(MvConfig.DEFAULT_JUMP_PARTICLES,
						{ MvMod.config.jumpParticles },
						{ MvMod.config.jumpParticles = it })
					.controller(IntegerFieldControllerBuilder::create)
					.build())
				.build())
			
			.group(
				OptionGroup.createBuilder()
				.name(Component.nullToEmpty("Speed indicator"))
				.collapsed(false)
				.option(
					Option.createBuilder<Boolean>()
					.name(Component.nullToEmpty("Delta indicator"))
					.description(OptionDescription.of(Component.nullToEmpty("Enables/disables the display of change in speed")))
					.binding(MvConfig.DEFAULT_SPEED_DELTA_INDICATOR_ENABLED,
						{ MvMod.config.speedDeltaIndicatorEnabled },
						{ MvMod.config.speedDeltaIndicatorEnabled = it })
					.controller(BooleanControllerBuilder::create)
					.build())
				
				.option(
					Option.createBuilder<Boolean>()
					.name(Component.nullToEmpty("Difference indicator"))
					.description(OptionDescription.of(Component.nullToEmpty("Enables/disables the display of +/- speed from last hop")))
					.binding(MvConfig.DEFAULT_SPEED_DIFF_INDICATOR_ENABLED,
							 { MvMod.config.speedDiffIndicatorEnabled },
							 { MvMod.config.speedDiffIndicatorEnabled = it })
					.controller(BooleanControllerBuilder::create)
					.build())
				
				.option(
					Option.createBuilder<Double>()
					.name(Component.nullToEmpty("Speed delta threshold"))
					.description(OptionDescription.of(Component.nullToEmpty("Minimum speed needed for indicator to appear")))
					.binding(MvConfig.DEFAULT_SPEED_DELTA_THRESHOLD,
						{ MvMod.config.speedDeltaThreshold },
						{ MvMod.config.speedDeltaThreshold = it })
					.controller(DoubleFieldControllerBuilder::create)
					.build())
				
				.option(
					Option.createBuilder<Color>()
					.name(Component.nullToEmpty("Speed gain color"))
					.description(OptionDescription.of(Component.nullToEmpty("Color of speed delta indicator when you gain additional speed")))
					.binding(MvConfig.SPEED_GAIN_COLOR,
						{ Color(MvMod.config.speedGainColor) },
						{ MvMod.config.speedGainColor = it.rgb })
					.controller(ColorControllerBuilder::create)
					.build())
				
				.option(
						Option.createBuilder<Color>()
					.name(Component.nullToEmpty("Speed loss color"))
					.description(OptionDescription.of(Component.nullToEmpty("Color of speed delta indicator when you lose gained speed")))
					.binding(MvConfig.SPEED_LOSS_COLOR,
						{ Color(MvMod.config.speedLossColor) },
						{ MvMod.config.speedLossColor = it.rgb })
					.controller(ColorControllerBuilder::create)
					.build())
				
				.option(
					Option.createBuilder<Color>()
					.name(Component.nullToEmpty("Speed unchanged color"))
					.description(OptionDescription.of(Component.nullToEmpty("Color of speed delta indicator when your speed remains the same")))
					.binding(MvConfig.SPEED_UNCHANGED_COLOR,
						{ Color(MvMod.config.speedUnchangedColor) },
						{ MvMod.config.speedUnchangedColor = it.rgb })
					.controller(ColorControllerBuilder::create)
					.build())
				.build())
			.build())
		.build()
		.generateScreen(parent)
