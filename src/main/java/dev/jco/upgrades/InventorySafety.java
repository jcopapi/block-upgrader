package dev.jco.upgrades;

import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;

/** A rollback is only safe when nothing is currently stored in the target. */
public final class InventorySafety {
    private InventorySafety() {}
    public static boolean isEmpty(BlockEntity entity) {
        if (entity == null) return true;
        if (entity instanceof net.minecraft.world.level.block.entity.BedBlockEntity) return true;
        if (entity instanceof Container container) return container.isEmpty();
        return ModList.get().isLoaded("sophisticatedstorage")
            && dev.jco.upgrades.integration.SophisticatedStorage.isStorage(entity)
            && dev.jco.upgrades.integration.SophisticatedStorage.isEmpty(entity);
    }
    /** Undo restores block metadata, never inventory items that were already withdrawn. */
    public static void clearRestoredContents(BlockEntity entity) {
        if (entity instanceof Container container) container.clearContent();
        else if (entity != null && ModList.get().isLoaded("sophisticatedstorage")
            && dev.jco.upgrades.integration.SophisticatedStorage.isStorage(entity))
            dev.jco.upgrades.integration.SophisticatedStorage.clear(entity);
    }
}
