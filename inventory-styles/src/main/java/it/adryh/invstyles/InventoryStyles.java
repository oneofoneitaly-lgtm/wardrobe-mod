package it.adryh.invstyles;

import it.adryh.invstyles.client.ClientSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** A client-side mod: it only changes how inventories look. */
@Mod(InventoryStyles.ID)
public final class InventoryStyles {
    public static final String ID = "invstyles";

    public InventoryStyles(FMLJavaModLoadingContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientSetup.init(context);
        }
    }
}
