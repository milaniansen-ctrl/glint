# Custom Glint

A client-side Fabric mod for **Minecraft 1.21.11** that recolors the enchantment glint.

- Press **G** (rebindable under *Controls > Custom Glint*) to open the settings screen.
- Three tabs: **Glint** (hex box, colour bar, glint strength), **Friends** (NameMC links) and **Experience** (Discord links).
- Type a **hex code** in the box, or drag the **hue bar**.
- A row of enchanted items in the screen previews the glint live as you change it.
- **Reset to Vanilla** puts the original glint back. Settings are saved to `config/glinting.json`.

It affects every glint: held items, inventory/GUI, dropped items and armor.

## How it works

No render mixins. Custom Glint reads the vanilla glint textures from your active resource packs,
keeps each pixel's brightness, swaps the hue for your color, and registers the result over
the original texture. It re-applies itself after F3+T or a resource pack change.

## Requirements

Minecraft 1.21.11, Fabric Loader 0.18+, Fabric API.

## Building

Needs JDK 21.

**Option A - locally**
```
gradle wrapper --gradle-version 9.2.1
./gradlew build          # Windows: gradlew.bat build
```
The jar is in `build/libs/CustomGlint-1.0.0.jar` (not the `-sources` one).
Run `./gradlew runClient` to test in a dev client.

**Option B - no setup, via GitHub**
Push this folder to a GitHub repo. The included workflow (`.github/workflows/build.yml`)
builds it; download the jar from the run's *Artifacts*.

## If something looks off

- **Colors are swapped (red shows as blue):** in `GlintTextures.java`, `NativeImage` pixel
  order differs on your build. Swap the `tr` and `tb` values in `apply()`.
- **Compile error on `Identifier`:** older mappings call it `ResourceLocation`.
  Replace the import `net.minecraft.resources.Identifier` with `...ResourceLocation` and rename uses.

## Credits

**Authors:** ITz_ERR0R, alienmc

**Contributors:** nobeltrobel, sarionteralt, marc1i, asian_ewan
