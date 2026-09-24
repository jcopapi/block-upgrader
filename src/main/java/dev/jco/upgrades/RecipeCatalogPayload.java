package dev.jco.upgrades;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** The server's resolved datapack and KubeJS definitions, for optional recipe viewers. */
public record RecipeCatalogPayload(CompoundTag catalog) implements CustomPacketPayload {
    public static final Type<RecipeCatalogPayload> TYPE = new Type<>(ResourceLocation.parse("jco_upgrades:recipe_catalog"));
    public static final StreamCodec<RegistryFriendlyByteBuf,RecipeCatalogPayload> CODEC = new StreamCodec<>() {
        @Override public RecipeCatalogPayload decode(RegistryFriendlyByteBuf buf) {
            CompoundTag tag = buf.readNbt();
            return new RecipeCatalogPayload(tag == null ? new CompoundTag() : tag);
        }
        @Override public void encode(RegistryFriendlyByteBuf buf, RecipeCatalogPayload payload) {
            buf.writeNbt(payload.catalog);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
