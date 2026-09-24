package dev.jco.upgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record ImpactPayload(String dimension,BlockPos pos,float amplitude,int ticks) implements CustomPacketPayload {
 public static final Type<ImpactPayload> TYPE=new Type<>(ResourceLocation.parse("jco_upgrades:impact"));
 public static final StreamCodec<RegistryFriendlyByteBuf,ImpactPayload> CODEC=new StreamCodec<>(){
 public ImpactPayload decode(RegistryFriendlyByteBuf b){return new ImpactPayload(b.readUtf(256),b.readBlockPos(),b.readFloat(),b.readVarInt());}
 public void encode(RegistryFriendlyByteBuf b,ImpactPayload p){b.writeUtf(p.dimension,256);b.writeBlockPos(p.pos);b.writeFloat(p.amplitude);b.writeVarInt(p.ticks);}};
 public Type<ImpactPayload> type(){return TYPE;}
}
