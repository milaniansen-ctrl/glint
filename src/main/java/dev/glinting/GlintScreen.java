package dev.glinting;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.DoubleConsumer;

/** Custom Glint settings: Glint / Friends / Experience tabs. */
public class GlintScreen extends Screen {
    private static final int W = 240;

    private static final int TAB_GLINT = 0, TAB_FRIENDS = 1, TAB_EXPERIENCE = 2;

    /** Two columns, read left to right, top to bottom. */
    private static final String[][] FRIENDS = {
            {"nobeltrobel", "txn_pawsz"},
            {"whisperz__", "ITz_ERR0RR"},
            {"marc1i", "larperchan"},
            {"asian_ewan", "sarionteralt"},
            {"_ASET", "hvxo"}
    };

    private static final String DISCORD = "https://discord.gg/t2BQx4AUfV";

    private static int currentTab = TAB_GLINT; // remembered while the game is open

    private final Screen parent;
    private final ItemStack[] previews;

    private double hue, sat, val;
    private boolean updating;

    private EditBox hexBox;
    private Slider hueSlider;
    private Button toggleButton;
    private int left, top;

    public GlintScreen(Screen parent) {
        super(Component.translatable("glinting.screen.title"));
        this.parent = parent;
        this.previews = new ItemStack[] {
                new ItemStack(Items.DIAMOND_SWORD),
                new ItemStack(Items.DIAMOND_CHESTPLATE),
                new ItemStack(Items.DIAMOND_PICKAXE),
                new ItemStack(Items.BOOK)
        };
        for (ItemStack stack : previews) {
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, Boolean.TRUE);
        }
    }

    @Override
    protected void init() {
        hexBox = null;
        hueSlider = null;
        toggleButton = null;

        left = this.width / 2 - W / 2;
        top = Math.max(8, (this.height - 206) / 2);

        // Tab bar
        String[] tabKeys = {"glinting.tab.glint", "glinting.tab.friends", "glinting.tab.experience"};
        int tabW = W / 3;
        for (int i = 0; i < 3; i++) {
            final int index = i;
            Button b = Button.builder(Component.translatable(tabKeys[i]), btn -> {
                currentTab = index;
                this.rebuildWidgets();
            }).bounds(left + i * tabW, top, tabW - 2, 20).build();
            b.active = currentTab != i;
            addRenderableWidget(b);
        }

        int doneY;
        if (currentTab == TAB_FRIENDS) {
            initFriends();
            doneY = top + 184;
        } else if (currentTab == TAB_EXPERIENCE) {
            initExperience();
            doneY = top + 154;
        } else {
            initGlint();
            doneY = top + 154;
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(left, doneY, W, 20).build());
    }

    // ---------------------------------------------------------------- Glint tab

    private void initGlint() {
        GlintConfig cfg = GlintConfig.get();
        double[] hsv = Colors.rgbToHsv(cfg.rgb);
        hue = hsv[0];
        sat = hsv[1];
        val = hsv[2];

        // Hex code box
        hexBox = new EditBox(this.font, left + 40, top + 54, W - 40, 20, Component.translatable("glinting.hex"));
        hexBox.setMaxLength(7);
        hexBox.setFilter(text -> text.matches("#?[0-9a-fA-F]{0,6}"));
        hexBox.setValue(Colors.toHex(cfg.rgb));
        hexBox.setResponder(this::onHexChanged);
        addRenderableWidget(hexBox);

        // Colour bar (hue)
        hueSlider = addRenderableWidget(new Slider(left, top + 80, W, 20,
                Component.translatable("glinting.hue"), 360, "", hue, v -> { hue = v; onHueChanged(); }));

        // Glint strength
        addRenderableWidget(new Slider(left, top + 104, W, 20,
                Component.translatable("glinting.strength"), 100, "%", cfg.strength / 100.0, v -> {
            cfg.strength = (int) Math.round(v * 100);
            cfg.enabled = true;
            refresh();
        }));

        toggleButton = addRenderableWidget(Button.builder(toggleLabel(), b -> {
            cfg.enabled = !cfg.enabled;
            refresh();
        }).bounds(left, top + 128, W / 2 - 2, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("glinting.reset"), b -> {
            cfg.enabled = false;
            cfg.strength = 100;
            this.rebuildWidgets();
            setColor(GlintConfig.DEFAULT_RGB);
        }).bounds(left + W / 2 + 2, top + 128, W / 2 - 2, 20).build());
    }

    private Component toggleLabel() {
        return Component.translatable(GlintConfig.get().enabled ? "glinting.enabled.on" : "glinting.enabled.off");
    }

    private void setColor(int rgb) {
        GlintConfig cfg = GlintConfig.get();
        cfg.rgb = rgb & 0xFFFFFF;
        double[] hsv = Colors.rgbToHsv(cfg.rgb);
        hue = hsv[0];
        sat = hsv[1];
        val = hsv[2];
        if (hexBox != null && hueSlider != null) {
            updating = true;
            hexBox.setValue(Colors.toHex(cfg.rgb));
            hueSlider.set(hue);
            updating = false;
        }
        refresh();
    }

    private void onHexChanged(String text) {
        if (updating) return;
        int rgb = Colors.parseHex(text);
        if (rgb < 0) return; // still typing
        GlintConfig cfg = GlintConfig.get();
        cfg.rgb = rgb;
        cfg.enabled = true;
        double[] hsv = Colors.rgbToHsv(rgb);
        hue = hsv[0];
        sat = hsv[1];
        val = hsv[2];
        updating = true;
        hueSlider.set(hue);
        updating = false;
        refresh();
    }

    private void onHueChanged() {
        if (updating) return;
        if (sat < 0.05) sat = 1;
        if (val < 0.05) val = 1;
        GlintConfig cfg = GlintConfig.get();
        cfg.rgb = Colors.hsvToRgb(hue, sat, val);
        cfg.enabled = true;
        updating = true;
        hexBox.setValue(Colors.toHex(cfg.rgb));
        updating = false;
        refresh();
    }

    private void refresh() {
        if (toggleButton != null) toggleButton.setMessage(toggleLabel());
        GlintTextures.refresh();
    }

    // -------------------------------------------------------------- Friends tab

    private void initFriends() {
        int colW = W / 2 - 4;
        for (int row = 0; row < FRIENDS.length; row++) {
            for (int col = 0; col < 2; col++) {
                final String name = FRIENDS[row][col];
                int x = left + col * (W / 2 + 4);
                int y = top + 30 + row * 30 + 11;
                addRenderableWidget(Button.builder(Component.translatable("glinting.namemc"),
                        b -> open("https://namemc.com/profile/" + name))
                        .bounds(x, y, colW, 16).build());
            }
        }
    }

    // ---------------------------------------------------------- Experience tab

    private void initExperience() {
        addRenderableWidget(Button.builder(Component.translatable("glinting.discord"), b -> open(DISCORD))
                .bounds(left, top + 44, W, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("glinting.discord"), b -> open(DISCORD))
                .bounds(left, top + 100, W, 20).build());
    }

    private static void open(String url) {
        Util.getPlatform().openUri(url);
    }

    // ------------------------------------------------------------------- render

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);

        if (currentTab == TAB_FRIENDS) {
            for (int row = 0; row < FRIENDS.length; row++) {
                for (int col = 0; col < 2; col++) {
                    int x = left + col * (W / 2 + 4);
                    g.drawString(this.font, FRIENDS[row][col], x + 2, top + 30 + row * 30, 0xFFFFFFFF);
                }
            }
        } else if (currentTab == TAB_EXPERIENCE) {
            g.drawString(this.font, Component.translatable("glinting.exp.dev"), left + 2, top + 32, 0xFFFFFFFF);
            g.drawString(this.font, Component.translatable("glinting.exp.jrmod"), left + 2, top + 88, 0xFFFFFFFF);
        } else {
            // Current colour swatch + live enchanted item preview
            int rgb = GlintConfig.get().rgb;
            g.fill(left - 1, top + 27, left + 101, top + 45, 0xFF000000);
            g.fill(left, top + 28, left + 100, top + 44, 0xFF000000 | rgb);
            for (int i = 0; i < previews.length; i++) {
                g.renderItem(previews[i], left + 110 + i * 22, top + 28);
            }

            g.drawString(this.font, Component.translatable("glinting.hex"), left, top + 60, 0xFFAAAAAA);

            // Rainbow strip under the colour bar
            for (int i = 0; i < W; i++) {
                g.fill(left + i, top + 100, left + i + 1, top + 103, 0xFF000000 | Colors.hsvToRgb(i / (double) W, 1, 1));
            }
        }
    }

    @Override
    public void onClose() {
        GlintConfig.save();
        this.minecraft.setScreen(parent);
    }

    /** 0..1 slider that shows a scaled number (degrees / percent). */
    private static final class Slider extends AbstractSliderButton {
        private final String label;
        private final String suffix;
        private final double max;
        private final DoubleConsumer onChange;

        Slider(int x, int y, int w, int h, Component label, double max, String suffix, double value, DoubleConsumer onChange) {
            super(x, y, w, h, Component.empty(), value);
            this.label = label.getString();
            this.suffix = suffix;
            this.max = max;
            this.onChange = onChange;
            updateMessage();
        }

        /** Move the handle without firing the change callback. */
        void set(double v) {
            this.value = Math.max(0, Math.min(1, v));
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (label == null) return; // called from the super constructor before fields exist
            setMessage(Component.literal(label + ": " + Math.round(value * max) + suffix));
        }

        @Override
        protected void applyValue() {
            onChange.accept(value);
        }
    }
}
