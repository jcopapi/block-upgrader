package dev.jco.upgrades;

import java.util.Comparator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Descriptive data only; clients never execute recipe rules from this catalog. */
public final class RecipeCatalog {
    private RecipeCatalog() {}

    public static CompoundTag snapshot(ServerPlayer player) {
        var root = new CompoundTag();
        var recipes = new ListTag();
        Upgrades.definitions().values().stream().sorted(Comparator.comparing(d -> d.id)).limit(512).forEach(d -> {
            var entry = new CompoundTag();
            entry.putString("id", d.id);
            entry.putString("title", d.title());
            entry.putString("description", d.description());
            entry.putString("source", BuiltInRegistries.BLOCK.getKey(d.source()).toString());
            entry.putBoolean("placedIncomplete", d.placedIncomplete());
            if (d.target() != null) entry.putString("result", BuiltInRegistries.BLOCK.getKey(d.target()).toString());
            entry.putInt("buildTime", d.buildTime());
            entry.putString("transfer", d.transferMode());
            entry.putBoolean("instant", d.buildTime() == 0 && d.stages().isEmpty());
            entry.put("resultData", d.resultData());
            var outputs = new ListTag();
            for (ItemStack stack : d.outputs()) outputs.add(stack.save(player.registryAccess()));
            entry.put("outputs", outputs);
            var materials = new ListTag();
            for (var material : d.materials()) {
                var row = new CompoundTag();
                row.putString("item", material.item().id());
                row.putInt("count", material.count());
                row.put("icon", material.feedback().display(UpgradeRuntime.representative(material.item())).saveOptional(player.registryAccess()));
                materials.add(row);
            }
            entry.put("materials", materials);
            var work = new ListTag();
            for (var stage : d.stages()) {
                var row = new CompoundTag();
                row.putString("item", stage.item().id());
                row.putString("input", stage.feedback().input().name());
                row.putInt("actions", stage.actions());
                row.putString("kind", "stage");
                row.put("icon", stage.feedback().display(UpgradeRuntime.representative(stage.item())).saveOptional(player.registryAccess()));
                work.add(row);
            }
            for (var accelerator : d.accelerators()) {
                var row = new CompoundTag();
                row.putString("item", accelerator.item.id());
                row.putString("input", accelerator.input().name());
                row.putInt("actions", accelerator.actions);
                row.putString("kind", "accelerator");
                row.put("icon", accelerator.feedback().display(UpgradeRuntime.representative(accelerator.item)).saveOptional(player.registryAccess()));
                work.add(row);
            }
            entry.put("work", work);
            recipes.add(entry);
        });
        root.put("recipes", recipes);
        return root;
    }

    public static void send(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new RecipeCatalogPayload(snapshot(player)));
    }

    public static void broadcastLater() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) server.execute(() -> server.getPlayerList().getPlayers().forEach(RecipeCatalog::send));
    }
}
