package it.adryh.aura.hud;

import it.adryh.aura.core.Colors;
import it.adryh.aura.core.Draw;
import it.adryh.aura.core.Text;
import it.adryh.aura.core.Theme;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

final class Widgets {
    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();
    private static float speed;

    static final List<Widget> ALL = new ArrayList<>();

    static {
        ALL.add(new Line("fps", "FPS", HudSettings.FPS, 0F, 0F) {
            @Override
            String value() {
                return Integer.toString(mc().getFps());
            }
        });
        ALL.add(new Coords());
        ALL.add(new Line("ping", "PING", HudSettings.PING, 0F, 0.25F) {
            @Override
            String value() {
                LocalPlayer p = mc().player;
                PlayerInfo info = p == null ? null : p.connection.getPlayerInfo(p.getUUID());
                return (info == null ? 0 : info.getLatency()) + " ms";
            }
        });
        ALL.add(new Line("speed", "VEL", HudSettings.SPEED, 0F, 0.34F) {
            @Override
            String value() {
                return String.format(Locale.ROOT, "%.1f blocchi/s", speed);
            }
        });
        ALL.add(new Compass());
        ALL.add(new Clock());
        ALL.add(new Potions());
        ALL.add(new Armor());
        ALL.add(new Keys());
    }

    private Widgets() {
    }

    static Minecraft mc() {
        return Minecraft.getInstance();
    }

