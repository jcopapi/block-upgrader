package dev.jco.upgrades;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpgradePayload(String dimension, BlockPos pos, List<String> lines,net.minecraft.world.item.ItemStack display,int remaining,net.minecraft.nbt.CompoundTag view) implements CustomPacketPayload {
    public UpgradePayload(String dimension,BlockPos pos,List<String> lines) {this(dimension,pos,lines,net.minecraft.world.item.ItemStack.EMPTY,0);}
    public UpgradePayload(String dimension,BlockPos pos,List<String> lines,net.minecraft.world.item.ItemStack display,int remaining){this(dimension,pos,lines,display,remaining,new net.minecraft.nbt.CompoundTag());}
    public static final Type<UpgradePayload> TYPE=new Type<>(ResourceLocation.parse("jco_upgrades:overlay"));
    public static final StreamCodec<RegistryFriendlyByteBuf,UpgradePayload> CODEC=new StreamCodec<>() {
        public UpgradePayload decode(RegistryFriendlyByteBuf b) {
            String dim=b.readUtf(256); var pos=b.readBlockPos(); int n=b.readVarInt();
            if(n<0 || n>70) throw new IllegalArgumentException("Invalid overlay size");
            var lines=new ArrayList<String>(); for(int i=0;i<n;i++) lines.add(b.readUtf(1024));
            return new UpgradePayload(dim,pos,List.copyOf(lines),net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode(b),b.readVarInt(),b.readNbt());
        }
        public void encode(RegistryFriendlyByteBuf b, UpgradePayload p) {
            b.writeUtf(p.dimension,256); b.writeBlockPos(p.pos); b.writeVarInt(p.lines.size());
            p.lines.forEach(s -> b.writeUtf(s,1024));
            net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode(b,p.display);b.writeVarInt(p.remaining);b.writeNbt(p.view);
        }
    };
    public Type<UpgradePayload> type() { return TYPE; }
}
