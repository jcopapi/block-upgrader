package dev.jco.upgrades.client.jei;

import dev.jco.upgrades.StackMatcher;
import dev.jco.upgrades.UpgradeRuntime;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;

/** Client-only presentation of a server-resolved definition. */
public record UpgradeJeiRecipe(ResourceLocation id, String title, String description,
    ItemStack source, ItemStack result, List<ItemStack> outputs, List<Need> materials,
    List<Need> work, int buildTime, String transfer) {
    public record Need(String selector, int count, ItemStack icon, String input, String kind) {
        public String label() {
            var actual = selector.startsWith("#") ? icon : BuiltInRegistries.ITEM.get(ResourceLocation.parse(selector)).getDefaultInstance();
            return UpgradeRuntime.label(new StackMatcher(selector), actual);
        }
    }

    public static UpgradeJeiRecipe read(CompoundTag tag, RegistryAccess registry) {
        var id = ResourceLocation.parse(tag.getString("id"));
        ItemStack source = blockItem(tag.getString("source"));
        ItemStack result = tag.contains("result") ? blockItem(tag.getString("result")) : ItemStack.EMPTY;
        if (!result.isEmpty() && ModList.get().isLoaded("sophisticatedstorage"))
            result = dev.jco.upgrades.integration.SophisticatedStorage.previewStatic(result, tag.getCompound("resultData"));
        var outputs = new ArrayList<ItemStack>();
        for (var raw : tag.getList("outputs", 10)) {
            var stack = ItemStack.parseOptional(registry, (CompoundTag) raw);
            if (!stack.isEmpty()) outputs.add(stack);
        }
        if (result.isEmpty() && !outputs.isEmpty()) result = outputs.getFirst().copy();
        var materials = readNeeds(tag, "materials", registry);
        var work = readNeeds(tag, "work", registry);
        return new UpgradeJeiRecipe(id, tag.getString("title"), tag.getString("description"), source,
            result, List.copyOf(outputs), List.copyOf(materials), List.copyOf(work), tag.getInt("buildTime"), tag.getString("transfer"));
    }

    private static List<Need> readNeeds(CompoundTag tag, String key, RegistryAccess registry) {
        var result = new ArrayList<Need>();
        for (var raw : tag.getList(key, 10)) {
            var row = (CompoundTag) raw;
            String selector = row.getString("item");
            ItemStack icon = ItemStack.parseOptional(registry, row.getCompound("icon"));
            result.add(new Need(selector, row.getInt(key.equals("work") ? "actions" : "count"),
                icon, row.getString("input"), row.getString("kind")));
        }
        return result;
    }

    private static ItemStack blockItem(String id) {
        var key = ResourceLocation.parse(id);
        if (!BuiltInRegistries.BLOCK.containsKey(key)) return new ItemStack(Items.BARRIER);
        var stack = BuiltInRegistries.BLOCK.get(key).asItem().getDefaultInstance();
        return stack.isEmpty() ? new ItemStack(Items.BARRIER) : stack;
    }
}
