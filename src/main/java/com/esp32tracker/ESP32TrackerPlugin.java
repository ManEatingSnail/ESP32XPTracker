package net.runelite.client.plugins.esp32tracker;

import com.google.gson.Gson;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.xptracker.XpTrackerPlugin;
import net.runelite.client.plugins.xptracker.XpTrackerService;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@PluginDescriptor(
		name = "ESP32 XP Tracker",
		description = "Sends real-time XP tracking data to an ESP32 device with TFT display. Displays skill progress, XP/hour, actions/hour, and time to level on external hardware.",
		tags = {"xp", "tracker", "esp32", "display", "hardware", "iot"},
		enabledByDefault = false
)
@PluginDependency(XpTrackerPlugin.class)
public class ESP32TrackerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private net.runelite.client.plugins.esp32tracker.ESP32TrackerConfig config;

	@Inject
	private XpTrackerService xpTrackerService;

	private final OkHttpClient httpClient = new OkHttpClient.Builder()
			.connectTimeout(5, TimeUnit.SECONDS)
			.writeTimeout(5, TimeUnit.SECONDS)
			.readTimeout(5, TimeUnit.SECONDS)
			.build();

	private final Gson gson = new Gson();

	private Skill lastSkill = null;
	private long lastUpdateTime = 0;
	private static final long UPDATE_DELAY_MS = 1000; // Throttle updates to 1 per second
	private boolean pendingUpdate = false;
	private Skill pendingSkill = null;

	// Track XP gained ourselves since XpTrackerService doesn't expose it
	private final java.util.Map<Skill, Integer> skillStartXp = new java.util.HashMap<>();

	@Override
	protected void startUp() throws Exception
	{
		// Test connection if IP is configured
		if (!config.esp32IpAddress().isEmpty())
		{
			testConnection();
		}
	}

	@Override
	protected void shutDown() throws Exception
	{
		lastSkill = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		GameState gameState = gameStateChanged.getGameState();

		if (gameState == GameState.LOGIN_SCREEN)
		{
			lastSkill = null;
			skillStartXp.clear();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		Skill skill = statChanged.getSkill();

		// Skip OVERALL
		if (skill == Skill.OVERALL)
		{
			return;
		}

		// Check if skill is ignored
		if (isSkillIgnored(skill))
		{
			return;
		}

		// Track starting XP if this is the first time we've seen this skill
		if (!skillStartXp.containsKey(skill))
		{
			skillStartXp.put(skill, client.getSkillExperience(skill));
		}

		// Update last skill being trained
		lastSkill = skill;

		// Mark that we need to send an update on the next game tick
		pendingUpdate = true;
		pendingSkill = skill;
	}

	// Check if a skill should be ignored based on config
	private boolean isSkillIgnored(Skill skill)
	{
		switch (skill)
		{
			case ATTACK:
				return config.ignoreAttack();
			case STRENGTH:
				return config.ignoreStrength();
			case DEFENCE:
				return config.ignoreDefence();
			case RANGED:
				return config.ignoreRanged();
			case PRAYER:
				return config.ignorePrayer();
			case MAGIC:
				return config.ignoreMagic();
			case RUNECRAFT:
				return config.ignoreRunecraft();
			case CONSTRUCTION:
				return config.ignoreConstruction();
			case HITPOINTS:
				return config.ignoreHitpoints();
			case AGILITY:
				return config.ignoreAgility();
			case HERBLORE:
				return config.ignoreHerblore();
			case THIEVING:
				return config.ignoreThieving();
			case CRAFTING:
				return config.ignoreCrafting();
			case FLETCHING:
				return config.ignoreFletching();
			case SLAYER:
				return config.ignoreSlayer();
			case HUNTER:
				return config.ignoreHunter();
			case MINING:
				return config.ignoreMining();
			case SMITHING:
				return config.ignoreSmithing();
			case FISHING:
				return config.ignoreFishing();
			case COOKING:
				return config.ignoreCooking();
			case FIREMAKING:
				return config.ignoreFiremaking();
			case WOODCUTTING:
				return config.ignoreWoodcutting();
			case FARMING:
				return config.ignoreFarming();
			case SAILING:
				return config.ignoreSailing();
			default:
				return false;
		}
	}

	@Subscribe
	public void onGameTick(net.runelite.api.events.GameTick gameTick)
	{
		if (config.esp32IpAddress().isEmpty())
		{
			return;
		}

		// If we have a last skill being trained, send updates every tick
		// This keeps XP/hr decaying when idle, just like the XP Tracker plugin
		Skill skillToUpdate = pendingUpdate ? pendingSkill : lastSkill;

		if (skillToUpdate == null)
		{
			return;
		}

		// Throttle updates
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastUpdateTime < UPDATE_DELAY_MS)
		{
			return;
		}
		lastUpdateTime = currentTime;

		// Build and send data - XpTrackerService has been updated by now
		ESP32SkillData skillData = buildSkillData(skillToUpdate);
		sendToESP32(skillData);

		pendingUpdate = false;
	}

	private ESP32SkillData buildSkillData(Skill skill)
	{
		ESP32SkillData data = new ESP32SkillData();

		// Basic skill info from client
		int currentXp = client.getSkillExperience(skill);
		int currentLevel = client.getRealSkillLevel(skill);

		data.skill = skill.getName();
		data.xp = currentXp;
		data.level = currentLevel;
		data.boosted_level = client.getBoostedSkillLevel(skill);

		// Use XpTrackerService for rates
		data.xp_hr = xpTrackerService.getXpHr(skill);
		data.actions_hr = xpTrackerService.getActionsHr(skill);
		data.time_to_level = xpTrackerService.getTimeTilGoal(skill);

		// Calculate XP gained from our own tracking
		Integer startXp = skillStartXp.get(skill);
		if (startXp != null)
		{
			data.xp_gained = currentXp - startXp;
		}
		else
		{
			data.xp_gained = 0;
		}

		// Calculate progress percentage for progress bar
		if (currentLevel < 99)
		{
			int currentLevelMinXp = net.runelite.api.Experience.getXpForLevel(currentLevel);
			int nextLevelXp = net.runelite.api.Experience.getXpForLevel(currentLevel + 1);
			int xpIntoLevel = currentXp - currentLevelMinXp;
			int xpNeededForLevel = nextLevelXp - currentLevelMinXp;
			data.progress_percent = (float) xpIntoLevel / xpNeededForLevel * 100.0f;
		}
		else
		{
			data.progress_percent = 100.0f;
		}

		return data;
	}

	private void sendToESP32(ESP32SkillData data)
	{
		String url = String.format("http://%s/update", config.esp32IpAddress());
		String json = gson.toJson(data);

		RequestBody body = RequestBody.create(
				MediaType.parse("application/json"),
				json
		);

		Request request = new Request.Builder()
				.url(url)
				.post(body)
				.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				// Silently fail - no need to spam logs
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				response.close();
			}
		});
	}

	private void testConnection()
	{
		String url = String.format("http://%s/", config.esp32IpAddress());

		Request request = new Request.Builder()
				.url(url)
				.get()
				.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				// Connection test failed - silently continue
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				response.close();
			}
		});
	}

	@Provides
	net.runelite.client.plugins.esp32tracker.ESP32TrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(net.runelite.client.plugins.esp32tracker.ESP32TrackerConfig.class);
	}

	// Data class for ESP32 communication
	private static class ESP32SkillData
	{
		String skill;
		int xp;
		int level;
		int boosted_level;
		int xp_hr;
		int actions_hr;
		int xp_gained;
		String time_to_level;
		float progress_percent;
	}
}