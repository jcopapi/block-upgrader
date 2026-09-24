package dev.jco.upgrades.client;
import java.util.*;
import dev.jco.upgrades.IncompletePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.RenderShape;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
@EventBusSubscriber(modid="jco_upgrades",value=Dist.CLIENT)
public final class IncompleteOverlay {
 private record Key(String dim,BlockPos pos){}
 private static final Map<Key,IncompletePayload> entries=new HashMap<>();
 public static boolean contains(BlockPos pos){var level=Minecraft.getInstance().level;return level!=null&&entries.containsKey(new Key(level.dimension().location().toString(),pos));}
 public static void accept(IncompletePayload p){var key=new Key(p.dimension(),p.pos());if(p.block().isEmpty())entries.remove(key);else entries.put(key,p);}
 @SubscribeEvent public static void unload(net.neoforged.neoforge.event.level.ChunkEvent.Unload e){if(e.getLevel() instanceof net.minecraft.client.multiplayer.ClientLevel level){var chunk=e.getChunk().getPos();String dim=level.dimension().location().toString();entries.keySet().removeIf(k->k.dim.equals(dim)&&new net.minecraft.world.level.ChunkPos(k.pos).equals(chunk));}}
 @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){entries.clear();}
 @SubscribeEvent public static void render(RenderLevelStageEvent e){
  if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)return;
  var mc=Minecraft.getInstance();if(mc.level==null)return;String dim=mc.level.dimension().location().toString();
  var camera=e.getCamera().getPosition();var pose=e.getPoseStack();var buffers=mc.renderBuffers().bufferSource();
  var type=RenderType.crumbling(ResourceLocation.parse("jco_upgrades:textures/misc/construction.png"));
  for(var entry:entries.entrySet()){
   var pos=entry.getKey().pos;if(!entry.getKey().dim.equals(dim)||!mc.level.hasChunkAt(pos)||pos.distToCenterSqr(camera.x,camera.y,camera.z)>4096)continue;
   var state=mc.level.getBlockState(pos);if(!net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(entry.getValue().block()))continue;
   pose.pushPose();pose.translate(pos.getX()-camera.x,pos.getY()-camera.y,pos.getZ()-camera.z);
   try{
    var info=entry.getValue();float alpha=info.duration()>0?(float)Math.clamp((info.deadline()-mc.level.getGameTime())/(double)info.duration(),0,1):1;
    var consumer=new SheetedDecalTextureGenerator(new FadeVertex(buffers.getBuffer(type),alpha*(.6F+.1F*(float)Math.sin(mc.level.getGameTime()*.06))),pose.last(),1);
    if(state.getRenderShape()==RenderShape.MODEL)mc.getBlockRenderer().renderBreakingTexture(state,pos,mc.level,pose,consumer);
    var be=mc.level.getBlockEntity(pos);if(be!=null)mc.getBlockEntityRenderDispatcher().render(be,e.getPartialTick().getGameTimeDeltaPartialTick(false),pose,ignored->consumer);
   } finally{pose.popPose();}
  }
  buffers.endBatch(type);
 }
}