    static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = mc();
        if (mc.player == null || mc.getDebugOverlay().showDebugScreen() || mc.screen instanceof HudEditorScreen) {
            return;
        }
        for (Widget w : ALL) {
            if (w.enabled.on() && w.hasContent()) {
                w.render(g);
            }
        }
    }

    static void tick() {
        LocalPlayer p = mc().player;
        if (p == null) {
            speed = 0;
            return;
        }
        double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
        speed += ((float) Math.sqrt(dx * dx + dz * dz) * 20F - speed) * 0.35F;
    }

    static void click(int button) {
        if (mc().screen != null) {
            return;
        }
        if (button == 0) {
            LEFT.add(Util.getMillis());
        } else if (button == 1) {
            RIGHT.add(Util.getMillis());
        }
    }

    private static int cps(Deque<Long> clicks) {
        long limit = Util.getMillis() - 1000;
        while (!clicks.isEmpty() && clicks.peekFirst() < limit) {
            clicks.removeFirst();
        }
        return clicks.size();
    }

    static void card(GuiGraphics g, float w, float h) {
        float alpha = HudSettings.OPACITY.f() / 100F;
        float r = Math.min(Theme.radius(), h / 2F);
        Draw.shadow(g, 0, 1, w, h, r, 6F, Colors.alpha(0x000000, 0.35F * alpha));
        Draw.rect(g, 0, 0, w, h, r, Colors.alpha(0x0B0D18, alpha));
        Draw.outlineH(g, 0, 0, w, h, r, 0.6F, Colors.alpha(Theme.accent(), 0.65F * alpha), Colors.alpha(Theme.second(), 0.12F * alpha));
    }

    private static int label() {
        return HudSettings.LABELS.get();
    }

    private abstract static class Line extends Widget {
        private final String title;

        Line(String id, String title, it.adryh.aura.core.Setting.Toggle enabled, float x, float y) {
            super(id, title, enabled, x, y);
            this.title = title;
        }

        abstract String value();

        @Override
        float width() {
            return Math.max(58, Text.width(title, true, 0.85F) + Text.width(value()) + 22);
        }

        @Override
        float height() {
            return 18;
        }

        @Override
        void draw(GuiGraphics g) {
            card(g, width(), height());
            Text.draw(g, title, 7, 5.5F, label(), true, 0.85F);
            Text.draw(g, value(), width() - 7 - Text.width(value()), 5F, Theme.TEXT);
        }
    }

    private static final class Coords extends Widget {
        Coords() {
            super("coords", "Coordinate", HudSettings.COORDS, 0F, 0.09F);
        }

        private String[] lines() {
            LocalPlayer p = mc().player;
            if (p == null) {
                return new String[]{"128", "64", "-256", "Pianura", "Nord"};
            }
            String biome = p.level().getBiome(p.blockPosition()).unwrapKey()
                    .map(k -> Component.translatable(Util.makeDescriptionId("biome", k.location())).getString())
                    .orElse("?");
            String facing = switch (p.getDirection()) {
                case NORTH -> "Nord";
                case SOUTH -> "Sud";
                case EAST -> "Est";
                case WEST -> "Ovest";
                default -> "";
            };
            return new String[]{Integer.toString(p.getBlockX()), Integer.toString(p.getBlockY()),
                    Integer.toString(p.getBlockZ()), biome, facing};
        }

        @Override
        float width() {
            String[] l = lines();
            return Math.max(92, Text.width(l[3] + "  " + l[4], false, 0.85F) + 16);
        }

        @Override
        float height() {
            return 48;
        }

        @Override
        void draw(GuiGraphics g) {
            String[] l = lines();
            float w = width();
            card(g, w, height());
            String[] axes = {"X", "Y", "Z"};
            for (int i = 0; i < 3; i++) {
                Text.draw(g, axes[i], 8, 5 + i * 10, label(), true, 0.9F);
                Text.draw(g, l[i], 20, 5 + i * 10, Theme.TEXT);
            }
            Draw.rect(g, 8, 35.5F, w - 16, 0.6F, 0, 0x30FFFFFF);
            Text.draw(g, l[3] + "  " + l[4], 8, 39, Theme.TEXT_DIM, false, 0.85F);
        }
    }

    private static final class Compass extends Widget {
        private static final String[] NAMES = {"S", "SO", "O", "NO", "N", "NE", "E", "SE"};

        Compass() {
            super("compass", "Bussola", HudSettings.COMPASS, 0.5F, 0F);
        }

        @Override
        float width() {
            return 170;
        }

        @Override
        float height() {
            return 22;
        }

        @Override
        void draw(GuiGraphics g) {
            card(g, width(), height());
            LocalPlayer p = mc().player;
            float partial = mc().getTimer().getGameTimeDeltaPartialTick(true);
            float yaw = p == null ? 180F : p.getViewYRot(partial);
            yaw = ((yaw % 360F) + 360F) % 360F;
            float center = width() / 2F;
            for (int deg = 0; deg < 360; deg += 15) {
                float diff = ((deg - yaw + 540F) % 360F) - 180F;
                float x = center + diff * 1.05F;
                if (x < 8 || x > width() - 8) {
                    continue;
                }
                float fade = Math.max(0.1F, 1F - Math.abs(x - center) / center);
                if (deg % 45 == 0) {
                    String name = NAMES[deg / 45];
                    boolean main = deg % 90 == 0;
                    Text.centered(g, name, x, main ? 5F : 6F, Colors.alpha(name.equals("N") ? label() : 0xFFFFFF, Math.min(1F, fade * 1.5F)),
                            main, main ? 1F : 0.75F);
                } else {
                    Draw.rect(g, x - 0.4F, 8, 0.8F, 5, 0.4F, Colors.alpha(0xFFFFFF, fade * 0.7F));
                }
            }
            Draw.rect(g, center - 5, 18, 10, 1.6F, 0.8F, label());
        }
    }

    private static final class Clock extends Widget {
        Clock() {
            super("clock", "Orologio", HudSettings.CLOCK, 1F, 0.66F);
        }

        @Override
        float width() {
            return 76;
        }

        @Override
        float height() {
            return 30;
        }

        @Override
        void draw(GuiGraphics g) {
            card(g, width(), height());
            long time = mc().level == null ? 6000 : mc().level.getDayTime();
            long inDay = Math.floorMod(time, 24000L);
            long hours = (inDay / 1000 + 6) % 24;
            long minutes = (inDay % 1000) * 60 / 1000;
            boolean night = inDay >= 13000 && inDay < 23000;
            // sun or crescent moon
            Draw.circle(g, 13, 15, 6, night ? 0xFFD7DCFF : 0xFFFFC845);
            if (night) {
                Draw.circle(g, 16, 12.5F, 5, 0xFF141730);
            }
            Text.draw(g, String.format(Locale.ROOT, "%02d:%02d", hours, minutes), 26, 5, Theme.TEXT, true, 1.15F);
            Text.draw(g, "Giorno " + (Math.floorDiv(time, 24000L) + 1), 26, 18, Theme.TEXT_DIM, false, 0.8F);
        }
    }

    private static final class Potions extends Widget {
        Potions() {
            super("potions", "Effetti", HudSettings.POTIONS, 1F, 0F);
        }

        private List<MobEffectInstance> effects() {
            LocalPlayer p = mc().player;
            return p == null ? List.of() : p.getActiveEffects().stream().filter(MobEffectInstance::showIcon).toList();
        }

        @Override
        boolean hasContent() {
            return !effects().isEmpty();
        }

        @Override
        float width() {
            return 112;
        }

        @Override
        float height() {
            return Math.max(1, effects().size()) * 24 + 4;
        }

        @Override
        void draw(GuiGraphics g) {
            card(g, width(), height());
            List<MobEffectInstance> list = effects();
            if (list.isEmpty()) {
                Text.draw(g, "Effetti attivi", 8, 10, Theme.TEXT_DIM);
                return;
            }
            float y = 4;
            for (MobEffectInstance e : list) {
                g.blit(5, (int) y + 1, 0, 18, 18, mc().getMobEffectTextures().get(e.getEffect()));
                String name = e.getEffect().value().getDisplayName().getString();
                if (e.getAmplifier() > 0) {
                    name += " " + (e.getAmplifier() + 1);
                }
                Text.draw(g, Text.fit(name, width() - 34, false, 0.9F), 28, y + 2, Theme.TEXT, false, 0.9F);
                int seconds = e.getDuration() / 20;
                String time = e.isInfiniteDuration() ? "--:--" : String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
                boolean ending = !e.isInfiniteDuration() && seconds < 10;
                Text.draw(g, time, 28, y + 12, ending ? 0xFFFF6B6B : label(), true, 0.85F);
                y += 24;
            }
        }
    }

    private static final class Armor extends Widget {
        Armor() {
            super("armor", "Armatura", HudSettings.ARMOR, 1F, 0.92F);
        }

        private List<ItemStack> items() {
            LocalPlayer p = mc().player;
            List<ItemStack> list = new ArrayList<>();
            if (p != null) {
                for (int i = 3; i >= 0; i--) {
                    list.add(p.getInventory().armor.get(i));
                }
                list.add(p.getMainHandItem());
                list.removeIf(ItemStack::isEmpty);
            }
            if (list.isEmpty() && mc().screen instanceof HudEditorScreen) {
                list.add(new ItemStack(Items.DIAMOND_HELMET));
                list.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
            }
            return list;
        }

        @Override
        boolean hasContent() {
            return !items().isEmpty();
        }

        @Override
        float width() {
            return 66;
        }

        @Override
        float height() {
            return Math.max(1, items().size()) * 19 + 4;
        }

        @Override
        void draw(GuiGraphics g) {
            card(g, width(), height());
            int y = 3;
            for (ItemStack stack : items()) {
                g.renderItem(stack, 4, y);
                if (stack.isDamageableItem()) {
                    float k = 1F - stack.getDamageValue() / (float) stack.getMaxDamage();
                    int color = Colors.hsv(k * 0.33F, 0.75F, 1F);
                    Text.draw(g, Integer.toString(stack.getMaxDamage() - stack.getDamageValue()), 25, y + 2, Theme.TEXT, false, 0.85F);
                    Draw.rect(g, 25, y + 12, 34, 2.2F, 1.1F, 0x40FFFFFF);
                    Draw.rect(g, 25, y + 12, Math.max(2, 34 * k), 2.2F, 1.1F, color);
                } else {
                    Text.draw(g, "x" + stack.getCount(), 25, y + 4, Theme.TEXT);
                }
                y += 19;
            }
        }
    }

    private static final class Keys extends Widget {
        Keys() {
            super("keys", "Tasti", HudSettings.KEYS, 0F, 0.72F);
        }

        @Override
        float width() {
            return 62;
        }

        @Override
        float height() {
            return 74;
        }

        @Override
        void draw(GuiGraphics g) {
            var o = mc().options;
            key(g, 21, 0, 20, 20, o.keyUp, "W");
            key(g, 0, 21, 20, 20, o.keyLeft, "A");
            key(g, 21, 21, 20, 20, o.keyDown, "S");
            key(g, 42, 21, 20, 20, o.keyRight, "D");
            button(g, 0, 42, 30.5F, 19, o.keyAttack.isDown(), "LMB", cps(LEFT));
            button(g, 31.5F, 42, 30.5F, 19, o.keyUse.isDown(), "RMB", cps(RIGHT));
            tile(g, 0, 62, 62, 12, o.keyJump.isDown());
            Draw.rect(g, 18, 67.4F, 26, 1.4F, 0.7F, 0xFFFFFFFF);
        }

        private void tile(GuiGraphics g, float x, float y, float w, float h, boolean down) {
            float alpha = HudSettings.OPACITY.f() / 100F;
            float r = Math.min(4, Theme.radius());
            if (down) {
                Draw.shadow(g, x, y, w, h, r, 5F, Colors.alpha(label(), 0.6F));
                Draw.gradientV(g, x, y, w, h, r, label(), Colors.lerp(label(), 0xFF000000, 0.25F));
            } else {
                Draw.rect(g, x, y, w, h, r, Colors.alpha(0x0B0D18, Math.max(0.25F, alpha)));
                Draw.outline(g, x, y, w, h, r, 0.6F, 0x38FFFFFF);
            }
        }

        private void key(GuiGraphics g, float x, float y, float w, float h, KeyMapping key, String name) {
            tile(g, x, y, w, h, key.isDown());
            Text.centered(g, name, x + w / 2F, y + 5.5F, 0xFFFFFFFF, true, 1F);
        }

        private void button(GuiGraphics g, float x, float y, float w, float h, boolean down, String name, int cps) {
            tile(g, x, y, w, h, down);
            Text.centered(g, name, x + w / 2F, y + 3F, 0xFFFFFFFF, true, 0.75F);
            Text.centered(g, cps + " CPS", x + w / 2F, y + 11F, 0xFFD8DAEC, false, 0.65F);
        }
    }
}
