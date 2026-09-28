package dev.jco.upgrades;
import net.minecraft.core.*;import net.minecraft.core.component.DataComponents;import net.minecraft.core.registries.*;import net.minecraft.nbt.*;import net.minecraft.server.level.*;import net.minecraft.world.*;import net.minecraft.world.item.*;import net.minecraft.world.item.component.CustomData;import net.minecraft.world.level.block.*;import net.minecraft.world.phys.Vec3;
public final class Downgrades {
 private static final String ITEM_KEY="jco_upgrade_history";
 private record Pending(CompoundTag history,long tick){}
 private static final java.util.Map<net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>,java.util.Map<Long,Pending>> PENDING=new java.util.HashMap<>();
 public static void detach(ServerLevel level,BlockPos pos){var n=UpgradeData.get(level).history.get(pos.asLong());if(n!=null){var map=PENDING.computeIfAbsent(level.dimension(),key->new java.util.HashMap<>());map.values().removeIf(p->p.tick<level.getGameTime());map.put(pos.asLong(),new Pending(n.copy(),level.getGameTime()));}}
 public static void clear(){PENDING.clear();}
 public static void drops(net.neoforged.neoforge.event.level.BlockDropsEvent event){var level=event.getLevel();var map=PENDING.get(level.dimension());if(map==null)return;var pending=map.remove(event.getPos().asLong());if(pending==null||pending.tick!=level.getGameTime()||!BuiltInRegistries.BLOCK.getKey(event.getState().getBlock()).toString().equals(pending.history.getString("target")))return;
  var item=event.getState().getBlock().asItem();for(var drop:event.getDrops())if(drop.getItem().is(item)){CustomData.update(DataComponents.CUSTOM_DATA,drop.getItem(),tag->tag.put(ITEM_KEY,pending.history.copy()));break;}
 }
 public static void placed(net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent event){if(!(event.getLevel() instanceof ServerLevel level)||!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player))return;var pos=event.getPos();var target=BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock()).toString();
  for(var hand:InteractionHand.values()){var stack=player.getItemInHand(hand);var custom=stack.get(DataComponents.CUSTOM_DATA);if(custom==null)continue;var n=custom.copyTag().getCompound(ITEM_KEY);if(n.isEmpty()||!target.equals(n.getString("target"))||!stack.is(event.getPlacedBlock().getBlock().asItem()))continue;var data=UpgradeData.get(level);data.history.put(pos.asLong(),n.copy());data.setDirty();return;}
 }
 public static void remember(ServerLevel level,BlockPos pos,UpgradeDefinition d,UpgradeData.Progress p){
  if(!p.rollbackReady)return;var data=UpgradeData.get(level);var n=new CompoundTag();n.putString("target",p.target);n.put("source",p.sourceState.copy());n.put("data",p.sourceData.copy());n.putString("tool",d.reverseTool());n.putString("toolLabel",d.reverseLabel());n.putInt("need",d.reverseHits());n.putLong("last",Long.MIN_VALUE);var materials=new ListTag();p.escrow.forEach(s->{if(!s.isEmpty())materials.add(s.save(level.registryAccess()));});n.put("materials",materials);if(data.history.containsKey(pos.asLong()))n.put("previous",data.history.get(pos.asLong()).copy());data.history.put(pos.asLong(),n);data.setDirty();
 }
 public static void rememberPair(ServerLevel level,BlockPos pos,UpgradeDefinition d,UpgradeData.Progress p){
  if(!p.rollbackReady)return;
  var data=UpgradeData.get(level);var other=BlockPos.of(p.partner);
  var first=new java.util.ArrayList<ItemStack>();var second=new java.util.ArrayList<ItemStack>();
  var used=new int[d.materials().size()];
  for(var stack:p.escrow){
   boolean assigned=false;
   for(int i=0;i<d.materials().size();i++)if(used[i]<d.materials().get(i).count()*2&&d.materials().get(i).item().matches(stack)){
    (used[i]++<d.materials().get(i).count()?first:second).add(stack.copy());assigned=true;break;
   }
   if(!assigned)first.add(stack.copy());
  }
  data.history.put(pos.asLong(),pairHistory(level,pos,other,d,p.target,p.sourceState,p.sourceData,first,data));
  data.history.put(other.asLong(),pairHistory(level,other,pos,d,p.target,p.partnerSourceState,p.partnerSourceData,second,data));
  data.setDirty();
 }
 private static CompoundTag pairHistory(ServerLevel level,BlockPos pos,BlockPos other,UpgradeDefinition d,String target,CompoundTag source,CompoundTag sourceData,java.util.List<ItemStack> materials,UpgradeData data){
  var n=new CompoundTag();n.putString("target",target);n.put("source",source.copy());n.put("data",sourceData.copy());n.putString("tool",d.reverseTool());n.putString("toolLabel",d.reverseLabel());n.putInt("need",d.reverseHits());n.putLong("last",Long.MIN_VALUE);n.putLong("partner",other.asLong());
  var list=new ListTag();materials.forEach(s->{if(!s.isEmpty())list.add(s.save(level.registryAccess()));});n.put("materials",list);
  if(data.history.containsKey(pos.asLong()))n.put("previous",data.history.get(pos.asLong()).copy());return n;
 }
 public static CompoundTag valid(ServerLevel level,BlockPos pos){var data=UpgradeData.get(level);var n=data.history.get(pos.asLong());if(n==null||UpgradeRuntime.occupied(level,pos)||data.entries.containsKey(pos.asLong()))return new CompoundTag();if(!BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString().equals(n.getString("target")))return new CompoundTag();return n;}
 public static void hit(ServerPlayer p,BlockPos pos){var level=p.serverLevel();var n=valid(level,pos);if(n.isEmpty()||p.isSpectator()||!p.mayBuild()||!level.mayInteract(p,pos)||p.distanceToSqr(Vec3.atCenterOf(pos))>Math.pow(p.blockInteractionRange()+1,2)||!new StackMatcher(n.getString("tool")).matches(p.getMainHandItem()))return;
  BlockPos other=null;CompoundTag otherHistory=new CompoundTag();
  if(n.contains("partner")){
   other=BlockPos.of(n.getLong("partner"));
   if(other.equals(PairedBlocks.partner(level,pos))){otherHistory=valid(level,other);if(otherHistory.isEmpty()||!otherHistory.contains("partner")||otherHistory.getLong("partner")!=pos.asLong())return;}
   else if(PairedBlocks.partner(level,pos)!=null)return;
  }
  if(!InventorySafety.isEmpty(level.getBlockEntity(pos))||other!=null&&!otherHistory.isEmpty()&&!InventorySafety.isEmpty(level.getBlockEntity(other)))return;
  long now=level.getGameTime(),last=n.getLong("last");if(last!=Long.MIN_VALUE&&now-last<10)return;
  var data=UpgradeData.get(level);int hits=n.getInt("hits")+1;n.putInt("hits",hits);n.putLong("last",now);
  if(!otherHistory.isEmpty()){otherHistory.putInt("hits",hits);otherHistory.putLong("last",now);}data.setDirty();
  new Feedback().sound("minecraft:block.anvil.hit",.5F,1).particles("minecraft:crit",4,.2,.02).emit(level,Vec3.atCenterOf(pos),p,InteractionHand.MAIN_HAND,p.getMainHandItem(),true);if(hits<n.getInt("need"))return;
  var state=NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK),n.getCompound("source"));
  UpgradeRuntime.TRANSFORMING.set(true);
  try{
   restore(level,pos,n,otherHistory.isEmpty());
   if(!otherHistory.isEmpty())restore(level,other,otherHistory,false);
   level.updateNeighborsAt(pos,level.getBlockState(pos).getBlock());
   if(!otherHistory.isEmpty())level.updateNeighborsAt(other,level.getBlockState(other).getBlock());
  }catch(RuntimeException ex){n.putInt("hits",hits-1);if(!otherHistory.isEmpty())otherHistory.putInt("hits",hits-1);return;}
  finally{UpgradeRuntime.TRANSFORMING.remove();}
  refund(p,level,pos,n,data);
  if(!otherHistory.isEmpty())refund(p,level,other,otherHistory,data);
  data.setDirty();new Feedback().particles("minecraft:happy_villager",12,.4,.03).emit(level,Vec3.atCenterOf(pos),p,InteractionHand.MAIN_HAND,state.getBlock().asItem().getDefaultInstance(),false);
 }
 private static void restore(ServerLevel level,BlockPos pos,CompoundTag history,boolean single){
  var state=NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK),history.getCompound("source"));if(single&&history.contains("partner"))state=PairedBlocks.single(state);
  if(!level.getBlockState(pos).equals(state)&&!level.setBlock(pos,state,18))throw new IllegalStateException("Block replacement was rejected");
  var be=level.getBlockEntity(pos);if(be!=null){be.loadWithComponents(history.getCompound("data"),level.registryAccess());if(net.neoforged.fml.ModList.get().isLoaded("sophisticatedstorage"))dev.jco.upgrades.integration.SophisticatedStorage.restoreWoodFromSnapshot(be,history.getCompound("data"));be.setChanged();level.sendBlockUpdated(pos,state,state,3);}
 }
 private static void refund(ServerPlayer player,ServerLevel level,BlockPos pos,CompoundTag n,UpgradeData data){
  data.history.remove(pos.asLong());if(n.contains("previous"))data.history.put(pos.asLong(),n.getCompound("previous").copy());
  for(var raw:n.getList("materials",10)){var stack=ItemStack.parseOptional(level.registryAccess(),(CompoundTag)raw);if(!player.getInventory().add(stack))player.drop(stack,false);}
 }
 public static CompoundTag view(ServerLevel level,BlockPos pos){var n=valid(level,pos);if(n.isEmpty())return new CompoundTag();var state=NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK),n.getCompound("source"));var out=new CompoundTag();out.putBoolean("reverse",true);out.putString("tool",n.getString("tool"));out.putString("id","reverse:"+n.getString("target"));out.putString("title",state.getBlock().getName().getString());out.putString("description","Right-click to undo / refund");out.putDouble("range",6);out.put("icon",state.getBlock().asItem().getDefaultInstance().saveOptional(level.registryAccess()));var rows=new ListTag();var row=new CompoundTag();var matcher=new StackMatcher(n.getString("tool"));var display=UpgradeRuntime.representative(matcher);row.put("icon",display.saveOptional(level.registryAccess()));row.putString("label",UpgradeRuntime.label(matcher,display,n.getString("toolLabel")));row.putString("input","RIGHT");row.putInt("have",n.getInt("hits"));row.putInt("need",n.getInt("need"));rows.add(row);out.put("rows",rows);return out;}
}
