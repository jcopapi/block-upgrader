package dev.jco.upgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record IncompletePayload(String dimension,BlockPos pos,String block,long deadline,int duration) implements CustomPacketPayload {
 public IncompletePayload(String dimension,BlockPos pos,String block){this(dimension,pos,block,-1,0);}
 public static final Type<IncompletePayload> TYPE=new Type<>(ResourceLocation.parse("jco_upgrades:incomplete"));
 public static final StreamCodec<RegistryFriendlyByteBuf,IncompletePayload> CODEC=new StreamCodec<>(){
 public IncompletePayload decode(RegistryFriendlyByteBuf b){return new IncompletePayload(b.readUtf(256),b.readBlockPos(),b.readUtf(256),b.readLong(),b.readVarInt());}
 public void encode(RegistryFriendlyByteBuf b,IncompletePayload p){b.writeUtf(p.dimension,256);b.writeBlockPos(p.pos);b.writeUtf(p.block,256);b.writeLong(p.deadline);b.writeVarInt(p.duration);}};
 public Type<IncompletePayload> type(){return TYPE;}
}
