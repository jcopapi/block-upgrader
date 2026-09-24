package dev.jco.upgrades;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record SelectRoutePayload(BlockPos pos,int step) implements CustomPacketPayload {
 public static final Type<SelectRoutePayload> TYPE=new Type<>(ResourceLocation.parse("jco_upgrades:select"));
 public static final StreamCodec<RegistryFriendlyByteBuf,SelectRoutePayload> CODEC=new StreamCodec<>(){
 public SelectRoutePayload decode(RegistryFriendlyByteBuf b){return new SelectRoutePayload(b.readBlockPos(),b.readVarInt());}
 public void encode(RegistryFriendlyByteBuf b,SelectRoutePayload p){b.writeBlockPos(p.pos);b.writeVarInt(p.step);}};
 public Type<SelectRoutePayload> type(){return TYPE;}
}
