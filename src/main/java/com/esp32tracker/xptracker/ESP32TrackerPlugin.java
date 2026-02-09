package com.esp32tracker.xptracker;

import com.google.inject.Provides;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.xptracker.XpTrackerPlugin;
import net.runelite.client.plugins.xptracker.XpTrackerService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

@Slf4j
@PluginDescriptor(
		name = "ESP32 XP Tracker",
		description = "Runs a local HTTP server that serves real-time XP data as JSON for external hardware displays.",
		tags = {"xp", "tracker", "esp32", "display", "hardware", "iot", "json", "api"}
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
	private com.google.gson.Gson gson;

	private HttpServer httpServer;
	private final Map<Skill, Integer> skillStartXp = new HashMap<>();
	private final Map<Skill, Long> skillLastGainTime = new HashMap<>();
	private String cachedJson = "{}";
	private String playerName = "";
	private boolean loggedIn = false;

	// Track which skill most recently gained real XP (not boost changes)
	private Skill activeSkill = null;
	// Track previous XP per skill to detect real gains vs boost-only changes
	private final Map<Skill, Integer> skillPreviousXp = new HashMap<>();

	@Override
	protected void startUp() throws Exception
	{
		startHttpServer();
	}

	@Override
	protected void shutDown() throws Exception
	{
		stopHttpServer();
		skillStartXp.clear();
		skillLastGainTime.clear();
		skillPreviousXp.clear();
		activeSkill = null;
		loggedIn = false;
		playerName = "";
	}

	private void startHttpServer()
	{
		try
		{
			httpServer = HttpServer.create(new InetSocketAddress("0.0.0.0", config.serverPort()), 0);
			httpServer.setExecutor(Executors.newFixedThreadPool(2));
			httpServer.createContext("/update", this::handleUpdate);
			httpServer.createContext("/", this::handleRoot);
			httpServer.start();
			log.info("ESP32 Tracker HTTP server started on port {}", config.serverPort());
		}
		catch (IOException e)
		{
			log.error("Failed to start ESP32 Tracker HTTP server on port {}", config.serverPort(), e);
		}
	}

	private void stopHttpServer()
	{
		if (httpServer != null)
		{
			httpServer.stop(0);
			httpServer = null;
			log.info("ESP32 Tracker HTTP server stopped");
		}
	}

	private void handleRoot(HttpExchange exchange) throws IOException
	{
		String html = "<!DOCTYPE html><html><head><title>ESP32 XP Tracker</title>"
				+ "<style>body{font-family:monospace;background:#1a1a2e;color:#eee;padding:20px;}"
				+ "a{color:#0ff;}pre{background:#16213e;padding:15px;border-radius:8px;overflow:auto;}</style></head>"
				+ "<body><h1>OSRS ESP32 XP Tracker</h1>"
				+ "<p>Status: " + (loggedIn ? "Logged in as <strong>" + playerName + "</strong>" : "Not logged in") + "</p>"
				+ "<p>API endpoint: <a href=\"/update\">/update</a></p>"
				+ "<h2>Current Data:</h2><pre>" + cachedJson + "</pre>"
				+ "<script>setTimeout(()=>location.reload(),2000)</script>"
				+ "</body></html>";

		byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
		exchange.sendResponseHeaders(200, bytes.length);
		try (OutputStream os = exchange.getResponseBody())
		{
			os.write(bytes);
		}
	}

	private void handleUpdate(HttpExchange exchange) throws IOException
	{
		byte[] bytes = cachedJson.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().set("Content-Type", "application/json");
		exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
		exchange.sendResponseHeaders(200, bytes.length);
		try (OutputStream os = exchange.getResponseBody())
		{
			os.write(bytes);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			loggedIn = true;
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			loggedIn = false;
			playerName = "";
			skillStartXp.clear();
			skillLastGainTime.clear();
			skillPreviousXp.clear();
			activeSkill = null;
			cachedJson = "{}";
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

		int currentXp = client.getSkillExperience(skill);

		// Only treat this as a real gain if XP actually increased.
		// StatChanged also fires for boosted level changes (HP regen,
		// potion effects wearing off, etc.) where XP stays the same.
		Integer previousXp = skillPreviousXp.get(skill);
		boolean xpActuallyGained = (previousXp == null || currentXp > previousXp);
		skillPreviousXp.put(skill, currentXp);

		if (!xpActuallyGained)
		{
			// Boost change only — do NOT update activeSkill
			return;
		}

		// Record start XP on first real gain
		if (!skillStartXp.containsKey(skill))
		{
			skillStartXp.put(skill, currentXp);
		}

		// Track when this skill last gained real XP
		skillLastGainTime.put(skill, System.currentTimeMillis());
		activeSkill = skill;
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		if (!loggedIn || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		if (playerName.isEmpty() && client.getLocalPlayer() != null)
		{
			playerName = client.getLocalPlayer().getName();
		}

		rebuildJson();
	}

	private void rebuildJson()
	{
		Map<String, Object> payload = new HashMap<>();
		payload.put("player", playerName);
		payload.put("logged_in", loggedIn);
		payload.put("active_skill", activeSkill != null ? activeSkill.getName() : null);
		payload.put("timestamp", System.currentTimeMillis());

		Map<String, Object> skillsMap = new HashMap<>();

		for (Skill skill : Skill.values())
		{
			if (skill == Skill.OVERALL || isSkillIgnored(skill))
			{
				continue;
			}

			int currentXp = client.getSkillExperience(skill);
			int currentLevel = client.getRealSkillLevel(skill);
			int boostedLevel = client.getBoostedSkillLevel(skill);

			Map<String, Object> skillData = new HashMap<>();
			skillData.put("xp", currentXp);
			skillData.put("level", currentLevel);
			skillData.put("boosted_level", boostedLevel);

			int xpHr = xpTrackerService.getXpHr(skill);
			int actionsHr = xpTrackerService.getActionsHr(skill);
			String ttl = xpTrackerService.getTimeTilGoal(skill);

			skillData.put("xp_hr", xpHr);
			skillData.put("actions_hr", actionsHr);
			skillData.put("time_to_level", ttl != null ? ttl : "");

			Integer startXp = skillStartXp.get(skill);
			int xpGained = (startXp != null) ? currentXp - startXp : 0;
			skillData.put("xp_gained", xpGained);

			float progressPercent = 100f;
			if (currentLevel < 99)
			{
				int currentLevelMinXp = Experience.getXpForLevel(currentLevel);
				int nextLevelXp = Experience.getXpForLevel(currentLevel + 1);
				int range = nextLevelXp - currentLevelMinXp;
				if (range > 0)
				{
					progressPercent = (float)(currentXp - currentLevelMinXp) / range * 100f;
				}
			}
			skillData.put("progress_percent", Math.round(progressPercent * 10f) / 10f);

			Long lastGain = skillLastGainTime.get(skill);
			skillData.put("last_gain", lastGain != null ? lastGain : 0);

			skillsMap.put(skill.getName(), skillData);
		}

		payload.put("skills", skillsMap);
		cachedJson = gson.toJson(payload);
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

	@Provides
	ESP32TrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ESP32TrackerConfig.class);
	}
}