package ca.thedestruc7i0n.itemdumper;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;


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
                var client = MinecraftClient.getInstance();
                return new CreativeInventoryScreen(
                        client.player,
                        client.player.networkHandler.getEnabledFeatures(),
                        false
                );
            });
            context.waitForScreen(CreativeInventoryScreen.class);

            context.runOnClient(client -> {
                try {
                    ItemDumper.dump();
                } catch (Exception e) {
                    throw new RuntimeException("dump() failed", e);
                }
            });

        }
    }
}
