package com.anantaya.smartcgolem;

import net.fabricmc.api.ModInitializer;
import com.anantaya.smartcgolem.util.CategoryPolicy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartCopperGolem implements ModInitializer {
	public static final String MOD_ID = "smart-copper-golem";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		LOGGER.info("Copper Golems are smart now!");
		LOGGER.info("Category catalog ready: {} item IDs across {} categories",
				CategoryPolicy.all().size(), CategoryPolicy.categoryKeys().size());
	}
}