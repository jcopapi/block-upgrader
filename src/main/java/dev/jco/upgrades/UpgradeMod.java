package dev.jco.upgrades;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@Mod("jco_upgrades")
public final class UpgradeMod {
    public UpgradeMod(IEventBus bus, net.neoforged.fml.ModContainer container) {
        UpgradeClientConfig.register(container);
        bus.addListener((RegisterPayloadHandlersEvent e) -> e.registrar("4").playToClient(UpgradePayload.TYPE,UpgradePayload.CODEC,
            (p,c) -> c.enqueueWork(() -> dev.jco.upgrades.client.UpgradeOverlay.accept(p))));
        bus.addListener((RegisterPayloadHandlersEvent e) -> e.registrar("1").playToClient(RecipeCatalogPayload.TYPE,RecipeCatalogPayload.CODEC,
            (p,c) -> c.enqueueWork(() -> dev.jco.upgrades.client.RecipeCatalogClient.accept(p))));
        bus.addListener((RegisterPayloadHandlersEvent e)->{
            e.registrar("5").playToServer(WorkPayload.TYPE,WorkPayload.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof net.minecraft.server.level.ServerPlayer player)UpgradeRuntime.work(player,p);}));
            e.registrar("4").playToServer(SelectRoutePayload.TYPE,SelectRoutePayload.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof net.minecraft.server.level.ServerPlayer player)UpgradeRuntime.select(player,p);}));
            e.registrar("4").playToClient(IncompletePayload.TYPE,IncompletePayload.CODEC,(p,c)->c.enqueueWork(()->dev.jco.upgrades.client.IncompleteOverlay.accept(p)));
        });
        bus.addListener((RegisterPayloadHandlersEvent e)->e.registrar("4").playToClient(ImpactPayload.TYPE,ImpactPayload.CODEC,(p,c)->c.enqueueWork(()->dev.jco.upgrades.client.BlockImpact.accept(p))));
        NeoForge.EVENT_BUS.addListener(UpgradeRuntime::chunk);
        NeoForge.EVENT_BUS.addListener(UpgradeRuntime::placed);
        NeoForge.EVENT_BUS.addListener(UpgradeRuntime::resumeOutputs);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,UpgradeRuntime::interact);
        NeoForge.EVENT_BUS.addListener(UpgradeRuntime::tick);
        NeoForge.EVENT_BUS.addListener(Downgrades::drops);
        NeoForge.EVENT_BUS.addListener(Downgrades::placed);
        NeoForge.EVENT_BUS.addListener(UpgradeCommands::register);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) RecipeCatalog.send(player);
        });
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(new UpgradeJsonLoader()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {Upgrades.clearSources();UpgradeRuntime.clearWatching();Downgrades.clear();});
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e)->UpgradeRuntime.clearWatching());
    }
}
