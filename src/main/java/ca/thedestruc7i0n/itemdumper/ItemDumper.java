package ca.thedestruc7i0n.itemdumper;

import com.google.gson.GsonBuilder;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;

public class ItemDumper {
	private static final Logger LOGGER = LoggerFactory.getLogger(ItemDumper.class);

	public static void dump() throws Exception {
		List<String> items = null;
		var grouped = new LinkedHashMap<String, List<String>>();

		for (var group : ItemGroups.getGroups()) {
			if (group.getType() == ItemGroup.Type.SEARCH) {
				items = group.getDisplayStacks().stream()
						.map(stack -> Registries.ITEM.getId(stack.getItem()).toString())
						.distinct()
						.toList();
			} else if (group.getType() == ItemGroup.Type.CATEGORY) {
				grouped.put(
						group.getDisplayName().getString(),
						group.getDisplayStacks().stream()
								.map(stack -> Registries.ITEM.getId(stack.getItem()).toString())
								.distinct()
								.toList()
				);
			}
		}

		if (items == null || items.isEmpty()) {
			throw new IllegalStateException("Search tab returned no items — display stacks may not be populated");
		}

		var gson = new GsonBuilder().setPrettyPrinting().create();
		var outputDir = Paths.get(System.getProperty("itemdumper.outputDir"));
		Files.createDirectories(outputDir);

		try (var writer = Files.newBufferedWriter(outputDir.resolve("items.json"))) {
			gson.toJson(gson.toJsonTree(items), writer);
		}
		try (var writer = Files.newBufferedWriter(outputDir.resolve("items_grouped.json"))) {
			gson.toJson(gson.toJsonTree(grouped), writer);
		}

		LOGGER.info("Dumped {} items across {} groups", items.size(), grouped.size());
	}
}
