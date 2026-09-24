package dev.jco.upgrades;

import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.fml.ModList;

/** A rollback is only safe when nothing is currently stored in the target. */
public final class InventorySafety {
    private InventorySafety() {}
    public static boolean isEmpty(BlockEntity entity) {
        if (entity == null) return true;
        if (entity instanceof Container container) return container.isEmpty();
        return ModList.get().isLoaded("sophisticatedstorage")
            && dev.jco.upgrades.integration.SophisticatedStorage.isStorage(entity)
            && dev.jco.upgrades.integration.SophisticatedStorage.isEmpty(entity);
    }
}
