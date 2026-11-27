package it.adryh.wardrobe;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** Version independent behaviour behind the events. */
public final class ClientHooks {
    private static boolean openRequested;

    private ClientHooks() {}

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        for (String name : new String[]{"wardrobe", "armadio"}) {
            dispatcher.register(Commands.literal(name).executes(ctx -> {
                // the chat closes right after the command: open on the next tick
                openRequested = true;
                return 1;
            }));
        }
    }

    public static void tick(KeyMapping key) {
        Minecraft mc = Minecraft.getInstance();
        boolean pressed = false;
        while (key.consumeClick()) pressed = true;
        if ((pressed || openRequested) && mc.player != null) {
            if (openRequested || mc.screen == null) mc.setScreen(new WardrobeScreen());
        }
        openRequested = false;
    }
}
