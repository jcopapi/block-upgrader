package dev.jco.upgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record WorkPayload(BlockPos pos,int step) implements CustomPacketPayload {
 public static final Type<WorkPayload> TYPE=new Type<>(ResourceLocation.parse("jco_upgrades:work"));
 public static final StreamCodec<RegistryFriendlyByteBuf,WorkPayload> CODEC=new StreamCodec<>(){
 public WorkPayload decode(RegistryFriendlyByteBuf b){return new WorkPayload(b.readBlockPos(),b.readVarInt());}
 public void encode(RegistryFriendlyByteBuf b,WorkPayload p){b.writeBlockPos(p.pos);b.writeVarInt(p.step);}};
 public Type<WorkPayload> type(){return TYPE;}
}
