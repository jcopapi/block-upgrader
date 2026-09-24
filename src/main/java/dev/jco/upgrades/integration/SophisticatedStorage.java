package dev.jco.upgrades.integration;

import dev.jco.upgrades.TransferContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.p3pp3rf1y.sophisticatedstorage.block.ChestBlock;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockBase;
import net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockEntity;

/** Isolated optional bridge. Matches Sophisticated Storage's own block-entity swap protocol. */
public final class SophisticatedStorage {
    private SophisticatedStorage() {}

    public static void prepare(TransferContext context) {
        if (!(context.level().getBlockEntity(context.pos()) instanceof StorageBlockEntity source)
            || !(context.target() instanceof StorageBlockEntity)
            || !(context.resultState().getBlock() instanceof StorageBlockBase))
            throw new IllegalStateException("Sophisticated transfer requires two Sophisticated Storage blocks");
        if (context.sourceState().hasProperty(ChestBlock.TYPE)
            && context.sourceState().getValue(ChestBlock.TYPE) != ChestType.SINGLE)
            throw new IllegalStateException("Separate double chests before upgrading");
        if (source.getControllerPos().isPresent())
            throw new IllegalStateException("Unlink this storage from its controller first");
        context.copyData();
        applyWoodType(source,context.target());
    }

    public static boolean isStorage(BlockEntity entity) { return entity instanceof StorageBlockEntity; }

    /** Copy the source's visual components to a prospective storage item. */
    public static ItemStack preview(ServerLevel level,BlockPos pos,ItemStack stack,CompoundTag resultData) {
        if(!(stack.getItem() instanceof net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem)
            || !(level.getBlockState(pos).getBlock() instanceof net.p3pp3rf1y.sophisticatedstorage.block.WoodStorageBlockBase source))return stack;
        source.addNameWoodAndTintData(stack,level,pos);
        if(resultData.contains("woodType")){
            String name=resultData.getString("woodType");
            var type=WoodType.values().filter(value->value.name().equals(name)||("minecraft:"+value.name()).equals(name)).findFirst();
            if(type.isPresent())stack=net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem.setWoodType(stack,type.get());
        }
        return stack;
    }

    public static ItemStack previewStatic(ItemStack stack,CompoundTag resultData) {
        if (!(stack.getItem() instanceof net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem)
            || !resultData.contains("woodType")) return stack;
        String name = resultData.getString("woodType");
        var type = WoodType.values().filter(value -> value.name().equals(name) || ("minecraft:" + value.name()).equals(name)).findFirst();
        return type.map(value -> net.p3pp3rf1y.sophisticatedstorage.item.WoodStorageBlockItem.setWoodType(stack, value)).orElse(stack);
    }

    public static boolean isEmpty(BlockEntity entity) {
        if (!(entity instanceof StorageBlockEntity storage)) return false;
        var wrapper = storage.getStorageWrapper();
        var items = wrapper.getInventoryHandler();
        for (int i = 0; i < items.getSlots(); i++) if (!items.getStackInSlot(i).isEmpty()) return false;
        var upgrades = wrapper.getUpgradeHandler();
        for (int i = 0; i < upgrades.getSlots(); i++) if (!upgrades.getStackInSlot(i).isEmpty()) return false;
        return true;
    }

    public static void clear(BlockEntity entity) { ((StorageBlockEntity) entity).clearContent(); }

    public static void detach(ServerLevel level, BlockPos pos, BlockEntity entity) {
        ((StorageBlockEntity) entity).setBeingUpgraded(true);
        level.removeBlockEntity(pos);
    }

    public static void installed(BlockEntity source,BlockEntity entity, BlockState state) {
        var storage = (StorageBlockEntity) entity;
        var block = (StorageBlockBase) state.getBlock();
        var wrapper = storage.getStorageWrapper();
        storage.changeStorageSize(block.getNumberOfInventorySlots() - wrapper.getInventoryHandler().getSlots(),
            block.getNumberOfUpgradeSlots() - wrapper.getUpgradeHandler().getSlots());
        applyWoodType(source,entity);
        storage.setBeingUpgraded(false);
        storage.setChanged();
    }

    private static void applyWoodType(BlockEntity source,BlockEntity target) {
        if(target instanceof WoodStorageBlockEntity wood){
            if(source instanceof WoodStorageBlockEntity original)
                wood.setWoodType(original.getWoodType().orElse(null));
        }
    }

    public static void failed(BlockEntity entity) {
        if (entity instanceof StorageBlockEntity storage) storage.setBeingUpgraded(false);
    }

    /** Sophisticated's NBT loader defaults absent woodType to acacia; absence is meaningful. */
    public static void restoreWoodFromSnapshot(BlockEntity entity,CompoundTag data) {
        if(entity instanceof WoodStorageBlockEntity wood&&!data.contains("woodType"))wood.setWoodType(null);
    }
}
