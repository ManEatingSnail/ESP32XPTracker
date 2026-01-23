package com.esp32tracker.xptracker;

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
import java.util.HashMap;
import java.util.Map;

@Slf4j
@PluginDescriptor(
		name = "ESP32 XP Tracker",
		description = "Sends real-time XP tracking data to an ESP32 device with TFT display. Displays skill progress, XP/hour, actions/hour, and time to level on external hardware.",
		tags = {"xp", "tracker", "esp32", "display", "hardware", "iot"}
)
@PluginDependency(XpTrackerPlugin.class)
public class ESP32TrackerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ESP32TrackerConfig config;

	@Inject
	private XpTrackerService xpTrackerService;

	@Inject
	private OkHttpClient httpClient; // Injected

	@Inject
	private com.google.gson.Gson gson; // Injected

	private Skill lastSkill = null;
	private long lastUpdateTime = 0;
	private static final long UPDATE_DELAY_MS = 1000; // 1 per second
	private boolean pendingUpdate = false;
	private Skill pendingSkill = null;

	private final Map<Skill, Integer> skillStartXp = new HashMap<>();

	@Override
	protected void startUp() throws Exception
	{
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
		if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN)
		{
			lastSkill = null;
			skillStartXp.clear();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		Skill skill = statChanged.getSkill();
		if (isSkillIgnored(skill))
		{
			return;
		}

		if (!skillStartXp.containsKey(skill))
		{
			skillStartXp.put(skill, client.getSkillExperience(skill));
		}

		lastSkill = skill;
		pendingUpdate = true;
		pendingSkill = skill;
	}

	private boolean isSkillIgnored(Skill skill)
	{
		switch (skill)
		{
			case ATTACK: return config.ignoreAttack();
			case STRENGTH: return config.ignoreStrength();
			case DEFENCE: return config.ignoreDefence();
			case RANGED: return config.ignoreRanged();
			case PRAYER: return config.ignorePrayer();
			case MAGIC: return config.ignoreMagic();
			case RUNECRAFT: return config.ignoreRunecraft();
			case CONSTRUCTION: return config.ignoreConstruction();
			case HITPOINTS: return config.ignoreHitpoints();
			case AGILITY: return config.ignoreAgility();
			case HERBLORE: return config.ignoreHerblore();
			case THIEVING: return config.ignoreThieving();
			case CRAFTING: return config.ignoreCrafting();
			case FLETCHING: return config.ignoreFletching();
			case SLAYER: return config.ignoreSlayer();
			case HUNTER: return config.ignoreHunter();
			case MINING: return config.ignoreMining();
			case SMITHING: return config.ignoreSmithing();
			case FISHING: return config.ignoreFishing();
			case COOKING: return config.ignoreCooking();
			case FIREMAKING: return config.ignoreFiremaking();
			case WOODCUTTING: return config.ignoreWoodcutting();
			case FARMING: return config.ignoreFarming();
			case SAILING: return config.ignoreSailing();
			default: return false;
		}
	}

	@Subscribe
	public void onGameTick(net.runelite.api.events.GameTick gameTick)
	{
		if (config.esp32IpAddress().isEmpty()) return;

		Skill skillToUpdate = pendingUpdate ? pendingSkill : lastSkill;
		if (skillToUpdate == null) return;

		long currentTime = System.currentTimeMillis();
		if (currentTime - lastUpdateTime < UPDATE_DELAY_MS) return;
		lastUpdateTime = currentTime;

		ESP32SkillData skillData = buildSkillData(skillToUpdate);
		sendToESP32(skillData);

		pendingUpdate = false;
	}

	private ESP32SkillData buildSkillData(Skill skill)
	{
		ESP32SkillData data = new ESP32SkillData();
		int currentXp = client.getSkillExperience(skill);
		int currentLevel = client.getRealSkillLevel(skill);

		data.skill = skill.getName();
		data.xp = currentXp;
		data.level = currentLevel;
		data.boosted_level = client.getBoostedSkillLevel(skill);

		data.xp_hr = xpTrackerService.getXpHr(skill);
		data.actions_hr = xpTrackerService.getActionsHr(skill);
		data.time_to_level = xpTrackerService.getTimeTilGoal(skill);

		Integer startXp = skillStartXp.get(skill);
		data.xp_gained = (startXp != null) ? currentXp - startXp : 0;

		if (currentLevel < 99)
		{
			int currentLevelMinXp = net.runelite.api.Experience.getXpForLevel(currentLevel);
			int nextLevelXp = net.runelite.api.Experience.getXpForLevel(currentLevel + 1);
			data.progress_percent = (float)(currentXp - currentLevelMinXp) / (nextLevelXp - currentLevelMinXp) * 100f;
		}
		else
		{
			data.progress_percent = 100f;
		}

		return data;
	}

	private void sendToESP32(ESP32SkillData data)
	{
		String url = String.format("http://%s/update", config.esp32IpAddress());
		String json = gson.toJson(data);

		// ✅ Corrected for OkHttp 4.x
		RequestBody body = RequestBody.create(
				MediaType.get("application/json"),
				json
		);

		Request request = new Request.Builder()
				.url(url)
				.post(body)
				.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e) { }

			@Override
			public void onResponse(Call call, Response response) { response.close(); }
		});
	}

	private void testConnection()
	{
		String url = String.format("http://%s/", config.esp32IpAddress());
		Request request = new Request.Builder().url(url).get().build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e) { }

			@Override
			public void onResponse(Call call, Response response) { response.close(); }
		});
	}

	@Provides
	ESP32TrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ESP32TrackerConfig.class);
	}

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