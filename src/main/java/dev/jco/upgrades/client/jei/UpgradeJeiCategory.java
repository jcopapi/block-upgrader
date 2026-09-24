package dev.jco.upgrades.client.jei;

import java.util.Arrays;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/** Only the block transformation and its material stacks are visible in JEI. */
public final class UpgradeJeiCategory implements IRecipeCategory<UpgradeJeiRecipe> {
    public static final RecipeType<UpgradeJeiRecipe> TYPE = RecipeType.create("jco_upgrades", "block_upgrade", UpgradeJeiRecipe.class);
    private static final int WIDTH = 176, HEIGHT = 87;
    private final IDrawable icon;

    public UpgradeJeiCategory(mezz.jei.api.helpers.IGuiHelper gui) {
        icon = gui.createDrawableItemStack(new ItemStack(Items.SMITHING_TABLE));
    }
    @Override public RecipeType<UpgradeJeiRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.literal("Block Upgrader"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }
    @Override public ResourceLocation getRegistryName(UpgradeJeiRecipe recipe) { return recipe.id(); }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, UpgradeJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(28, 8).addItemStack(recipe.source()).setStandardSlotBackground();
        builder.addOutputSlot(130, 8).addItemStack(recipe.result()).setOutputSlotBackground();
        for (int i = 1; i < recipe.outputs().size(); i++)
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(recipe.outputs().get(i));

        int visible = Math.min(recipe.materials().size(), recipe.materials().size() > 12 ? 11 : 12);
        for (int i = 0; i < recipe.materials().size(); i++) {
            var material = recipe.materials().get(i);
            if (i < visible) addMaterial(builder.addInputSlot(4 + (i % 6) * 28, 42 + (i / 6) * 25).setStandardSlotBackground(), material);
            else addMaterial(builder.addInvisibleIngredients(RecipeIngredientRole.INPUT), material);
        }
        for (var action : recipe.work())
            addTool(builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST), action);
    }

    private static void addMaterial(mezz.jei.api.gui.builder.IIngredientAcceptor<?> slot, UpgradeJeiRecipe.Need need) {
        if (need.selector().startsWith("#")) {
            var tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(need.selector().substring(1)));
            slot.addItemStacks(Arrays.stream(Ingredient.of(tag).getItems()).map(stack -> stack.copyWithCount(need.count())).toList());
        } else {
            var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(need.selector()));
            slot.addItemStack(item.getDefaultInstance().copyWithCount(need.count()));
        }
    }

    private static void addTool(mezz.jei.api.gui.builder.IIngredientAcceptor<?> slot, UpgradeJeiRecipe.Need need) {
        if (need.selector().startsWith("#")) {
            var tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(need.selector().substring(1)));
            slot.addIngredients(Ingredient.of(tag));
        } else slot.addItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(need.selector())).getDefaultInstance());
    }

    @Override public void draw(UpgradeJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics gui, double mouseX, double mouseY) {
        gui.drawString(Minecraft.getInstance().font, ">", 86, 13, 0xFF555555, false);
        if (recipe.materials().size() > 12)
            gui.drawString(Minecraft.getInstance().font, "+" + (recipe.materials().size() - 11), 151, 73, 0xFF555555, false);
    }

    @Override public void getTooltip(ITooltipBuilder lines, UpgradeJeiRecipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (mouseX >= 65 && mouseX < 113 && mouseY >= 7 && mouseY < 30) {
            lines.add(Component.literal(recipe.title()));
            if (!recipe.description().isBlank()) lines.add(Component.literal(recipe.description()));
            if (recipe.placedIncomplete()) lines.add(Component.literal("Finish after placement"));
            for (var action : recipe.work())
                lines.add(Component.literal(action.count() + "x " + action.label()));
        } else if (recipe.materials().size() > 12 && mouseX >= 147 && mouseY >= 65) {
            for (int i = 11; i < recipe.materials().size(); i++) {
                var material = recipe.materials().get(i);
                lines.add(Component.literal(material.count() + "x " + material.label()));
            }
        }
    }
}
