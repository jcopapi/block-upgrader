package dev.jco.upgrades;

import com.mojang.logging.LogUtils;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class UpgradeRuntime {
 private static final ThreadLocal<InteractionMode> actionInput=ThreadLocal.withInitial(()->InteractionMode.RIGHT);
 public static void work(ServerPlayer player,WorkPayload packet){if(packet.step()<0||packet.step()>2)return;var hit=player.pick(player.blockInteractionRange(),1,false);if(!(hit instanceof BlockHitResult b)||hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK||!b.getBlockPos().equals(packet.pos()))return;if(packet.step()==2){Downgrades.hit(player,packet.pos());relevant(player.serverLevel(),packet.pos());return;}actionInput.set(packet.step()==0?InteractionMode.LEFT:InteractionMode.RIGHT);try{interact(new PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,packet.pos(),b));}finally{actionInput.remove();}}
 
    public static final ThreadLocal<Boolean> TRANSFORMING = ThreadLocal.withInitial(() -> false);
    public static UpgradeDefinition find(BlockState state) {
        return Upgrades.routes(state).stream().findFirst().orElse(null);
    }
    public static boolean occupied(ServerLevel level,BlockPos pos){
        if(!net.neoforged.fml.ModList.get().isLoaded("jco_item_upgrades"))return false;
        try{return (boolean)Class.forName("dev.jco.itemupgrades.StationRuntime").getMethod("occupied",ServerLevel.class,BlockPos.class).invoke(null,level,pos);}catch(ReflectiveOperationException ex){throw new IllegalStateException("Cannot check station inventory",ex);}
    }
    private record Selection(net.minecraft.core.GlobalPos pos,String id){}
    private static final Map<UUID,Selection> SELECTION=new HashMap<>();
    public static List<UpgradeDefinition> routes(BlockState state){return Upgrades.routes(state);}
    public static UpgradeDefinition selected(ServerPlayer player,BlockPos pos){
        var level=player.serverLevel();if(occupied(level,pos))return null;var p=UpgradeData.get(level).entries.get(pos.asLong());
        if(p!=null)return Upgrades.definitions().get(p.id);
        var all=routes(level.getBlockState(pos));if(all.isEmpty())return null;
        var selected=SELECTION.get(player.getUUID());
        return selected!=null&&selected.pos.equals(net.minecraft.core.GlobalPos.of(level.dimension(),pos))?all.stream().filter(d->d.id.equals(selected.id)).findFirst().orElse(all.getFirst()):all.getFirst();
    }
    public static boolean targeted(ServerPlayer player,BlockPos pos,double range){
        if(player.isSpectator()||!player.isShiftKeyDown()||player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))>range*range)return false;
        var hit=player.pick(Math.min(range,player.blockInteractionRange()),1,false);
        return hit instanceof BlockHitResult b&&hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&b.getBlockPos().equals(pos);
    }
    public static void select(ServerPlayer player,SelectRoutePayload packet){
        var level=player.serverLevel();if(Math.abs(packet.step())!=1||!level.hasChunkAt(packet.pos())||UpgradeData.get(level).entries.containsKey(packet.pos().asLong()))return;
        var current=selected(player,packet.pos());if(current==null||!targeted(player,packet.pos(),current.hudRange()))return;
        var all=routes(level.getBlockState(packet.pos()));int index=Math.floorMod(all.indexOf(current)+packet.step(),all.size());
        SELECTION.put(player.getUUID(),new Selection(net.minecraft.core.GlobalPos.of(level.dimension(),packet.pos()),all.get(index).id));snapshot(player,packet.pos());
    }
    public static boolean incomplete(ServerLevel level,BlockPos pos){var p=UpgradeData.get(level).entries.get(pos.asLong());return p!=null&&p.incomplete;}
    public static void incompleteSync(ServerLevel level,BlockPos pos,String target){
        var p=UpgradeData.get(level).entries.get(pos.asLong());var d=p==null?null:Upgrades.definitions().get(p.id);PacketDistributor.sendToPlayersTrackingChunk(level,new net.minecraft.world.level.ChunkPos(pos),new IncompletePayload(level.dimension().location().toString(),pos,target,p==null?-1:p.deadline,d==null?0:d.buildTime()));
    }
    public static void chunk(net.neoforged.neoforge.event.level.ChunkWatchEvent.Sent e){
        UpgradeData.get(e.getLevel()).entries.forEach((key,p)->{var pos=BlockPos.of(key);if(p.incomplete&&new net.minecraft.world.level.ChunkPos(pos).equals(e.getPos()))PacketDistributor.sendToPlayer(e.getPlayer(),new IncompletePayload(e.getLevel().dimension().location().toString(),pos,p.target,p.deadline,Upgrades.definitions().containsKey(p.id)?Upgrades.definitions().get(p.id).buildTime():0));});
    }
    public static void removed(ServerLevel level, BlockPos pos) {
        if (TRANSFORMING.get()) return;
        var data=UpgradeData.get(level);Downgrades.detach(level,pos);if(data.history.remove(pos.asLong())!=null)data.setDirty(); var p=data.entries.get(pos.asLong());if(p!=null&&p.prepared&&!p.completed){p.removeOnComplete=true;data.schedule(pos.asLong(),level.getGameTime()+1);data.setDirty();return;}data.entries.remove(pos.asLong());
        if(p!=null) { incompleteSync(level,pos,"");data.setDirty(); if(!p.incomplete)p.escrow.forEach(s -> Block.popResource(level,pos,s.copy())); }
    }
    /** Player cancellation restores only a reversible, empty-inventory construction snapshot. */
    public static boolean cancel(ServerPlayer player,BlockPos pos){
        var level=player.serverLevel();var data=UpgradeData.get(level);var p=data.entries.get(pos.asLong());
        if(p==null||p.committing||p.prepared||p.completed||player.isSpectator()||!player.mayBuild()||!level.mayInteract(player,pos)||player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))>Math.pow(player.blockInteractionRange()+1,2))return false;
        if(p.incomplete){
            if(!p.rollbackReady||!InventorySafety.isEmpty(level.getBlockEntity(pos))){
                player.displayClientMessage(Component.literal("Cannot safely undo this construction: legacy state or transferred inventory. Materials already built into it are retained."),true);return false;
            }
            var current=level.getBlockState(pos);if(!net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(current.getBlock()).toString().equals(p.target))return false;
            var source=net.minecraft.nbt.NbtUtils.readBlockState(level.holderLookup(net.minecraft.core.registries.Registries.BLOCK),p.sourceState);
            p.committing=true;TRANSFORMING.set(true);
            try{if(!level.setBlock(pos,source,3))return false;var be=level.getBlockEntity(pos);if(be!=null){be.loadWithComponents(p.sourceData,level.registryAccess());if(net.neoforged.fml.ModList.get().isLoaded("sophisticatedstorage"))dev.jco.upgrades.integration.SophisticatedStorage.restoreWoodFromSnapshot(be,p.sourceData);be.setChanged();level.sendBlockUpdated(pos,source,source,3);}}
            finally{TRANSFORMING.set(false);p.committing=false;}
        }
        data.entries.remove(pos.asLong());data.setDirty();incompleteSync(level,pos,"");
        for(var deposited:p.escrow){var stack=deposited.copy();if(!player.getInventory().add(stack))player.drop(stack,false);}p.escrow.clear();
        player.swing(InteractionHand.MAIN_HAND,true);level.playSound(null,pos,net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),net.minecraft.sounds.SoundSource.PLAYERS,.35F,.8F);relevant(level,pos);return true;
    }
    private static UpgradeData.Progress progress(ServerLevel level, BlockPos pos, UpgradeDefinition d, boolean create) {
        var data=UpgradeData.get(level); var p=data.entries.get(pos.asLong());
        if(p!=null && (!p.id.equals(d.id) || !p.fingerprint.equals(d.fingerprint()) || p.supplied.length!=d.materials().size())) {
            if(p.incomplete)return p;
            removed(level,pos); p=null;
        }
        if(p==null && create) { p=new UpgradeData.Progress(d); data.entries.put(pos.asLong(),p); data.setDirty(); }
        return p;
    }
    public static void interact(PlayerInteractEvent.RightClickBlock e) {
        if(e.getHand()!=InteractionHand.MAIN_HAND)return;
        if(!(e.getEntity() instanceof ServerPlayer player)||!(e.getLevel() instanceof ServerLevel level))return;
        var pos=e.getPos();if(occupied(level,pos))return;var old=UpgradeData.get(level).entries.get(pos.asLong());
        if(old!=null&&old.incomplete){e.setCanceled(true);e.setCancellationResult(InteractionResult.CONSUME);}
        if(old!=null&&actionInput.get()==InteractionMode.RIGHT&&player.isShiftKeyDown()&&player.getMainHandItem().isEmpty()){e.setCanceled(true);e.setCancellationResult(InteractionResult.CONSUME);cancel(player,pos);return;}
        var d=selected(player,pos);if(d==null)return;
        var stack=player.getMainHandItem();
        boolean clipboard=net.neoforged.fml.ModList.get().isLoaded("create")&&net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("create:clipboard");
        if(!player.isShiftKeyDown() && old==null && !d.materials().isEmpty() && !clipboard)return;
        e.setCanceled(true);e.setCancellationResult(InteractionResult.CONSUME);
        if(!player.mayBuild()||!level.mayInteract(player,pos)||player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))>Math.pow(Math.min(d.hudRange(),player.blockInteractionRange()+1),2))return;
        if(clipboard) {
            try{dev.jco.upgrades.integration.CreateClipboard.save(stack,d);player.swinging=false;player.swing(e.getHand(),true);player.displayClientMessage(Component.literal("Saved: "+d.title()),true);level.playSound(null,pos,net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN,net.minecraft.sounds.SoundSource.PLAYERS,.65F,1.2F);}catch(RuntimeException ex){error(player,ex);}return;
        }
        var p=progress(level,pos,d,false);if(p!=null&&p.incomplete&&!p.fingerprint.equals(d.fingerprint())){error(player,new IllegalStateException("Incomplete definition changed; restore definition or cancel administratively"));return;}
        if(p==null)p=new UpgradeData.Progress(d);
        if(p.committing||p.completed)return;
        if(p.prepared){finishIfReady(level,pos,player,d,p);return;}
        if(p.lastAction==level.getGameTime())return;
        var data=UpgradeData.get(level);
        if(actionInput.get()==InteractionMode.LEFT&&!p.incomplete)return;
        if(!p.incomplete)for(int i=0;i<d.materials().size();i++) {
            var m=d.materials().get(i);if(p.supplied[i]>=m.count()||!m.item().matches(stack))continue;
            var item=stack.copyWithCount(1);p.supplied[i]++;p.lastAction=level.getGameTime();data.entries.put(pos.asLong(),p);
            if(!player.isCreative()){p.escrow.add(item.copy());stack.shrink(1);}data.setDirty();
            m.feedback().emit(level,net.minecraft.world.phys.Vec3.atCenterOf(pos),player,e.getHand(),item,false);
            finishIfReady(level,pos,player,d,p);relevant(level,pos);return;
        }
        if(!materialsDone(d,p)){if(old==null&&actionInput.get()==InteractionMode.RIGHT){data.entries.put(pos.asLong(),p);data.setDirty();new Feedback().sound("minecraft:block.note_block.chime",.3F,1.4F).particles("",0,0,0).emit(level,net.minecraft.world.phys.Vec3.atCenterOf(pos),player,e.getHand(),d.display(),false);relevant(level,pos);}return;}
        if(!p.incomplete){data.entries.put(pos.asLong(),p);finishIfReady(level,pos,player,d,p);if(!d.itemOutput()||p.completed||!p.incomplete){relevant(level,pos);return;}}
        if(p.incomplete&&d.buildTime()>0){
            if(p.deadline<=level.getGameTime()&&p.stage>=d.stages().size()){finishIfReady(level,pos,player,d,p);relevant(level,pos);return;}
            if(accelerate(level,pos,player,d,p,stack)){finishIfReady(level,pos,player,d,p);relevant(level,pos);return;}
        }
        if(p.stage<d.stages().size()) {
            var stage=d.stages().get(p.stage);
            if(p.lastAction!=Long.MIN_VALUE&&level.getGameTime()-p.lastAction<stage.feedback().cooldown())return;
            if(!stage.feedback().input().accepts(actionInput.get())||!stage.item().matches(stack)||stack.getCount()<stage.consume()||stage.damage()>0&&(!stack.isDamageableItem()||stack.getMaxDamage()-stack.getDamageValue()<stage.damage()))return;
            try{if(!Upgrades.STAGE_TYPES.get(stage.type()).test(new Upgrades.Action(player,level,pos,stack,stage)))return;}catch(RuntimeException ex){error(player,ex);return;}
            var item=stack.copyWithCount(1);
            if(!player.isCreative()){if(stage.consume()>0&&!d.itemOutput())p.escrow.add(stack.copyWithCount(stage.consume()));stack.shrink(stage.consume());if(stage.damage()>0&&!stack.isEmpty())stack.hurtAndBreak(stage.damage(),player,EquipmentSlot.MAINHAND);}
            p.lastAction=level.getGameTime();boolean stageDone=++p.actions>=stage.actions();if(stageDone){p.stage++;p.actions=0;}
            data.setDirty();stage.feedback().emit(level,net.minecraft.world.phys.Vec3.atCenterOf(pos),player,e.getHand(),item,true);
            if(stageDone){if(stage.feedback().completed()!=null)stage.feedback().completed().emit(level,net.minecraft.world.phys.Vec3.atCenterOf(pos),null,e.getHand(),item,true);else new Feedback().blockImpact(.065,8).sound("minecraft:entity.experience_orb.pickup",.35F,1.5F).particles("minecraft:crit",6,.2,.02).emit(level,net.minecraft.world.phys.Vec3.atCenterOf(pos),null,e.getHand(),item,true);}
        }
        finishIfReady(level,pos,player,d,p);relevant(level,pos);
    }
    private static boolean materialsDone(UpgradeDefinition d, UpgradeData.Progress p) {
        for(int i=0;i<p.supplied.length;i++) if(p.supplied[i]<d.materials().get(i).count()) return false;
        return true;
    }
    private static <T extends Comparable<T>> BlockState copyProperty(BlockState from, BlockState to, Property<T> property) {
        var target=to.getBlock().getStateDefinition().getProperty(property.getName());
        if(target==null) return to;
        return setProperty(to,target,property.getName(from.getValue(property)));
    }
    private static <T extends Comparable<T>> BlockState setProperty(BlockState state, Property<T> p, String name) {
        return p.getValue(name).map(v -> state.setValue(p,v)).orElse(state);
    }
    private static void finishIfReady(ServerLevel level, BlockPos pos, ServerPlayer player, UpgradeDefinition d, UpgradeData.Progress p) {
        if(!materialsDone(d,p))return;
        if(d.itemOutput()) {finishOutputs(level,pos,player,d,p);return;}
        if(p.incomplete) {
            if(d.manualCompletes()&&!d.stages().isEmpty()&&d.accelerators().isEmpty()&&p.stage>=d.stages().size())p.deadline=level.getGameTime();
            if(p.stage>=d.stages().size() && (d.buildTime()==0||p.deadline>=0&&p.deadline<=level.getGameTime())) {
                Downgrades.remember(level,pos,d,p);UpgradeData.get(level).entries.remove(pos.asLong());UpgradeData.get(level).setDirty();incompleteSync(level,pos,"");
                d.completion().emit(level,net.minecraft.world.phys.Vec3.atCenterOf(pos),player,InteractionHand.MAIN_HAND,d.display(),false);
            }
            return;
        }
        var oldState=level.getBlockState(pos); if(!oldState.is(d.source())) return;
        var old=level.getBlockEntity(pos);
        CompoundTag backup=old==null ? new CompoundTag() : old.saveWithoutMetadata(level.registryAccess());
        BlockState targetState=d.target().defaultBlockState();
        if(d.preserve()) for(var property:oldState.getProperties()) targetState=copyProperty(oldState,targetState,property);
        BlockEntity target=d.target() instanceof EntityBlock eb ? eb.newBlockEntity(pos,targetState) : null;
        try {
            if(d.transfer()==null && old!=null) {
                if(!InventorySafety.isEmpty(old))
                    throw new IllegalStateException("Empty the source inventory, or configure an explicit transfer callback for this block entity");
            }
            if(old!=null&&net.neoforged.fml.ModList.get().isLoaded("sophisticatedstorage")
                && dev.jco.upgrades.integration.SophisticatedStorage.isStorage(old)
                && !d.transferMode().equals("sophisticated_storage"))
                throw new IllegalStateException("Sophisticated Storage needs the sophisticated_storage transfer mode");
            if(d.transfer()!=null) d.transfer().accept(new TransferContext(level,pos,player,oldState,targetState,backup.copy(),target));
        } catch(RuntimeException ex) { error(player,ex); return; }
        TRANSFORMING.set(true);
        try {
            p.sourceState=net.minecraft.nbt.NbtUtils.writeBlockState(oldState);
            boolean builtinTransfer=d.transferMode().equals("copy_data")||d.transferMode().equals("copy_inventory")||d.transferMode().equals("sophisticated_storage");
            p.rollbackReady=builtinTransfer||d.transfer()==null&&InventorySafety.isEmpty(old);
            if(old instanceof Container c)c.clearContent();
            else if(d.transferMode().equals("sophisticated_storage"))dev.jco.upgrades.integration.SophisticatedStorage.clear(old);
            p.sourceData=builtinTransfer&&old!=null?old.saveWithoutMetadata(level.registryAccess()):backup.copy();
            if(d.transferMode().equals("sophisticated_storage"))dev.jco.upgrades.integration.SophisticatedStorage.detach(level,pos,old);
            if(!level.setBlock(pos,targetState,3)) throw new IllegalStateException("Block replacement was rejected");
            if(target!=null) { level.setBlockEntity(target);if(d.transferMode().equals("sophisticated_storage"))dev.jco.upgrades.integration.SophisticatedStorage.installed(old,target,targetState);if(!d.resultData().isEmpty()){var merged=target.saveWithoutMetadata(level.registryAccess());merged.merge(d.resultData());target.loadWithComponents(merged,level.registryAccess());}target.setChanged(); level.sendBlockUpdated(pos,targetState,targetState,3); }
            else if(!d.resultData().isEmpty())throw new IllegalStateException("Result data requires a block entity");
            var data=UpgradeData.get(level);p.incomplete=true;p.target=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(d.target()).toString();startTimer(level,pos,d,p);data.setDirty();
            incompleteSync(level,pos,p.target);
            new Feedback().blockImpact(.1,10).sound("minecraft:block.anvil.place",.65F,1.15F).particles("minecraft:crit",14,.4,.06).emit(level,net.minecraft.world.phys.Vec3.atCenterOf(pos),player,InteractionHand.MAIN_HAND,d.display(),false);
            if(d.stages().isEmpty())finishIfReady(level,pos,player,d,p);
        } catch(RuntimeException ex) {
            if(d.transferMode().equals("sophisticated_storage"))dev.jco.upgrades.integration.SophisticatedStorage.failed(old);
            level.setBlock(pos,oldState,3);
            var restored=level.getBlockEntity(pos);
            if(restored==null&&oldState.getBlock() instanceof EntityBlock oldEntity){restored=oldEntity.newBlockEntity(pos,oldState);if(restored!=null)level.setBlockEntity(restored);}
            if(restored!=null) { restored.loadWithComponents(backup,level.registryAccess());if(net.neoforged.fml.ModList.get().isLoaded("sophisticatedstorage"))dev.jco.upgrades.integration.SophisticatedStorage.restoreWoodFromSnapshot(restored,backup); restored.setChanged(); }
            error(player,ex);
        } finally { TRANSFORMING.remove(); }
    }
    private static void finishOutputs(ServerLevel level,BlockPos pos,ServerPlayer player,UpgradeDefinition d,UpgradeData.Progress p){
        var data=UpgradeData.get(level);p.incomplete=true;p.target=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(d.source()).toString();startTimer(level,pos,d,p);data.setDirty();
        if(p.committing||p.completed||p.stage<d.stages().size()||d.buildTime()>0&&p.deadline>level.getGameTime())return;
        p.committing=true;
        try {
            if(!p.prepared){
                var ctx=new CompletionContext(level,pos,player,(player==null?ItemStack.EMPTY:player.getMainHandItem()));
                d.outputs().forEach(ctx::drop);if(d.removesBlock())ctx.removeBlock();
                if(d.completionCallback()!=null)d.completionCallback().accept(ctx);
                if(ctx.removes()&&level.getBlockEntity(pos) instanceof Container c&&!c.isEmpty())throw new IllegalStateException("Empty the source inventory before removing it");
                p.outputs.addAll(ctx.outputs());p.removeOnComplete=ctx.removes();p.prepared=true;p.escrow.clear();data.schedule(pos.asLong(),level.getGameTime()+1);data.setDirty();
            }
            flushOutputPlan(level,pos,p);
            if(p.completed)d.completion().emit(level,net.minecraft.world.phys.Vec3.atCenterOf(pos),player,InteractionHand.MAIN_HAND,d.display(),false);
        }catch(RuntimeException ex){error(player,ex);}finally{p.committing=false;}
    }
    public static void flushOutputPlan(ServerLevel level,BlockPos pos,UpgradeData.Progress p){
        var data=UpgradeData.get(level);
        if(!p.prepared||p.completed)return;
        if(p.removeOnComplete&&!level.getBlockState(pos).isAir()){
            TRANSFORMING.set(true);try{if(!level.setBlock(pos,Blocks.AIR.defaultBlockState(),3))return;}finally{TRANSFORMING.remove();}
        }
        while(!p.outputs.isEmpty()){
            var stack=p.outputs.getFirst();var entity=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,stack.copy());entity.setDefaultPickUpDelay();entity.setDeltaMovement((level.random.nextDouble()-.5)*.12,.18,(level.random.nextDouble()-.5)*.12);
            if(!level.addFreshEntity(entity))return;
            p.outputs.removeFirst();data.setDirty();
        }
        p.completed=true;p.incomplete=false;data.setDirty();incompleteSync(level,pos,"");
        if(p.removeOnComplete){data.entries.remove(pos.asLong());data.history.remove(pos.asLong());}
    }
    private static void startTimer(ServerLevel level,BlockPos pos,UpgradeDefinition d,UpgradeData.Progress p){if(d.buildTime()>0&&p.deadline<0){p.deadline=level.getGameTime()+d.buildTime();UpgradeData.get(level).schedule(pos.asLong(),p.deadline);}}
    private static boolean accelerate(ServerLevel level,BlockPos pos,ServerPlayer player,UpgradeDefinition d,UpgradeData.Progress p,ItemStack stack){
        if(p.deadline<=level.getGameTime())return false;
        for(int i=0;i<d.accelerators().size();i++){var a=d.accelerators().get(i);if(!a.input().accepts(actionInput.get())||!a.item.matches(stack)||i>=p.acceleratorCounts.length||p.acceleratorCounts[i]<0||!elapsed(level.getGameTime(),p.lastAction,a.cooldown())||stack.getCount()<a.consume()||a.damage()>0&&(!stack.isDamageableItem()||stack.getMaxDamage()-stack.getDamageValue()<a.damage()))continue;
            var display=stack.copyWithCount(1);if(!player.isCreative()){if(a.consume()>0&&!d.itemOutput())p.escrow.add(stack.copyWithCount(a.consume()));stack.shrink(a.consume());if(a.damage()>0&&!stack.isEmpty())stack.hurtAndBreak(a.damage(),player,EquipmentSlot.MAINHAND);}
            p.lastAction=level.getGameTime();int before=p.acceleratorCounts[i];boolean milestone=++p.acceleratorCounts[i]>=a.actions;
            long reduction=a.grouped()?(milestone?a.reduction:0):((long)a.reduction*(before+1)/a.actions-(long)a.reduction*before/a.actions);
            p.deadline=Math.max(level.getGameTime(),p.deadline-reduction);
            if(milestone)p.acceleratorCounts[i]=a.repeatable()?0:-1;
            boolean allDone=!d.accelerators().isEmpty();for(int j=0;j<d.accelerators().size();j++)if(p.acceleratorCounts[j]>=0)allDone=false;
            if(d.manualCompletes()&&allDone)p.deadline=level.getGameTime();
            UpgradeData.get(level).schedule(pos.asLong(),p.deadline);
            UpgradeData.get(level).setDirty();a.feedback().emit(level,net.minecraft.world.phys.Vec3.atCenterOf(pos),player,InteractionHand.MAIN_HAND,display,true);incompleteSync(level,pos,p.target);return true;
        }return false;
    }
    private static boolean elapsed(long now,long last,int cooldown){return last==Long.MIN_VALUE||now>last&&now-last>=cooldown;}
    public static void resumeOutputs(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level))return;var data=UpgradeData.get(level);long now=level.getGameTime();int budget=256;
        while(budget-->0&&!data.deadlines.isEmpty()&&data.deadlines.peek().time()<=now){var due=data.deadlines.poll();var p=data.entries.get(due.pos());if(p==null||p.completed||p.committing)continue;
            var pos=BlockPos.of(due.pos());if(!level.hasChunkAt(pos)){data.schedule(due.pos(),now+20);continue;}
            if(p.prepared){p.committing=true;try{flushOutputPlan(level,pos,p);}finally{p.committing=false;}if(!p.completed)data.schedule(due.pos(),now+20);continue;}
            var d=Upgrades.definitions().get(p.id);if(d==null||!p.fingerprint.equals(d.fingerprint()))continue;
            if(p.incomplete&&p.deadline>=0&&p.deadline<=now&&p.stage>=d.stages().size()){finishIfReady(level,pos,null,d,p);relevant(level,pos);}
        }
    }
    private static void error(ServerPlayer player, RuntimeException e) {
        LogUtils.getLogger().error("JCO upgrade failed",e);
        if(player!=null)player.displayClientMessage(Component.literal("Upgrade paused: "+e.getMessage()),true);
    }
    private static final Map<java.util.UUID,LinkedHashMap<net.minecraft.core.GlobalPos,Long>> WATCHED=new HashMap<>();
    public static void clearWatching() {WATCHED.clear();SELECTION.clear();}
    private static void watch(ServerPlayer player,BlockPos pos) {
        var map=WATCHED.computeIfAbsent(player.getUUID(),id->new LinkedHashMap<>());
        map.put(net.minecraft.core.GlobalPos.of(player.serverLevel().dimension(),pos.immutable()),player.serverLevel().getGameTime()+200);
        while(map.size()>16) map.remove(map.keySet().iterator().next());
    }
    private static void relevant(ServerLevel level,BlockPos pos) {
        for(var player:level.players()) if(player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))<=256) {watch(player,pos);snapshot(player,pos);}
    }
    public record Requirement(ItemStack display,int remaining) {}
    public static Requirement requirement(UpgradeDefinition d,UpgradeData.Progress p) {
        if(!p.fingerprint.equals(d.fingerprint()) || p.supplied.length!=d.materials().size()) return new Requirement(ItemStack.EMPTY,0);
        for(int i=0;i<d.materials().size();i++) {var m=d.materials().get(i);if(p.supplied[i]<m.count()) return new Requirement(m.feedback().display(representative(m.item())),m.count()-p.supplied[i]);}
        if(p.stage<d.stages().size()) {var s=d.stages().get(p.stage);return new Requirement(s.feedback().display(representative(s.item())),s.actions()-p.actions);}
        return new Requirement(d.display(),0);
    }
    public static ItemStack representative(StackMatcher matcher) {
        var registry=net.minecraft.core.registries.BuiltInRegistries.ITEM;
        if(!matcher.id().startsWith("#"))return registry.get(net.minecraft.resources.ResourceLocation.parse(matcher.id())).getDefaultInstance();
        return registry.getTag(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.ResourceLocation.parse(matcher.id().substring(1))))
            .flatMap(tag->tag.stream().findFirst()).map(holder->holder.value().getDefaultInstance()).orElse(ItemStack.EMPTY);
    }
    /** A tag is a tool class, not the arbitrary first item in registry order. */
    public static String label(StackMatcher matcher,ItemStack display) {
        if(!matcher.id().startsWith("#"))return display.getHoverName().getString();
        String path=matcher.id().substring(1);
        path=path.substring(Math.max(path.lastIndexOf('/'),path.lastIndexOf(':'))+1);
        return switch(path) {
            case "pickaxes" -> "Pickaxe";
            case "axes" -> "Axe";
            case "shovels" -> "Shovel";
            case "hoes" -> "Hoe";
            case "hammers" -> "Hammer";
            case "logs" -> "Log";
            default -> display.isEmpty()?path.replace('_',' '):display.getHoverName().getString();
        };
    }
    public static void tick(PlayerTickEvent.Post e) {
        if(!(e.getEntity() instanceof ServerPlayer player) || player.tickCount%5!=0) return;
        if(player.isSpectator()) return;
        var hit=player.pick(player.blockInteractionRange(),1F,false);
        var level=player.serverLevel();
        if(hit instanceof BlockHitResult b && hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK && (selected(player,b.getBlockPos())!=null||!Downgrades.valid(level,b.getBlockPos()).isEmpty())) watch(player,b.getBlockPos());
        var watched=WATCHED.get(player.getUUID());if(watched==null)return;
        watched.entrySet().removeIf(entry->entry.getValue()<level.getGameTime()||!entry.getKey().dimension().equals(level.dimension())||player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(entry.getKey().pos()))>256);
        for(var entry:watched.entrySet()) if(level.hasChunkAt(entry.getKey().pos())) snapshot(player,entry.getKey().pos());
    }
    private static void snapshot(ServerPlayer player,BlockPos pos) {
        var level=player.serverLevel();var d=selected(player,pos);
        if(d==null) {var reverse=Downgrades.view(level,pos);var n=new CompoundTag();if(!reverse.isEmpty()){n.put("reverse",reverse);n.putDouble("range",6);n.putString("block",net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString());n.putBoolean("reverseOnly",true);}PacketDistributor.sendToPlayer(player,new UpgradePayload(level.dimension().location().toString(),pos,reverse.isEmpty()?List.of():List.of("Downgrade"),ItemStack.EMPTY,0,n));return;}
        var p=progress(level,pos,d,false); if(p==null) p=new UpgradeData.Progress(d);
        if(!p.fingerprint.equals(d.fingerprint()) || p.supplied.length!=d.materials().size()) {
            PacketDistributor.sendToPlayer(player,new UpgradePayload(level.dimension().location().toString(),pos,List.of("Upgrade paused: definition changed"),ItemStack.EMPTY,0));
            return;
        }
        var lines=new ArrayList<String>(); lines.add("Upgrade: "+d.title());
        for(int i=0;i<d.materials().size();i++) { var m=d.materials().get(i); lines.add(m.item().id()+": "+p.supplied[i]+" / "+m.count()); }
        if(!materialsDone(d,p)) lines.add("Stage: MATERIAL — supply materials");
        else if(p.stage<d.stages().size()) {
            var s=d.stages().get(p.stage); lines.add("Stage "+(p.stage+1)+" / "+d.stages().size()+": "+s.type());
            lines.add(s.item().id()+" — remaining: "+(s.actions()-p.actions));
        } else lines.add("Ready — Shift + right-click to finish");
        lines.add("Shift + right-click to contribute");
        var requirement=requirement(d,p);
        PacketDistributor.sendToPlayer(player,new UpgradePayload(level.dimension().location().toString(),pos,lines,requirement.display(),requirement.remaining(),view(level,pos,d,p)));
    }
    private static CompoundTag view(ServerLevel level,BlockPos pos,UpgradeDefinition d,UpgradeData.Progress p){
        var n=new CompoundTag();var reverse=Downgrades.view(level,pos);if(!reverse.isEmpty())n.put("reverse",reverse);n.putBoolean("blockUpgrade",true);n.putBoolean("compact",d.materials().isEmpty());n.putBoolean("canCancel",!p.prepared&&!p.completed&&(!p.incomplete||p.rollbackReady));n.putString("block",net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString());n.putString("id",d.id);n.putString("title",d.title());n.putString("description",d.description());n.putDouble("range",d.hudRange());n.put("icon",preview(level,pos,d).saveOptional(level.registryAccess()));
        var all=routes(level.getBlockState(pos));n.putInt("routes",all.size());n.putInt("index",all.indexOf(d));n.putBoolean("locked",UpgradeData.get(level).entries.containsKey(pos.asLong()));n.putBoolean("incomplete",p.incomplete||d.itemOutput()&&d.materials().isEmpty());n.putBoolean("completed",p.completed);
        var rows=new net.minecraft.nbt.ListTag();
        for(int i=0;i<d.materials().size();i++){var m=d.materials().get(i);var row=new CompoundTag();var display=m.feedback().display(representative(m.item()));row.put("icon",display.saveOptional(level.registryAccess()));row.putString("label",label(m.item(),display));row.putString("input","RIGHT");row.putBoolean("material",true);row.putInt("have",p.supplied[i]);row.putInt("need",m.count());rows.add(row);}
        if(materialsDone(d,p)&&p.stage<d.stages().size()){var action=d.stages().get(p.stage);var row=new CompoundTag();var display=action.feedback().display(representative(action.item()));row.put("icon",display.saveOptional(level.registryAccess()));row.putString("label",label(action.item(),display));row.putString("input",action.feedback().input().name());row.putInt("have",p.actions);row.putInt("need",action.actions());rows.add(row);}
        if(p.incomplete&&d.buildTime()>0){n.putLong("remaining",Math.max(0,p.deadline-level.getGameTime()));n.putInt("duration",d.buildTime());for(int i=0;i<d.accelerators().size();i++){var a=d.accelerators().get(i);if(i>=p.acceleratorCounts.length||p.acceleratorCounts[i]<0)continue;var row=new CompoundTag();var display=a.feedback().display(representative(a.item));row.put("icon",display.saveOptional(level.registryAccess()));row.putString("label",label(a.item,display));row.putString("input",a.input().name());row.putInt("have",p.acceleratorCounts[i]);row.putInt("need",a.actions);row.putInt("reduction",a.reduction);rows.add(row);}}
        n.put("rows",rows);var neighbors=new net.minecraft.nbt.ListTag();for(var route:all){var c=new CompoundTag();c.putString("title",route.title());c.put("icon",preview(level,pos,route).saveOptional(level.registryAccess()));neighbors.add(c);}n.put("carousel",neighbors);return n;
    }
    private static ItemStack preview(ServerLevel level,BlockPos pos,UpgradeDefinition definition){
        var stack=definition.display();
        if(net.neoforged.fml.ModList.get().isLoaded("sophisticatedstorage"))return dev.jco.upgrades.integration.SophisticatedStorage.preview(level,pos,stack,definition.resultData());
        return stack;
    }
}
