package it.adryh.mythoria.core.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.adryh.mythoria.core.Mythoria;
import it.adryh.mythoria.core.market.MarketCatalog;
import it.adryh.mythoria.core.market.MarketCategory;
import it.adryh.mythoria.core.market.MarketEntry;
import it.adryh.mythoria.core.net.Net;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The fantasy market: categories, goods on the left, a spinning 3d preview on the right. */
public final class MarketScreen extends Screen {
    private static final int PANEL_W = 408;
    private static final int PANEL_H = 232;
    private static final int ROW_H = 30;
    private static final int ROWS = 5;
    private static final int LIST_W = 214;

    private static final int GOLD = 0xFFF0C24B;
    private static final int GOLD_DARK = 0xFF8A5E12;
    private static final int TEXT = 0xFFF4ECFF;
    private static final int MUTED = 0xFFB4A6D4;

    private static MarketCategory lastCategory = MarketCategory.CREATURES;

    private final Map<ResourceLocation, LivingEntity> previews = new HashMap<>();
    private long balance;
    private MarketCategory category = lastCategory;
    private MarketEntry selected;
    private int scroll;
    private Component message = Component.empty();
    private int messageColor = TEXT;
    private int messageTicks;
    private int ticks;
    private int left;
    private int top;

    public MarketScreen(long balance) {
        super(Component.translatable("mythoria.market.title"));
        this.balance = balance;
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2;
        if (selected == null) {
            select(category);
        }
    }

    private void select(MarketCategory next) {
        category = next;
        lastCategory = next;
        scroll = 0;
        List<MarketEntry> list = MarketCatalog.in(next);
        selected = list.isEmpty() ? null : list.get(0);
    }

    void update(Net.State state) {
        balance = state.balance();
        if (state.result() == 0) {
            return;
        }
        MarketEntry entry = MarketCatalog.get(ResourceLocation.parse(state.entry()));
        Component name = entry == null ? Component.empty() : entry.name();
        boolean bought = state.result() == 1;
        message = Component.translatable(bought ? "mythoria.market.bought" : "mythoria.market.poor", name);
        messageColor = bought ? 0xFF7CF29A : 0xFFFF7A7A;
        messageTicks = 70;
    }

    @Override
    public void tick() {
        ticks++;
        if (messageTicks > 0) {
            messageTicks--;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ layout helpers
    private int tabX(int index) {
        return left + 10 + index * 98;
    }

    private int listY() {
        return top + 56;
    }

    private int previewX() {
        return left + 10 + LIST_W + 8;
    }

    private int previewW() {
        return PANEL_W - LIST_W - 28;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    // ------------------------------------------------------------------ drawing
    private static void frame(GuiGraphics g, int x, int y, int w, int h, int fillTop, int fillBottom, int border) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF07040D);
        g.fill(x, y, x + w, y + h, border);
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, fillTop, fillBottom);
    }

