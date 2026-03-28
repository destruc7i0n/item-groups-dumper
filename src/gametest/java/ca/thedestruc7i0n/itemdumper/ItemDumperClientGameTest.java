package ca.thedestruc7i0n.itemdumper;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;


@SuppressWarnings("UnstableApiUsage")
public class ItemDumperClientGameTest implements FabricClientGameTest {

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder()
                .adjustSettings(settings -> settings.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
                .create()) {

            singleplayer.getClientLevel().waitForChunksRender();

            // Opening the creative screen calls CreativeModeTabs.tryRebuildTabContents(),
            // which populates getDisplayItems() for all item groups.
            context.setScreen(() -> {
                var client = Minecraft.getInstance();
                return new CreativeModeInventoryScreen(
                        client.player,
                        client.player.connection.enabledFeatures(),
                        false
                );
            });
            context.waitForScreen(CreativeModeInventoryScreen.class);

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
