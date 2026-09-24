package dev.jco.upgrades;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

public final class UpgradeData extends SavedData {
    public static final class Progress {
        public String id, fingerprint;
        public int[] supplied;
        public int stage, actions;
        public long deadline=-1;public int[] acceleratorCounts=new int[0];
        public boolean incomplete, prepared, completed, removeOnComplete;
        public boolean rollbackReady;public CompoundTag sourceState=new CompoundTag(),sourceData=new CompoundTag();
        public transient boolean committing;
        public final List<ItemStack> outputs=new ArrayList<>();
        public String target="";
        public long lastAction = Long.MIN_VALUE;
        public final List<ItemStack> escrow = new ArrayList<>();
        public Progress(UpgradeDefinition d) { id=d.id; fingerprint=d.fingerprint(); supplied=new int[d.materials().size()];acceleratorCounts=new int[d.accelerators().size()]; }
        private Progress() {}
    }
    public record Due(long pos,long time) implements Comparable<Due>{public int compareTo(Due other){return Long.compare(time,other.time);}}
    public final PriorityQueue<Due> deadlines=new PriorityQueue<>();
    private boolean indexed;
    public void schedule(long pos,long time){deadlines.add(new Due(pos,time));}
    public final Map<Long,CompoundTag> history=new HashMap<>();
    public final Map<Long, Progress> entries = new HashMap<>();
    public static UpgradeData get(ServerLevel level) {
        var data=level.getDataStorage().computeIfAbsent(new Factory<>(UpgradeData::new, UpgradeData::load), "jco_upgrades");if(!data.indexed){data.indexed=true;data.entries.forEach((pos,p)->{if(p.deadline>=0||p.prepared&&!p.completed)data.schedule(pos,p.prepared?level.getGameTime():p.deadline);});}return data;
    }
    public static UpgradeData load(CompoundTag tag, HolderLookup.Provider lookup) {
        var data = new UpgradeData();
        for(var raw:tag.getList("history",10)){var n=(CompoundTag)raw;data.history.put(n.getLong("pos"),n.getCompound("value"));}
        for (var raw : tag.getList("entries", Tag.TAG_COMPOUND)) {
            var n = (CompoundTag) raw;
            var p = new Progress(); p.id=n.getString("id"); p.fingerprint=n.getString("fingerprint");
            p.incomplete=n.getBoolean("incomplete");p.target=n.getString("target");p.lastAction=n.contains("lastAction")?n.getLong("lastAction"):Long.MIN_VALUE;
            p.deadline=n.contains("deadline")?n.getLong("deadline"):-1;p.acceleratorCounts=n.getIntArray("accelerators");
            p.rollbackReady=n.getBoolean("rollbackReady");p.sourceState=n.getCompound("sourceState");p.sourceData=n.getCompound("sourceData");
            p.prepared=n.getBoolean("prepared");p.completed=n.getBoolean("completed");p.removeOnComplete=n.getBoolean("removeOnComplete");
            for(var item:n.getList("outputs",Tag.TAG_COMPOUND))p.outputs.add(ItemStack.parseOptional(lookup,(CompoundTag)item));
            p.supplied=n.getIntArray("supplied"); p.stage=Math.max(0,n.getInt("stage")); p.actions=Math.max(0,n.getInt("actions"));
            for (var item : n.getList("escrow", Tag.TAG_COMPOUND)) p.escrow.add(ItemStack.parseOptional(lookup, (CompoundTag)item));
            data.entries.put(n.getLong("pos"), p);
        }
        return data;
    }
    private void dataHistory(ListTag list){history.forEach((pos,value)->{var n=new CompoundTag();n.putLong("pos",pos);n.put("value",value);list.add(n);});}
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
        var list=new ListTag();
        entries.forEach((pos,p) -> {
            var n=new CompoundTag(); n.putLong("pos",pos); n.putString("id",p.id); n.putString("fingerprint",p.fingerprint);
            n.putBoolean("incomplete",p.incomplete);n.putString("target",p.target);n.putLong("lastAction",p.lastAction);n.putIntArray("supplied",p.supplied); n.putInt("stage",p.stage); n.putInt("actions",p.actions);
            n.putLong("deadline",p.deadline);n.putIntArray("accelerators",p.acceleratorCounts);n.putBoolean("prepared",p.prepared);n.putBoolean("completed",p.completed);n.putBoolean("removeOnComplete",p.removeOnComplete);
            n.putBoolean("rollbackReady",p.rollbackReady);n.put("sourceState",p.sourceState);n.put("sourceData",p.sourceData);
            var out=new ListTag();p.outputs.forEach(i->{if(!i.isEmpty())out.add(i.save(lookup));});n.put("outputs",out);
            var items=new ListTag(); p.escrow.forEach(i -> { if (!i.isEmpty()) items.add(i.save(lookup)); }); n.put("escrow",items); list.add(n);
        });
        tag.put("entries",list);var histories=new ListTag();dataHistory(histories);tag.put("history",histories); return tag;
    }
}
