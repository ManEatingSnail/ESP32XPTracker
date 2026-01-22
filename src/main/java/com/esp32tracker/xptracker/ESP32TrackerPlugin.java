package com.esp32tracker.xptracker;

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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@PluginDescriptor(
		name = "ESP32 XP Tracker",
		description = "Sends real-time XP tracking data to an ESP32 device with TFT display.",
		tags = {"xp", "tracker", "esp32", "hardware", "iot"},
		enabledByDefault = false
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

	private final OkHttpClient httpClient = new OkHttpClient.Builder()
			.connectTimeout(5, TimeUnit.SECONDS)
			.writeTimeout(5, TimeUnit.SECONDS)
			.readTimeout(5, TimeUnit.SECONDS)
			.build();

	private final Gson gson = new Gson();

	private Skill lastSkill;
	private long lastUpdateTime;

	private static final long UPDATE_DELAY_MS = 1000;

	private boolean pendingUpdate;
	private Skill pendingSkill;

	private final Map<Skill, Integer> skillStartXp = new HashMap<>();

	@Override
	protected void startUp()
	{
		if (!config.esp32IpAddress().isEmpty())
		{
			testConnection();
		}
	}

	@Override
	protected void shutDown()
	{
		lastSkill = null;
		skillStartXp.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			lastSkill = null;
			skillStartXp.clear();
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		Skill skill = event.getSkill();

		if (skill == Skill.OVERALL || isSkillIgnored(skill))
		{
			return;
		}

		skillStartXp.putIfAbsent(skill, client.getSkillExperience(skill));
		lastSkill = skill;
		pendingUpdate = true;
		pendingSkill = skill;
	}

	@Subscribe
	public void onGameTick(net.runelite.api.events.GameTick tick)
	{
		if (config.esp32IpAddress().isEmpty())
		{
			return;
		}

		Skill skill = pendingUpdate ? pendingSkill : lastSkill;
		if (skill == null)
		{
			return;
		}

		long now = System.currentTimeMillis();
		if (now - lastUpdateTime < UPDATE_DELAY_MS)
		{
			return;
		}

		lastUpdateTime = now;
		sendToESP32(buildSkillData(skill));
		pendingUpdate = false;
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

		int startXp = skillStartXp.getOrDefault(skill, currentXp);
		data.xp_gained = currentXp - startXp;

		return data;
	}

	private void sendToESP32(ESP32SkillData data)
	{
		String url = "http://" + config.esp32IpAddress() + "/update";

		RequestBody body = RequestBody.create(
				MediaType.parse("application/json"),
				gson.toJson(data)
		);

		Request request = new Request.Builder().url(url).post(body).build();
		httpClient.newCall(request).enqueue(new Callback()
		{
			public void onFailure(Call call, IOException ignored) {}
			public void onResponse(Call call, Response response) { response.close(); }
		});
	}

	private void testConnection()
	{
		Request request = new Request.Builder()
				.url("http://" + config.esp32IpAddress() + "/")
				.get()
				.build();

		httpClient.newCall(request).enqueue(new Callback()
		{
			public void onFailure(Call call, IOException ignored) {}
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
	}
}