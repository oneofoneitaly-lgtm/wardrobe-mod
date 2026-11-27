package it.adryh.invstyles.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.CommandDispatcher;
import it.adryh.invstyles.paint.Accent;
import it.adryh.invstyles.paint.Category;
import it.adryh.invstyles.paint.Style;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

public final class ClientSetup {
    private static final KeyMapping OPEN = new KeyMapping("key.invstyles.open", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_I, "key.categories.invstyles");
    /** textures were (re)loaded and need their styles again */
    private static boolean stale = true;
    private static boolean wantsMenu;

    private ClientSetup() {}

    public static void init(FMLJavaModLoadingContext context) {
        Config.load();
        context.getModEventBus().addListener((RegisterKeyMappingsEvent event) -> event.register(OPEN));
        context.getModEventBus().addListener((RegisterClientReloadListenersEvent event) ->
                event.registerReloadListener((ResourceManagerReloadListener) manager -> stale = true));
        MinecraftForge.EVENT_BUS.addListener((TickEvent.ClientTickEvent.Post event) -> tick());
        MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> commands(event.getDispatcher()));
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) -> button(event));
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (stale && mc.getOverlay() == null) {
            stale = false;
            Styles.apply(null);
        }
        boolean pressed = false;
        while (OPEN.consumeClick()) {
            pressed = true;
        }
        if ((pressed || wantsMenu) && mc.screen == null) {
            wantsMenu = false;
            mc.setScreen(new StyleScreen(null));
        }
    }

    /** A small button in the corner of every inventory. */
    private static void button(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?>)) {
            return;
        }
        event.addListener(Button.builder(Component.translatable("invstyles.button"), pressed -> openFrom(screen))
                .bounds(4, 4, 44, 16).tooltip(Tooltip.create(Component.translatable("invstyles.title"))).build());
    }

    private static void openFrom(Screen from) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        boolean own = from instanceof InventoryScreen || from instanceof CreativeModeInventoryScreen;
        Category shown = own ? Category.INVENTORY : null;
        mc.player.closeContainer();
        mc.setScreen(new StyleScreen(shown == null ? null : () -> new InventoryScreen(mc.player)));
    }

    /** /invstyle, /invstyle set <group|all> <style>, /invstyle colour <name> */
    private static void commands(CommandDispatcher<CommandSourceStack> dispatcher) {
        var set = Commands.literal("set");
        for (int i = -1; i < Category.values().length; i++) {
            Category category = i < 0 ? null : Category.values()[i];
            var group = Commands.literal(category == null ? "all" : category.id());
            for (Style style : Style.values()) {
                group.then(Commands.literal(style.id()).executes(command -> {
                    choose(category, style);
                    return 1;
                }));
            }
            set.then(group);
        }
        var colour = Commands.literal("colour");
        for (Accent accent : Accent.values()) {
            colour.then(Commands.literal(accent.id()).executes(command -> {
                choose(accent);
                return 1;
            }));
        }
        dispatcher.register(Commands.literal("invstyle").then(set).then(colour).executes(command -> {
            wantsMenu = true;
            return 1;
        }));
    }

    static void choose(Category category, Style style) {
        for (Category each : Category.values()) {
            if (category == null || category == each) {
                Config.style(each, style);
            }
        }
        Config.save();
        Styles.apply(category);
    }

    static void choose(Accent accent) {
        Config.accent(accent);
        Config.save();
        Styles.apply(null);
    }
}
