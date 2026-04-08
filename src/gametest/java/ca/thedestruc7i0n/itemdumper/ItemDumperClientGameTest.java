package ca.thedestruc7i0n.itemdumper;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.item.CreativeModeTabs;


@SuppressWarnings("UnstableApiUsage")
public class ItemDumperClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .adjustSettings(settings -> settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
                .create()) {

            singleplayer.getClientLevel().waitForChunksRender();

            context.runOnClient(client -> {
                try {
                    CreativeModeTabs.tryRebuildTabContents(
                            client.player.connection.enabledFeatures(), // enabledFeatures: gates tab contents behind feature flags
                            false, // hasPermissions: false excludes operator-only entries from the dump
                            client.player.level().registryAccess() // registryAccess: supplies holder lookups used while populating items
                    );
                    ItemDumper.dump();
                } catch (Exception e) {
                    throw new RuntimeException("dump() failed", e);
                }
            });

        }
    }
}