    private static void gem(GuiGraphics g, int cx, int cy, int color) {
        for (int i = -3; i <= 3; i++) {
            int half = 4 - Math.abs(i);
            g.fill(cx - half, cy + i, cx + half, cy + i + 1, 0xFF07040D);
        }
        for (int i = -2; i <= 2; i++) {
            int half = 3 - Math.abs(i);
            g.fill(cx - half, cy + i, cx + half, cy + i + 1, color);
        }
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partial) {
        g.fillGradient(0, 0, width, height, 0xB0120A22, 0xD0050308);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        frame(g, left, top, PANEL_W, PANEL_H, 0xFF2A1D45, 0xFF150E27, GOLD_DARK);
        g.renderOutline(left + 3, top + 3, PANEL_W - 6, PANEL_H - 6, 0x66F0C24B);
        gem(g, left, top, 0xFF5BE8F0);
        gem(g, left + PANEL_W, top, 0xFF5BE8F0);
        gem(g, left, top + PANEL_H, 0xFF5BE8F0);
        gem(g, left + PANEL_W, top + PANEL_H, 0xFF5BE8F0);

        // title and wallet
        g.pose().pushPose();
        g.pose().translate(left + 12, top + 10, 0);
        g.pose().scale(1.5F, 1.5F, 1);
        g.drawString(font, title, 0, 0, GOLD, true);
        g.pose().popPose();
        String money = String.format("%,d", balance);
        int boxW = font.width(money) + 32;
        int boxX = left + PANEL_W - 10 - boxW;
        frame(g, boxX, top + 7, boxW, 20, 0xFF3B2A10, 0xFF1F1608, GOLD);
        g.renderItem(new ItemStack(Mythoria.CROWN.get()), boxX + 4, top + 9);
        g.drawString(font, money, boxX + 24, top + 13, 0xFFFFE9A0, true);

        MarketCategory[] categories = MarketCategory.values();
        for (int i = 0; i < categories.length; i++) {
            boolean active = categories[i] == category;
            boolean hover = inside(mouseX, mouseY, tabX(i), top + 32, 94, 17);
            frame(g, tabX(i), top + 32, 94, 17,
                    active ? 0xFF7A4DC0 : hover ? 0xFF47346E : 0xFF2E2249,
                    active ? 0xFF4A2A82 : hover ? 0xFF33264F : 0xFF1F1733,
                    active ? GOLD : 0xFF5C4A80);
            g.drawCenteredString(font, Component.translatable(categories[i].key()), tabX(i) + 47, top + 37, active ? 0xFFFFE9A0 : TEXT);
        }

        // goods
        List<MarketEntry> list = MarketCatalog.in(category);
        for (int row = 0; row < ROWS && row + scroll < list.size(); row++) {
            MarketEntry entry = list.get(row + scroll);
            int x = left + 10;
            int y = listY() + row * ROW_H;
            boolean active = entry.equals(selected);
            boolean hover = inside(mouseX, mouseY, x, y, LIST_W, ROW_H - 3);
            frame(g, x, y, LIST_W, ROW_H - 3,
                    active ? 0xFF55397F : hover ? 0xFF3B2B5C : 0xFF261B40,
                    active ? 0xFF35215A : 0xFF1B1330,
                    active ? GOLD : 0xFF4A3A6A);
            List<ItemStack> stacks = entry.stacks();
            if (!stacks.isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(x + 4, y + 1.5F, 0);
                g.pose().scale(1.5F, 1.5F, 1.0F);
                g.renderItem(stacks.get(0), 0, 0);
                g.pose().popPose();
            }
            g.drawString(font, entry.name(), x + 34, y + 5, TEXT, true);
            g.drawString(font, String.format("%,d", entry.price()), x + 34, y + 16,
                    balance >= entry.price() ? GOLD : 0xFFD06A6A, false);
            g.drawString(font, Component.translatable("mythoria.market.crowns"),
                    x + 38 + font.width(String.format("%,d", entry.price())), y + 16, MUTED, false);
        }
        if (list.size() > ROWS) {
            int trackH = ROWS * ROW_H - 3;
            int thumbH = trackH * ROWS / list.size();
            int thumbY = listY() + (trackH - thumbH) * scroll / (list.size() - ROWS);
            g.fill(left + 10 + LIST_W + 2, listY(), left + 10 + LIST_W + 4, listY() + trackH, 0x55000000);
            g.fill(left + 10 + LIST_W + 2, thumbY, left + 10 + LIST_W + 4, thumbY + thumbH, GOLD);
        }

        renderPreview(g, mouseX, mouseY, partial);
    }

