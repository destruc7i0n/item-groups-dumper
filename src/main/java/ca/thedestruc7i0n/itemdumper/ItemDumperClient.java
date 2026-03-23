package ca.thedestruc7i0n.itemdumper;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.DataConfiguration;
import net.minecraft.server.integrated.IntegratedServerLoader;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.world.rule.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public class ItemDumperClient implements ClientModInitializer {
	private static final Logger LOGGER = LoggerFactory.getLogger(ItemDumperClient.class);
	private static final String LEVEL_ID = "item-dump";

	private enum State { WAITING, SCREEN_OPENED, DONE }
	private State state = State.WAITING;
	private boolean worldLoadStarted = false;

	@Override
	public void onInitializeClient() {
		if (System.getProperty("fabric.client.gametest") != null) return;
		ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
	}

	private void onClientTick(MinecraftClient client) {
		if (state == State.DONE) return;

		if (!worldLoadStarted && client.currentScreen instanceof TitleScreen) {
			worldLoadStarted = true;
			loadOrCreateWorld(client);
		}

		if (client.player == null) return;

		if (state == State.WAITING) {
			client.setScreen(new CreativeInventoryScreen(client.player,
				client.player.networkHandler.getEnabledFeatures(), false));
			state = State.SCREEN_OPENED;
			return;
		}

		// SCREEN_OPENED: CreativeInventoryScreen.init() has run, all display stacks are populated
		client.setScreen(null);
		try {
			dump();
		} catch (Exception e) {
			LOGGER.error("Failed to dump items", e);
		}
		state = State.DONE;
		client.scheduleStop();
	}

	private void loadOrCreateWorld(MinecraftClient client) {
		var loader = client.createIntegratedServerLoader();
		if (client.getLevelStorage().levelExists(LEVEL_ID)) {
			loader.start(LEVEL_ID, () -> {});
		} else {
			LOGGER.info("World '{}' not found, creating it", LEVEL_ID);
			var levelInfo = new LevelInfo(LEVEL_ID, GameMode.CREATIVE, false,
				Difficulty.PEACEFUL, true, new GameRules(FeatureFlags.DEFAULT_ENABLED_FEATURES), DataConfiguration.SAFE_MODE);
			loader.createAndStart(LEVEL_ID, levelInfo, GeneratorOptions.createRandom(),
				lookup -> {
					var entries = lookup.getOrThrow(RegistryKeys.DIMENSION).streamEntries()
						.collect(Collectors.toMap(e -> e.registryKey(), e -> e.value()));
					return new DimensionOptionsRegistryHolder(entries);
				}, null);
		}
	}

	public static void dump() throws Exception {
		var allItemIds = new LinkedHashSet<String>();
		var groupedObject = new JsonObject();

		for (ItemGroup group : ItemGroups.getGroups()) {
			if (group.getType() != ItemGroup.Type.CATEGORY) continue;
			var groupItemIds = new LinkedHashSet<String>();
			for (var stack : group.getDisplayStacks()) {
				var id = Registries.ITEM.getId(stack.getItem()).toString();
				groupItemIds.add(id);
				allItemIds.add(id);
			}
			var groupArray = new JsonArray();
			groupItemIds.forEach(groupArray::add);
			groupedObject.add(group.getDisplayName().getString(), groupArray);
		}

		var flatArray = new JsonArray();
		allItemIds.forEach(flatArray::add);

		var gson = new GsonBuilder().setPrettyPrinting().create();
		var outputDir = FabricLoader.getInstance().getGameDir()
			.toAbsolutePath().getParent().resolve("generated");
		Files.createDirectories(outputDir);

		try (var writer = Files.newBufferedWriter(outputDir.resolve("items.json"))) {
			gson.toJson(flatArray, writer);
		}
		try (var writer = Files.newBufferedWriter(outputDir.resolve("items_grouped.json"))) {
			gson.toJson(groupedObject, writer);
		}

		LOGGER.info("Dumped {} items across {} groups", allItemIds.size(), groupedObject.size());
	}
}
