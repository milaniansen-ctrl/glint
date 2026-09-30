package dev.glinting;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Recolours the glint by swapping the game's glint textures for tinted copies.
 *
 * The vanilla glint texture is read from the active resource packs, each pixel's
 * brightness is kept, and the hue is replaced with the chosen colour. The result is
 * registered over the original texture id, so every glint (items, armor, GUI) changes
 * with no render mixins. {@link #refresh()} is cheap and runs every tick, which also
 * re-applies the tint after a resource reload (F3+T) or a resource pack change.
 */
public final class GlintTextures {
    private GlintTextures() {}

    private static final class Slot {
        final Identifier id;
        int width, height;
        int[] base;              // original ARGB pixels
        Object baseSource;       // the vanilla texture object the base was read for
        DynamicTexture ours;     // our tinted texture, once registered
        int appliedRgb = -1;
        int appliedStrength = -1;
        boolean appliedEnabled;
        boolean missing;

        Slot(String path) {
            this.id = Identifier.withDefaultNamespace(path);
        }
    }

    // Item/GUI glint, armor/entity glint, and the pre-1.19.3 filename (skipped if absent).
    private static final List<Slot> SLOTS = List.of(
            new Slot("textures/misc/enchanted_glint_item.png"),
            new Slot("textures/misc/enchanted_glint_entity.png"),
            new Slot("textures/misc/enchanted_item_glint.png")
    );

    public static void refresh() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getResourceManager() == null) return;
        TextureManager tm = mc.getTextureManager();
        GlintConfig cfg = GlintConfig.get();

        for (Slot s : SLOTS) {
            if (s.missing) continue;

            AbstractTexture current = tm.getTexture(s.id);
            boolean stillOurs = s.ours != null && current == s.ours;

            if (!stillOurs) {
                // Vanilla texture is (re)loaded: our old one is gone, read the base again.
                s.ours = null;
                s.appliedRgb = -1;
                s.appliedStrength = -1;
                if (s.baseSource != current) {
                    if (!loadBase(mc, s)) {
                        s.missing = true;
                        continue;
                    }
                    s.baseSource = current;
                }
                if (!cfg.enabled) continue; // leave vanilla alone
            } else if (s.appliedRgb == cfg.rgb && s.appliedStrength == cfg.strength && s.appliedEnabled == cfg.enabled) {
                continue; // nothing changed
            }

            apply(tm, s, cfg);
        }
    }

    private static boolean loadBase(Minecraft mc, Slot s) {
        Optional<Resource> res = mc.getResourceManager().getResource(s.id);
        if (res.isEmpty()) return false;
        try (InputStream in = res.get().open(); NativeImage img = NativeImage.read(in)) {
            s.width = img.getWidth();
            s.height = img.getHeight();
            s.base = new int[s.width * s.height];
            for (int y = 0; y < s.height; y++) {
                for (int x = 0; x < s.width; x++) {
                    s.base[y * s.width + x] = img.getPixel(x, y); // ARGB in 1.21.2+
                }
            }
            return true;
        } catch (IOException | RuntimeException e) {
            GlintingClient.LOGGER.warn("Could not read glint texture {}", s.id, e);
            return false;
        }
    }

    private static void apply(TextureManager tm, Slot s, GlintConfig cfg) {
        int tr = (cfg.rgb >> 16) & 255;
        int tg = (cfg.rgb >> 8) & 255;
        int tb = cfg.rgb & 255;

        NativeImage img = new NativeImage(s.width, s.height, false);
        for (int y = 0; y < s.height; y++) {
            for (int x = 0; x < s.width; x++) {
                int p = s.base[y * s.width + x];
                int out;
                if (cfg.enabled) {
                    int a = p >>> 24;
                    int m = Math.max((p >> 16) & 255, Math.max((p >> 8) & 255, p & 255)) * cfg.strength / 100;
                    out = (a << 24) | ((tr * m / 255) << 16) | ((tg * m / 255) << 8) | (tb * m / 255);
                } else {
                    out = p;
                }
                img.setPixel(x, y, out);
            }
        }

        DynamicTexture tex = new DynamicTexture(() -> "glinting " + s.id, img);
        tm.register(s.id, tex); // closes whatever was registered before (vanilla or our previous tint)
        tex.upload();

        s.ours = tex;
        s.appliedRgb = cfg.rgb;
        s.appliedStrength = cfg.strength;
        s.appliedEnabled = cfg.enabled;
    }
}
