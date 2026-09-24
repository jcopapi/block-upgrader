package dev.jco.upgrades;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

/** Server datapack definitions in data/<namespace>/jco_block_upgrades/*.json. */
public final class UpgradeJsonLoader extends SimpleJsonResourceReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    public UpgradeJsonLoader() { super(new Gson(), "jco_block_upgrades"); }

    @Override protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resources, ProfilerFiller profiler) {
        var definitions = new LinkedHashMap<String, UpgradeDefinition>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try {
                UpgradeDefinition definition = parse(entry.getKey(), entry.getValue());
                if (definition != null) definitions.put(definition.id, definition);
            } catch (RuntimeException exception) {
                LOGGER.error("Invalid block upgrade {}: {}", entry.getKey(), exception.getMessage(), exception);
            }
        });
        Upgrades.replaceDatapack(definitions);
        LOGGER.info("Loaded {} JCO block upgrades from datapacks", definitions.size());
    }

    /** Returns null for a disabled definition or an absent optional mod. */
    public static UpgradeDefinition parse(ResourceLocation id, JsonElement json) {
        if (!json.isJsonObject()) throw new IllegalArgumentException("Expected JSON object");
        JsonObject root = json.getAsJsonObject();
        if (!bool(root, "enabled", true)) return null;
        for (JsonElement mod : array(root, "required_mods")) if (!ModList.get().isLoaded(mod.getAsString())) return null;

        var definition = new UpgradeDefinition(id.toString());
        definition.block(string(root, "source"));
        if (root.has("result")) definition.result(string(root, "result"));
        if (root.has("placed_incomplete")) definition.placedIncomplete(bool(root, "placed_incomplete", false));
        for (JsonElement output : array(root, "outputs")) {
            JsonObject entry = object(output, "output");
            var key = ResourceLocation.parse(string(entry, "item"));
            if (!BuiltInRegistries.ITEM.containsKey(key)) throw new IllegalArgumentException("Unknown output item: " + key);
            definition.output(new ItemStack(BuiltInRegistries.ITEM.get(key), integer(entry, "count", 1)));
        }
        if (root.has("title")) definition.title(string(root, "title"));
        if (root.has("description")) definition.description(string(root, "description"));
        if (root.has("display_item")) definition.displayItem(stack(string(root, "display_item")));
        if (root.has("hud_range")) definition.hudRange(number(root, "hud_range", 6));
        if (root.has("preserve_properties")) definition.preserveProperties(bool(root, "preserve_properties", true));
        if (root.has("remove_block_on_complete")) definition.removeBlockOnComplete(bool(root, "remove_block_on_complete", false));
        if (root.has("build_time")) definition.buildTime(integer(root, "build_time", 0));
        if (root.has("manual_completes")) definition.manualCompletes(bool(root, "manual_completes", false));
        if (root.has("transfer")) definition.transferMode(string(root, "transfer"));
        if (root.has("result_data")) definition.resultData(object(root.get("result_data"), "result_data").toString());
        if (root.has("downgrade")) {
            JsonObject undo = object(root.get("downgrade"), "downgrade");
            definition.downgrade(string(undo, "tool"), integer(undo, "hits", 3));
        }
        for (JsonElement material : array(root, "materials")) {
            JsonObject entry = object(material, "material");
            definition.material(string(entry, "item"), integer(entry, "count", 1), feedback -> configure(feedback, entry));
        }
        for (JsonElement stage : array(root, "stages")) {
            JsonObject entry = object(stage, "stage");
            String type = string(entry, "type");
            definition.stage(type, string(entry, "item"), integer(entry, "actions", 1),
                integer(entry, "consume", type.equals("ITEM_APPLICATION") ? 1 : 0),
                integer(entry, "damage", type.equals("TOOL_ACTION") ? 1 : 0),
                feedback -> configure(feedback, entry));
        }
        for (JsonElement accelerator : array(root, "accelerators")) {
            JsonObject entry = object(accelerator, "accelerator");
            String kind = string(entry, "kind");
            java.util.function.Consumer<Accelerator> settings = acceleration -> {
                if (entry.has("input")) acceleration.input(string(entry, "input"));
                if (entry.has("damage")) acceleration.durability(integer(entry, "damage", 0));
                if (entry.has("consume")) acceleration.consume(integer(entry, "consume", 0));
                if (entry.has("cooldown")) acceleration.actionCooldown(integer(entry, "cooldown", 10));
                if (entry.has("repeatable")) acceleration.repeatable(bool(entry, "repeatable", false));
                if (entry.has("grouped")) acceleration.grouped(bool(entry, "grouped", false));
                acceleration.feedback(feedback -> configure(feedback, entry));
            };
            if (kind.equals("tool")) definition.acceleratorTool(string(entry, "item"), integer(entry, "actions", 1), integer(entry, "reduction", 1), settings);
            else if (kind.equals("item")) definition.acceleratorItem(string(entry, "item"), integer(entry, "actions", 1), integer(entry, "reduction", 1), settings);
            else throw new IllegalArgumentException("Unknown accelerator kind: " + kind);
        }
        if (root.has("completion_feedback")) definition.completionFeedback(feedback -> configure(feedback, object(root.get("completion_feedback"), "completion_feedback")));
        definition.freeze();
        return definition;
    }

    private static void configure(Feedback feedback, JsonObject entry) {
        if (entry.has("input")) feedback.input(string(entry, "input"));
        if (entry.has("cooldown")) feedback.actionCooldown(integer(entry, "cooldown", 10));
        if (entry.has("display_item")) feedback.displayItem(stack(string(entry, "display_item")));
        if (entry.has("sound")) {
            JsonObject sound = object(entry.get("sound"), "sound");
            feedback.sound(string(sound, "id"), (float) number(sound, "volume", .6), (float) number(sound, "pitch", 1));
        }
        if (entry.has("particles")) {
            JsonObject particles = object(entry.get("particles"), "particles");
            feedback.particles(string(particles, "id"), integer(particles, "count", 5), number(particles, "spread", .18), number(particles, "speed", .03));
        }
        if (entry.has("impact")) {
            JsonObject impact = object(entry.get("impact"), "impact");
            feedback.blockImpact(number(impact, "amplitude", .045), integer(impact, "ticks", 6));
        }
        if (entry.has("completed")) feedback.completed(done -> configure(done, object(entry.get("completed"), "completed")));
    }

    private static ItemStack stack(String id) {
        var key = ResourceLocation.parse(id);
        if (!BuiltInRegistries.ITEM.containsKey(key)) throw new IllegalArgumentException("Unknown display item: " + key);
        return BuiltInRegistries.ITEM.get(key).getDefaultInstance();
    }
    private static JsonObject object(JsonElement value, String name) {
        if (value == null || !value.isJsonObject()) throw new IllegalArgumentException("Expected object: " + name);
        return value.getAsJsonObject();
    }
    private static JsonArray array(JsonObject object, String name) {
        if (!object.has(name)) return new JsonArray();
        JsonElement value = object.get(name);
        if (!value.isJsonArray()) throw new IllegalArgumentException("Expected array: " + name);
        return value.getAsJsonArray();
    }
    private static String string(JsonObject object, String name) {
        if (!object.has(name) || !object.get(name).isJsonPrimitive()) throw new IllegalArgumentException("Expected string: " + name);
        return object.get(name).getAsString();
    }
    private static int integer(JsonObject object, String name, int fallback) {
        return object.has(name) ? object.get(name).getAsInt() : fallback;
    }
    private static double number(JsonObject object, String name, double fallback) {
        return object.has(name) ? object.get(name).getAsDouble() : fallback;
    }
    private static boolean bool(JsonObject object, String name, boolean fallback) {
        return object.has(name) ? object.get(name).getAsBoolean() : fallback;
    }
}