    private void renderPreview(GuiGraphics g, int mouseX, int mouseY, float partial) {
        int x = previewX();
        int y = listY();
        int w = previewW();
        int h = PANEL_H - 66;
        frame(g, x, y, w, h, 0xFF1A1130, 0xFF372358, GOLD_DARK);
        if (selected == null) {
            return;
        }
        int stageH = h - 62;
        g.enableScissor(x + 1, y + 1, x + w - 1, y + stageH);
        float spin = (ticks + partial) * 2.2F;
        int cx = x + w / 2;
        int cy = y + stageH / 2 + 2;
        // soft pedestal light
        for (int i = 0; i < 6; i++) {
            g.fill(cx - 50 + i * 7, y + stageH - 14 + i, cx + 50 - i * 7, y + stageH - 13 + i, 0x30FFE08A);
        }
        LivingEntity living = previewEntity(selected);
        if (living != null) {
            float yaw = 180.0F + spin;
            living.setYRot(yaw);
            living.yRotO = yaw;
            living.yBodyRot = yaw;
            living.yBodyRotO = yaw;
            living.yHeadRot = yaw;
            living.yHeadRotO = yaw;
            living.setXRot(0);
            living.tickCount = ticks;
            Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
            Quaternionf camera = new Quaternionf().rotateX(-0.18F);
            pose.mul(camera);
            Vector3f offset = new Vector3f(0.0F, selected.lift(), 0.0F);
            InventoryScreen.renderEntityInInventory(g, cx, cy, selected.size(), offset, pose, camera, living);
        } else {
            List<ItemStack> stacks = selected.stacks();
            if (!stacks.isEmpty()) {
                renderSpinning(g, stacks.get(0), cx, cy, selected.size(), selected.lift(), spin);
            }
        }
        g.disableScissor();

        List<FormattedCharSequence> lines = font.split(selected.description(), w - 12);
        for (int i = 0; i < lines.size() && i < 3; i++) {
            g.drawString(font, lines.get(i), x + 6, y + stageH + 3 + i * 10, MUTED, false);
        }
        int bx = x + 6;
        int by = y + h - 24;
        boolean afford = balance >= selected.price();
        boolean hover = inside(mouseX, mouseY, bx, by, w - 12, 19);
        frame(g, bx, by, w - 12, 19,
                afford ? (hover ? 0xFF46C06A : 0xFF2F9A50) : 0xFF5A3A44,
                afford ? (hover ? 0xFF2C8A4A : 0xFF1C6834) : 0xFF3A2530,
                afford ? GOLD : 0xFF7A5560);
        Component label = Component.translatable("mythoria.market.buy", String.format("%,d", selected.price()));
        g.drawCenteredString(font, label, bx + (w - 12) / 2, by + 6, afford ? 0xFFFFFFFF : 0xFFD0A8B0);

        if (messageTicks > 0) {
            int mw = font.width(message) + 16;
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            frame(g, width / 2 - mw / 2, top + PANEL_H - 9, mw, 17, 0xF0201438, 0xF0100A1E, messageColor);
            g.drawCenteredString(font, message, width / 2, top + PANEL_H - 4, messageColor);
            g.pose().popPose();
        }
    }

    private void renderSpinning(GuiGraphics g, ItemStack stack, int cx, int cy, float size, float lift, float spin) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 150);
        pose.scale(size, -size, size);
        pose.mulPose(Axis.XP.rotationDegrees(12));
        if (category == MarketCategory.WEAPONS) {
            pose.mulPose(Axis.ZP.rotationDegrees(-24));
        }
        pose.mulPose(Axis.YP.rotationDegrees(spin));
        pose.translate(0, -lift, 0);
        g.flush();
        Lighting.setupForFlatItems();
        minecraft.getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, 0xF000F0, OverlayTexture.NO_OVERLAY,
                pose, g.bufferSource(), minecraft.level, 0);
        g.flush();
        Lighting.setupFor3DItems();
        pose.popPose();
    }

    /** A creature, or an armour stand wearing the pieces, built once per entry. */
    private LivingEntity previewEntity(MarketEntry entry) {
        if (minecraft.level == null || entry.entity() == null) {
            return null;
        }
        return previews.computeIfAbsent(entry.id(), key -> {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(entry.entity());
            if (type == null || !(type.create(minecraft.level) instanceof LivingEntity living)) {
                return null;
            }
            if (living instanceof ArmorStand) {
                for (ItemStack stack : entry.stacks()) {
                    if (stack.getItem() instanceof ArmorItem armor) {
                        EquipmentSlot slot = armor.getEquipmentSlot();
                        living.setItemSlot(slot, stack);
                    }
                }
            }
            return living;
        });
    }

    // ------------------------------------------------------------------ input
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button == 0) {
            MarketCategory[] categories = MarketCategory.values();
            for (int i = 0; i < categories.length; i++) {
                if (inside(mx, my, tabX(i), top + 32, 94, 17)) {
                    select(categories[i]);
                    click();
                    return true;
                }
            }
            List<MarketEntry> list = MarketCatalog.in(category);
            for (int row = 0; row < ROWS && row + scroll < list.size(); row++) {
                if (inside(mx, my, left + 10, listY() + row * ROW_H, LIST_W, ROW_H - 3)) {
                    selected = list.get(row + scroll);
                    click();
                    return true;
                }
            }
            int by = listY() + PANEL_H - 66 - 24;
            if (selected != null && inside(mx, my, previewX() + 6, by, previewW() - 12, 19)) {
                Net.requestBuy(selected.id());
                click();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int max = Math.max(0, MarketCatalog.in(category).size() - ROWS);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(dy)));
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (CoreClient.OPEN_MARKET.matches(key, scan)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    private void click() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
