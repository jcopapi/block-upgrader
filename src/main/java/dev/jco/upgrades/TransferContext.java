package dev.jco.upgrades;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Target is detached during preparation. Throw to abort before world/items are changed. */
public record TransferContext(ServerLevel level, BlockPos pos, ServerPlayer player,
        BlockState sourceState, BlockState resultState, CompoundTag sourceData, BlockEntity target) {
    public void copyData() {
        if (target == null) throw new IllegalStateException("Result has no block entity");
        target.loadWithComponents(sourceData.copy(), level.registryAccess());
    }
    public void copyInventory() {
        if (!(level.getBlockEntity(pos) instanceof net.minecraft.world.Container source)
            || !(target instanceof net.minecraft.world.Container destination))
            throw new IllegalStateException("Inventory transfer requires two inventories");
        target.setLevel(level);
        for(int slot=0;slot<source.getContainerSize();slot++) {
            var stack=source.getItem(slot);
            if(stack.isEmpty())continue;
            if(slot>=destination.getContainerSize()||!destination.canPlaceItem(slot,stack))
                throw new IllegalStateException("Result cannot accept source inventory slot "+slot);
            destination.setItem(slot,stack.copy());
        }
    }
}
