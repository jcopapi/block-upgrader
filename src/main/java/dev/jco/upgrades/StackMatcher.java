package dev.jco.upgrades;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

public record StackMatcher(String id) {
    public StackMatcher {
        ResourceLocation.parse(id.startsWith("#") ? id.substring(1) : id);
        if (!id.startsWith("#") && !BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id)))
            throw new IllegalArgumentException("Unknown item: " + id);
    }
    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && (id.startsWith("#")
            ? stack.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(id.substring(1))))
            : stack.is(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))));
    }
}
