package com.esp32tracker.xptracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("esp32tracker")
public interface ESP32TrackerConfig extends Config
{
	@ConfigItem(
			keyName = "serverPort",
			name = "Server Port",
			description = "Port for the local HTTP server (default: 8080). The ESP32 will poll http://YOUR_PC_IP:PORT/update",
			position = 0
	)
	default int serverPort()
	{
		return 8080;
	}

	@ConfigSection(
			name = "Ignored Skills",
			description = "Select skills to exclude from tracking",
			position = 1
	)
	String ignoredSkillsSection = "ignoredSkills";

	@ConfigItem(keyName = "ignoreAttack", name = "Ignore Attack", description = "", section = "ignoredSkills", position = 1)
	default boolean ignoreAttack() { return false; }

	@ConfigItem(keyName = "ignoreStrength", name = "Ignore Strength", description = "", section = "ignoredSkills", position = 2)
	default boolean ignoreStrength() { return false; }

	@ConfigItem(keyName = "ignoreDefence", name = "Ignore Defence", description = "", section = "ignoredSkills", position = 3)
	default boolean ignoreDefence() { return false; }

	@ConfigItem(keyName = "ignoreRanged", name = "Ignore Ranged", description = "", section = "ignoredSkills", position = 4)
	default boolean ignoreRanged() { return false; }

	@ConfigItem(keyName = "ignorePrayer", name = "Ignore Prayer", description = "", section = "ignoredSkills", position = 5)
	default boolean ignorePrayer() { return false; }

	@ConfigItem(keyName = "ignoreMagic", name = "Ignore Magic", description = "", section = "ignoredSkills", position = 6)
	default boolean ignoreMagic() { return false; }

	@ConfigItem(keyName = "ignoreRunecraft", name = "Ignore Runecraft", description = "", section = "ignoredSkills", position = 7)
	default boolean ignoreRunecraft() { return false; }

	@ConfigItem(keyName = "ignoreConstruction", name = "Ignore Construction", description = "", section = "ignoredSkills", position = 8)
	default boolean ignoreConstruction() { return false; }

	@ConfigItem(keyName = "ignoreHitpoints", name = "Ignore Hitpoints", description = "", section = "ignoredSkills", position = 9)
	default boolean ignoreHitpoints() { return false; }

	@ConfigItem(keyName = "ignoreAgility", name = "Ignore Agility", description = "", section = "ignoredSkills", position = 10)
	default boolean ignoreAgility() { return false; }

	@ConfigItem(keyName = "ignoreHerblore", name = "Ignore Herblore", description = "", section = "ignoredSkills", position = 11)
	default boolean ignoreHerblore() { return false; }

	@ConfigItem(keyName = "ignoreThieving", name = "Ignore Thieving", description = "", section = "ignoredSkills", position = 12)
	default boolean ignoreThieving() { return false; }

	@ConfigItem(keyName = "ignoreCrafting", name = "Ignore Crafting", description = "", section = "ignoredSkills", position = 13)
	default boolean ignoreCrafting() { return false; }

	@ConfigItem(keyName = "ignoreFletching", name = "Ignore Fletching", description = "", section = "ignoredSkills", position = 14)
	default boolean ignoreFletching() { return false; }

	@ConfigItem(keyName = "ignoreSlayer", name = "Ignore Slayer", description = "", section = "ignoredSkills", position = 15)
	default boolean ignoreSlayer() { return false; }

	@ConfigItem(keyName = "ignoreHunter", name = "Ignore Hunter", description = "", section = "ignoredSkills", position = 16)
	default boolean ignoreHunter() { return false; }

	@ConfigItem(keyName = "ignoreMining", name = "Ignore Mining", description = "", section = "ignoredSkills", position = 17)
	default boolean ignoreMining() { return false; }

	@ConfigItem(keyName = "ignoreSmithing", name = "Ignore Smithing", description = "", section = "ignoredSkills", position = 18)
	default boolean ignoreSmithing() { return false; }

	@ConfigItem(keyName = "ignoreFishing", name = "Ignore Fishing", description = "", section = "ignoredSkills", position = 19)
	default boolean ignoreFishing() { return false; }

	@ConfigItem(keyName = "ignoreCooking", name = "Ignore Cooking", description = "", section = "ignoredSkills", position = 20)
	default boolean ignoreCooking() { return false; }

	@ConfigItem(keyName = "ignoreFiremaking", name = "Ignore Firemaking", description = "", section = "ignoredSkills", position = 21)
	default boolean ignoreFiremaking() { return false; }

	@ConfigItem(keyName = "ignoreWoodcutting", name = "Ignore Woodcutting", description = "", section = "ignoredSkills", position = 22)
	default boolean ignoreWoodcutting() { return false; }

	@ConfigItem(keyName = "ignoreFarming", name = "Ignore Farming", description = "", section = "ignoredSkills", position = 23)
	default boolean ignoreFarming() { return false; }

	@ConfigItem(keyName = "ignoreSailing", name = "Ignore Sailing", description = "", section = "ignoredSkills", position = 24)
	default boolean ignoreSailing() { return false; }
}