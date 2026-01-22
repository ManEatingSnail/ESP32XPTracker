package net.runelite.client.plugins.esp32tracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("esp32tracker")
public interface ESP32TrackerConfig extends Config
{
	@ConfigItem(
			keyName = "esp32IpAddress",
			name = "ESP32 IP Address",
			description = "Local network IP address of your ESP32 device (shown on ESP32 screen at startup)",
			position = 1
	)
	default String esp32IpAddress()
	{
		return "";
	}

	@ConfigSection(
			name = "Ignored Skills",
			description = "Select skills to exclude from tracking (they won't be sent to the ESP32)",
			position = 2
	)
	String ignoredSkillsSection = "ignoredSkills";

	@ConfigItem(
			keyName = "ignoreAttack",
			name = "Ignore Attack",
			description = "Don't track Attack",
			section = "ignoredSkills",
			position = 1
	)
	default boolean ignoreAttack() { return false; }

	@ConfigItem(
			keyName = "ignoreStrength",
			name = "Ignore Strength",
			description = "Don't track Strength",
			section = "ignoredSkills",
			position = 2
	)
	default boolean ignoreStrength() { return false; }

	@ConfigItem(
			keyName = "ignoreDefence",
			name = "Ignore Defence",
			description = "Don't track Defence",
			section = "ignoredSkills",
			position = 3
	)
	default boolean ignoreDefence() { return false; }

	@ConfigItem(
			keyName = "ignoreRanged",
			name = "Ignore Ranged",
			description = "Don't track Ranged",
			section = "ignoredSkills",
			position = 4
	)
	default boolean ignoreRanged() { return false; }

	@ConfigItem(
			keyName = "ignorePrayer",
			name = "Ignore Prayer",
			description = "Don't track Prayer",
			section = "ignoredSkills",
			position = 5
	)
	default boolean ignorePrayer() { return false; }

	@ConfigItem(
			keyName = "ignoreMagic",
			name = "Ignore Magic",
			description = "Don't track Magic",
			section = "ignoredSkills",
			position = 6
	)
	default boolean ignoreMagic() { return false; }

	@ConfigItem(
			keyName = "ignoreRunecraft",
			name = "Ignore Runecraft",
			description = "Don't track Runecraft",
			section = "ignoredSkills",
			position = 7
	)
	default boolean ignoreRunecraft() { return false; }

	@ConfigItem(
			keyName = "ignoreConstruction",
			name = "Ignore Construction",
			description = "Don't track Construction",
			section = "ignoredSkills",
			position = 8
	)
	default boolean ignoreConstruction() { return false; }

	@ConfigItem(
			keyName = "ignoreHitpoints",
			name = "Ignore Hitpoints",
			description = "Don't track Hitpoints",
			section = "ignoredSkills",
			position = 9
	)
	default boolean ignoreHitpoints() { return false; }

	@ConfigItem(
			keyName = "ignoreAgility",
			name = "Ignore Agility",
			description = "Don't track Agility",
			section = "ignoredSkills",
			position = 10
	)
	default boolean ignoreAgility() { return false; }

	@ConfigItem(
			keyName = "ignoreHerblore",
			name = "Ignore Herblore",
			description = "Don't track Herblore",
			section = "ignoredSkills",
			position = 11
	)
	default boolean ignoreHerblore() { return false; }

	@ConfigItem(
			keyName = "ignoreThieving",
			name = "Ignore Thieving",
			description = "Don't track Thieving",
			section = "ignoredSkills",
			position = 12
	)
	default boolean ignoreThieving() { return false; }

	@ConfigItem(
			keyName = "ignoreCrafting",
			name = "Ignore Crafting",
			description = "Don't track Crafting",
			section = "ignoredSkills",
			position = 13
	)
	default boolean ignoreCrafting() { return false; }

	@ConfigItem(
			keyName = "ignoreFletching",
			name = "Ignore Fletching",
			description = "Don't track Fletching",
			section = "ignoredSkills",
			position = 14
	)
	default boolean ignoreFletching() { return false; }

	@ConfigItem(
			keyName = "ignoreSlayer",
			name = "Ignore Slayer",
			description = "Don't track Slayer",
			section = "ignoredSkills",
			position = 15
	)
	default boolean ignoreSlayer() { return false; }

	@ConfigItem(
			keyName = "ignoreHunter",
			name = "Ignore Hunter",
			description = "Don't track Hunter",
			section = "ignoredSkills",
			position = 16
	)
	default boolean ignoreHunter() { return false; }

	@ConfigItem(
			keyName = "ignoreMining",
			name = "Ignore Mining",
			description = "Don't track Mining",
			section = "ignoredSkills",
			position = 17
	)
	default boolean ignoreMining() { return false; }

	@ConfigItem(
			keyName = "ignoreSmithing",
			name = "Ignore Smithing",
			description = "Don't track Smithing",
			section = "ignoredSkills",
			position = 18
	)
	default boolean ignoreSmithing() { return false; }

	@ConfigItem(
			keyName = "ignoreFishing",
			name = "Ignore Fishing",
			description = "Don't track Fishing",
			section = "ignoredSkills",
			position = 19
	)
	default boolean ignoreFishing() { return false; }

	@ConfigItem(
			keyName = "ignoreCooking",
			name = "Ignore Cooking",
			description = "Don't track Cooking",
			section = "ignoredSkills",
			position = 20
	)
	default boolean ignoreCooking() { return false; }

	@ConfigItem(
			keyName = "ignoreFiremaking",
			name = "Ignore Firemaking",
			description = "Don't track Firemaking",
			section = "ignoredSkills",
			position = 21
	)
	default boolean ignoreFiremaking() { return false; }

	@ConfigItem(
			keyName = "ignoreWoodcutting",
			name = "Ignore Woodcutting",
			description = "Don't track Woodcutting",
			section = "ignoredSkills",
			position = 22
	)
	default boolean ignoreWoodcutting() { return false; }

	@ConfigItem(
			keyName = "ignoreFarming",
			name = "Ignore Farming",
			description = "Don't track Farming",
			section = "ignoredSkills",
			position = 23
	)
	default boolean ignoreFarming() { return false; }

	@ConfigItem(
			keyName = "ignoreSailing",
			name = "Ignore Sailing",
			description = "Don't track Sailing",
			section = "ignoredSkills",
			position = 24
	)
	default boolean ignoreSailing() { return false; }
}