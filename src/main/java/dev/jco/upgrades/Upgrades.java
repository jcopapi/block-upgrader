package dev.jco.upgrades;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class Upgrades {
    public record Action(ServerPlayer player, ServerLevel level, BlockPos pos, ItemStack stack, UpgradeDefinition.Stage stage) {}
    public static final Map<String, Predicate<Action>> STAGE_TYPES = new LinkedHashMap<>();
    private static volatile Map<String, UpgradeDefinition> definitions = Map.of();
    private static Map<String, UpgradeDefinition> datapack = Map.of(), scripts = Map.of();
    private static Set<String> scriptDisabled = Set.of();
    private static final Map<net.minecraft.world.level.block.Block,List<UpgradeDefinition>> routes=new HashMap<>();
    public static List<UpgradeDefinition> routes(net.minecraft.world.level.block.state.BlockState state){return routes.computeIfAbsent(state.getBlock(), block->definitions.values().stream().filter(d->state.is(d.source())).toList());}
    static {
        STAGE_TYPES.put("TOOL_ACTION", action -> true);
        STAGE_TYPES.put("ITEM_APPLICATION", action -> true);
    }
    /** Register extensions during mod initialization, before scripts define stages. */
    public static void registerStageType(String id, Predicate<Action> validator) {
        if (STAGE_TYPES.putIfAbsent(id, Objects.requireNonNull(validator)) != null) throw new IllegalArgumentException("Duplicate stage type");
    }
    public static Map<String, UpgradeDefinition> definitions() { return definitions; }
    public static void replace(Map<String, UpgradeDefinition> values) { definitions=Collections.unmodifiableMap(new LinkedHashMap<>(values));routes.clear(); }
    private static void rebuild() {
        var merged = new LinkedHashMap<>(scripts);
        datapack.forEach((id,definition)->{if(!scriptDisabled.contains(id))merged.putIfAbsent(id,definition);});
        replace(merged);
    }
    /** Datapack IDs are the baseline; server scripts with the same ID override them. */
    public static synchronized void replaceDatapack(Map<String, UpgradeDefinition> values) {datapack=Collections.unmodifiableMap(new LinkedHashMap<>(values));rebuild();RecipeCatalog.broadcastLater();}
    public static synchronized void replaceScripts(Map<String, UpgradeDefinition> values,Set<String> disabled) {scripts=Collections.unmodifiableMap(new LinkedHashMap<>(values));scriptDisabled=Set.copyOf(disabled);rebuild();RecipeCatalog.broadcastLater();}
    public static synchronized void clearSources() {datapack=Map.of();scripts=Map.of();scriptDisabled=Set.of();replace(Map.of());}
}
