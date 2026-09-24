package dev.jco.upgrades;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class UpgradeCommands {
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("jco_upgrades").requires(s->s.hasPermission(2))
            .then(Commands.literal("status").executes(c->{
                var data=UpgradeData.get(c.getSource().getLevel());
                c.getSource().sendSuccess(()->Component.literal("Definitions: "+Upgrades.definitions().keySet()+"; active upgrades in dimension: "+data.entries.size()),false); return data.entries.size();
            }))
            .then(Commands.literal("cancel").executes(c->{
                var p=c.getSource().getPlayerOrException(); var hit=p.pick(p.blockInteractionRange(),1,false);
                if(!(hit instanceof BlockHitResult b) || hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK) return 0;
                if(!UpgradeRuntime.cancel(p,b.getBlockPos()))return 0;
                c.getSource().sendSuccess(()->Component.literal("Upgrade canceled; deposited materials returned"),false); return 1;
            })));
    }
}
