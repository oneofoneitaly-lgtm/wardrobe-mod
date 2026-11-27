package it.adryh.aura.hud;

import it.adryh.aura.core.Colors;
import it.adryh.aura.core.Draw;
import it.adryh.aura.core.Text;
import it.adryh.aura.core.Theme;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Glass hotbar with health, hunger, armour, air and experience bars. */
final class Hotbar {
    private static final int SLOT = 20;
    private static final int GAP = 2;
    private static final int WIDTH = 9 * SLOT + 8 * GAP + 8;
    private static final ResourceLocation HEART = ResourceLocation.withDefaultNamespace("hud/heart/full");
    private static final ResourceLocation FOOD = ResourceLocation.withDefaultNamespace("hud/food_full");
    private static final ResourceLocation ARMOR = ResourceLocation.withDefaultNamespace("hud/armor_full");
    private static final ResourceLocation AIR = ResourceLocation.withDefaultNamespace("hud/air");

    private static float selector = -1;
    private static float health = -1;
    private static float ghost = -1;
    private static long last = System.currentTimeMillis();

    private Hotbar() {
    }

    /** The vanilla bar stays for spectators and while riding an animal. */
    static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return HudSettings.BAR.on() && mc.player != null && mc.gameMode != null
                && mc.gameMode.getPlayerMode() != GameType.SPECTATOR
                && !(mc.player.getVehicle() instanceof LivingEntity);
    }

    static void render(GuiGraphics g, DeltaTracker delta) {
        if (!active()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        long now = System.currentTimeMillis();
        float dt = Math.min(0.1F, (now - last) / 1000F);
        last = now;

        float left = (g.guiWidth() - WIDTH) / 2F;
        float top = g.guiHeight() - 31;
        int accent = HudSettings.BAR_COLOR.get();

        Draw.shadow(g, left, top, WIDTH, 28, Theme.radius(), 8F, 0x60000000);
        Draw.rect(g, left, top, WIDTH, 28, Theme.radius(), 0xB00B0D18);
        Draw.outlineH(g, left, top, WIDTH, 28, Theme.radius(), 0.6F, Colors.alpha(Theme.accent(), 0.55F), Colors.alpha(Theme.second(), 0.3F));

        int selected = p.getInventory().selected;
        float target = left + 4 + selected * (SLOT + GAP);
        selector = selector < 0 || Math.abs(selector - target) > WIDTH ? target : Theme.approach(selector, target, dt * 1.6F);
        float r = Math.min(5, Theme.radius());
        for (int i = 0; i < 9; i++) {
            Draw.rect(g, left + 4 + i * (SLOT + GAP), top + 4, SLOT, SLOT, r, 0x24FFFFFF);
        }
        Draw.shadow(g, selector, top + 4, SLOT, SLOT, r, 7F, Colors.alpha(accent, 0.75F));
        Draw.gradientV(g, selector, top + 4, SLOT, SLOT, r, Colors.alpha(accent, 0.35F), Colors.alpha(accent, 0.75F));
        Draw.outline(g, selector - 0.5F, top + 3.5F, SLOT + 1, SLOT + 1, r + 0.5F, 1F, Colors.lerp(accent, 0xFFFFFFFF, 0.45F));

        for (int i = 0; i < 9; i++) {
            item(g, p, p.getInventory().getItem(i), (int) left + 6 + i * (SLOT + GAP), (int) top + 6, i);
        }
        ItemStack off = p.getOffhandItem();
        if (!off.isEmpty()) {
            Draw.rect(g, left - 32, top + 2, 26, 24, Theme.radius(), 0xB00B0D18);
            Draw.outline(g, left - 32, top + 2, 26, 24, Theme.radius(), 0.6F, Colors.alpha(Theme.accent(), 0.5F));
            item(g, p, off, (int) left - 27, (int) top + 6, 40);
        }

        if (mc.gameMode.canHurtPlayer()) {
            status(g, p, left, top, dt);
        }
        // name of the item just selected, kept above the bars
        mc.gui.renderSelectedItemName(g, 70);
    }

    private static void item(GuiGraphics g, LocalPlayer p, ItemStack stack, int x, int y, int seed) {
        if (!stack.isEmpty()) {
            g.renderItem(p, stack, x, y, seed);
            g.renderItemDecorations(Minecraft.getInstance().font, stack, x, y);
        }
    }

    private static void status(GuiGraphics g, LocalPlayer p, float left, float top, float dt) {
        float barWidth = (WIDTH - 28) / 2F;
        float right = left + WIDTH - barWidth;
        float y = top - 15;

        float max = Math.max(1F, p.getMaxHealth());
        float now = Math.max(0F, p.getHealth());
        health = health < 0 ? now : Theme.approach(health, now, dt);
        ghost = ghost < health ? health : Math.max(health, ghost - max * dt * 0.35F);
        int from = 0xFFE11D48, to = 0xFFFB7185;
        if (p.hasEffect(MobEffects.WITHER)) {
            from = 0xFF18181B;
            to = 0xFF52525B;
        } else if (p.hasEffect(MobEffects.POISON)) {
            from = 0xFF4D7C0F;
            to = 0xFFA3E635;
        } else if (p.level().getLevelData().isHardcore()) {
            from = 0xFF7F1D1D;
            to = 0xFFDC2626;
        }
        boolean low = now <= max * 0.25F;
        if (low) {
            float pulse = 0.35F + 0.35F * (float) Math.sin(System.currentTimeMillis() / 110.0);
            Draw.shadow(g, left, y, barWidth, 11, 5.5F, 7F, Colors.alpha(0xFF1744, pulse));
        }
        track(g, left, y, barWidth, 11);
        float ghostWidth = barWidth * Math.min(1F, ghost / max);
        if (ghostWidth > 1) {
            Draw.rect(g, left, y, Math.max(8, ghostWidth), 11, 5.5F, 0x90FFFFFF);
        }
        float k = Math.min(1F, health / max);
        if (k > 0.001F) {
            Draw.gradientH(g, left, y, Math.max(8, barWidth * k), 11, 5.5F, from, to);
        }
        float absorption = p.getAbsorptionAmount();
        if (absorption > 0) {
            Draw.outline(g, left - 0.8F, y - 0.8F, barWidth + 1.6F, 12.6F, 6.3F, 1.1F, 0xFFFBBF24);
        }
        g.blitSprite(HEART, (int) left + 2, (int) y + 1, 9, 9);
        if (HudSettings.NUMBERS.on()) {
            String text = (int) Math.ceil(now) + (absorption > 0 ? " +" + (int) Math.ceil(absorption) : "") + " / " + (int) max;
            Text.centered(g, text, left + barWidth / 2F, y + 2.2F, 0xFFFFFFFF, true, 0.75F);
        }

        int food = p.getFoodData().getFoodLevel();
        track(g, right, y, barWidth, 11);
        if (food > 0) {
            float w = Math.max(8, barWidth * Math.min(1F, food / 20F));
            boolean hungry = p.hasEffect(MobEffects.HUNGER);
            Draw.gradientH(g, right + barWidth - w, y, w, 11, 5.5F, hungry ? 0xFF84A33C : 0xFFFBBF24, hungry ? 0xFF4D6B1F : 0xFFD97706);
        }
        g.blitSprite(FOOD, (int) (right + barWidth) - 11, (int) y + 1, 9, 9);
        if (HudSettings.NUMBERS.on()) {
            Text.centered(g, food + " / 20", right + barWidth / 2F, y + 2.2F, 0xFFFFFFFF, true, 0.75F);
        }

        int armor = p.getArmorValue();
        if (armor > 0) {
            thin(g, left, y - 10, barWidth, Math.min(1F, armor / 20F), 0xFF94A3B8, 0xFFE2E8F0, false);
            g.blitSprite(ARMOR, (int) left, (int) y - 13, 9, 9);
        }
        int airMax = p.getMaxAirSupply();
        if (p.isEyeInFluid(FluidTags.WATER) || p.getAirSupply() < airMax) {
            float air = Math.max(0, p.getAirSupply()) / (float) airMax;
            thin(g, right, y - 10, barWidth, air, 0xFF38BDF8, 0xFF7DD3FC, true);
            g.blitSprite(AIR, (int) (right + barWidth) - 9, (int) y - 13, 9, 9);
        }

        if (Minecraft.getInstance().gameMode.hasExperience()) {
            // thin experience line on the bar and the level between health and hunger
            float xw = WIDTH - 12;
            Draw.rect(g, left + 6, top - 2.6F, xw, 2F, 1F, 0x90000000);
            if (p.experienceProgress > 0.005F) {
                Draw.gradientH(g, left + 6, top - 2.6F, Math.max(2, xw * p.experienceProgress), 2F, 1F, Theme.accent(), Theme.second());
            }
            if (p.experienceLevel > 0) {
                String level = Integer.toString(p.experienceLevel);
                Text.centered(g, level, left + WIDTH / 2F, y + 1.5F, Colors.lerp(Theme.second(), 0xFFFFFFFF, 0.4F), true, 1F);
            }
        }
    }

    private static void track(GuiGraphics g, float x, float y, float w, float h) {
        Draw.rect(g, x, y, w, h, h / 2F, 0xB40B0D18);
        Draw.outline(g, x, y, w, h, h / 2F, 0.6F, 0x30FFFFFF);
    }

    private static void thin(GuiGraphics g, float x, float y, float w, float k, int from, int to, boolean reversed) {
        float bx = x + (reversed ? 0 : 12);
        float bw = w - 12;
        Draw.rect(g, bx, y, bw, 3.5F, 1.75F, 0x90000000);
        if (k > 0.005F) {
            float fw = Math.max(3.5F, bw * k);
            Draw.gradientH(g, reversed ? bx + bw - fw : bx, y, fw, 3.5F, 1.75F, from, to);
        }
    }
}
