package it.adryh.wardrobe;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

/** CI only: drives the mod inside a real game and asks the script for screenshots. */
final class DevHook {
    private static int step;
    private static long next;

    private DevHook() {}

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || step > 100) return;
        long now = System.currentTimeMillis();
        if (next == 0) {
            next = now + 20000;
            say("JOINED");
            return;
        }
        if (now < next) return;
        next = now + 7000;
        switch (step++) {
            case 0 -> {
                Wardrobe.undressAll();
                say("SEND_COMMAND");
                mc.player.connection.sendCommand("wardrobe");
            }
            case 1 -> shot(mc, "01-command-menu");
            case 2 -> mc.setScreen(null);
            case 3 -> say("PRESSKEY");
            case 4 -> {
                shot(mc, "02-key-menu");
                wear("cappellino_rosso", "felpa_adryh", "jeans", "sneakers_bianche", "occhiali_sole", "zaino");
            }
            case 5 -> shot(mc, "03-menu-worn");
            case 6 -> {
                mc.setScreen(null);
                mc.options.hideGui = true;
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            }
            case 7 -> shot(mc, "04-front");
            case 8 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 9 -> shot(mc, "05-back");
            case 10 -> {
                Wardrobe.undressAll();
                wear("corona", "smoking", "cargo", "anfibi", "ali_angelo");
            }
            case 11 -> shot(mc, "06-back-2");
            case 12 -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            case 13 -> shot(mc, "07-front-2");
            case 14 -> {
                Wardrobe.reload();
                mc.setScreen(new WardrobeScreen());
            }
            case 15 -> shot(mc, "08-after-reload");
            default -> {
                say("DONE errors=" + Wardrobe.errors().size());
                step = 1000;
                mc.stop();
            }
        }
    }

    private static void wear(String... ids) {
        for (Slot.Tab tab : Slot.Tab.values()) {
            for (Outfit outfit : Wardrobe.byTab(tab)) {
                for (String id : ids) {
                    if (outfit.id.equals(id) && !Wardrobe.isWorn(outfit)) Wardrobe.toggle(outfit);
                }
            }
        }
        say("WORN " + Wardrobe.worn().size() + "/" + ids.length);
    }

    private static void shot(Minecraft mc, String name) {
        say("STATE " + name + " screen=" + (mc.screen == null ? "none" : mc.screen.getClass().getSimpleName()));
        say("SHOT " + name);
    }

    private static void say(String message) {
        WardrobeLog.info("AUTOTEST:" + message);
    }
}
