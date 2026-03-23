package ca.thedestruc7i0n.itemdumper;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings("UnstableApiUsage")
public class ItemDumperClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .adjustSettings(settings -> settings.setGameMode(WorldCreator.Mode.CREATIVE))
                .create()) {

            singleplayer.getClientWorld().waitForChunksRender();

            // Opening the creative screen calls ItemGroups.updateDisplayContext(),
            // which populates getDisplayStacks() for all item groups.
            context.setScreen(() -> {
                var client = net.minecraft.client.MinecraftClient.getInstance();
                return new CreativeInventoryScreen(
                        client.player,
                        client.player.networkHandler.getEnabledFeatures(),
                        false
                );
            });
            context.waitForScreen(CreativeInventoryScreen.class);

            Path outputDir = FabricLoader.getInstance().getGameDir()
                    .toAbsolutePath().getParent().resolve("generated");
            deleteIfExists(outputDir.resolve("items.json"));
            deleteIfExists(outputDir.resolve("items_grouped.json"));

            context.runOnClient(client -> {
                try {
                    ItemDumperClient.dump();
                } catch (Exception e) {
                    throw new RuntimeException("dump() failed", e);
                }
            });

            assertFileContainsItemIds(outputDir.resolve("items.json"));
            assertFileContainsItemIds(outputDir.resolve("items_grouped.json"));
        }
    }

    private static void deleteIfExists(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new AssertionError("Failed to delete " + file.getFileName(), e);
        }
    }

    private static void assertFileContainsItemIds(Path file) {
        if (!Files.exists(file)) {
            throw new AssertionError(file.getFileName() + " was not created");
        }
        try {
            String content = Files.readString(file);
            if (!content.contains("minecraft:")) {
                throw new AssertionError(file.getFileName() + " should contain namespaced item IDs");
            }
        } catch (IOException e) {
            throw new AssertionError("Failed to read " + file.getFileName(), e);
        }
    }
}
