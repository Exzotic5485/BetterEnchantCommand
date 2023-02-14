package xyz.exzotic.betterenchantcommand;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.exzotic.betterenchantcommand.command.DisenchantCommand;

public class BetterEnchantCommand implements ModInitializer {
	public static final String MODID = "betterenchantcommand";

	public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

	@Override
	public void onInitialize() {
		LOGGER.info("Better Enchant Command loaded");

		CommandRegistrationCallback.EVENT.register((dispatcher, other) -> {
			DisenchantCommand.register(dispatcher);
		});
	}
}
