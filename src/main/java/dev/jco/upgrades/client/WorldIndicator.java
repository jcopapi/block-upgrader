package dev.jco.upgrades.client;

import dev.jco.upgrades.UpgradePayload;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid="jco_upgrades",value=Dist.CLIENT)
public final class WorldIndicator {
    private record Entry(UpgradePayload packet,long time) {}
    private static final Map<BlockPos,Entry> entries=new LinkedHashMap<>();
    public static void accept(UpgradePayload p) {
        if(p.lines().isEmpty()) {entries.remove(p.pos());return;}
        entries.put(p.pos(),new Entry(p,System.nanoTime()));
        while(entries.size()>32)entries.remove(entries.keySet().iterator().next());
    }
    public static double age(BlockPos pos){var e=entries.get(pos);return e==null?0:(System.nanoTime()-e.time)/1E9;}
    public static UpgradePayload lookup(BlockPos pos) {
        var e=entries.get(pos);var level=Minecraft.getInstance().level;
        return e!=null && level!=null && e.packet.dimension().equals(level.dimension().location().toString()) && System.nanoTime()-e.time<750_000_000L?e.packet:null;
    }
    @SubscribeEvent public static void register(RegisterGuiLayersEvent e) {
        e.registerAboveAll(ResourceLocation.parse("jco_upgrades:world_indicators"),(gui,delta)->{
            var level=Minecraft.getInstance().level;
            if(level==null){entries.clear();return;}
            entries.values().removeIf(v->System.nanoTime()-v.time>750_000_000L||!v.packet.dimension().equals(level.dimension().location().toString()));
            var mc=Minecraft.getInstance();if(mc.player==null||mc.screen!=null||!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)||hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK)return;
            var p=lookup(hit.getBlockPos());if(p==null||!p.view().getBoolean("compact")||p.view().getBoolean("completed")||mc.player.position().distanceTo(Vec3.atCenterOf(p.pos()))>p.view().getDouble("range"))return;
            var data=p.view().copy();if(data.contains("remaining"))data.putLong("remaining",Math.max(0,data.getLong("remaining")-(long)(age(p.pos())*20)));
            WorldLabels.draw(gui,Vec3.atCenterOf(p.pos()).add(0,.8,0),data);
        });
    }
}
