package dev.glinting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GlintConfig {
    /** Roughly vanilla's purple. Used as the default colour in the picker. */
    public static final int DEFAULT_RGB = 0x8040CC;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static GlintConfig instance = new GlintConfig();

    /** When false, the vanilla glint texture is used untouched. */
    public boolean enabled = true;
    /** 0xRRGGBB */
    public int rgb = DEFAULT_RGB;
    /** Glint strength, 0..100 (%). */
    public int strength = 100;

    public static GlintConfig get() {
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("glinting.json");
    }

    public static void load() {
        Path p = path();
        if (!Files.exists(p)) return;
        try (Reader r = Files.newBufferedReader(p)) {
            GlintConfig loaded = GSON.fromJson(r, GlintConfig.class);
            if (loaded != null) {
                loaded.rgb &= 0xFFFFFF;
                loaded.strength = Math.max(0, Math.min(100, loaded.strength));
                instance = loaded;
            }
        } catch (Exception e) {
            GlintingClient.LOGGER.warn("Could not read glinting.json, using defaults", e);
        }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(path())) {
            GSON.toJson(instance, w);
        } catch (Exception e) {
            GlintingClient.LOGGER.warn("Could not save glinting.json", e);
        }
    }
}
