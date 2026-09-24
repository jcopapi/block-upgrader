package dev.jco.upgrades.client;

import dev.jco.upgrades.RecipeCatalogPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Holds the authoritative server catalog; JEI may start before it arrives. */
@EventBusSubscriber(modid = "jco_upgrades", value = Dist.CLIENT)
public final class RecipeCatalogClient {
    private static CompoundTag catalog = new CompoundTag();
    private static boolean pending;
    private static long revision;
    private RecipeCatalogClient() {}

    public static CompoundTag current() { return catalog.copy(); }
    public static long revision() { return revision; }
    public static void accept(RecipeCatalogPayload packet) {
        catalog = packet.catalog().copy();
        revision++;
        pending = true;
        refresh();
    }
    private static void refresh() {
        if (!pending || Minecraft.getInstance().level == null) return;
        pending = false;
        if (ModList.get().isLoaded("jei")) dev.jco.upgrades.client.jei.UpgradeJeiPlugin.refresh();
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) { refresh(); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        catalog = new CompoundTag();
        revision++;
        pending = true;
        if (ModList.get().isLoaded("jei")) dev.jco.upgrades.client.jei.UpgradeJeiPlugin.refresh();
    }
}
