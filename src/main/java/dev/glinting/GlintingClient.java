package dev.glinting;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlintingClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Glinting");

    @Override
    public void onInitializeClient() {
        GlintConfig.load();

        KeyMapping.Category category =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath("glinting", "main"));
        KeyMapping openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.glinting.open", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            GlintTextures.refresh();
            while (openKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new GlintScreen(null));
                }
            }
        });
    }
}
