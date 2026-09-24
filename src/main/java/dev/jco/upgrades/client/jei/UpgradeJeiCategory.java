package dev.jco.upgrades.client.jei;

import dev.jco.upgrades.client.InputIcons;
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
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/** Native JEI slots: block to upgraded block, materials, then tool actions. */
public final class UpgradeJeiCategory implements IRecipeCategory<UpgradeJeiRecipe> {
    public static final RecipeType<UpgradeJeiRecipe> TYPE = RecipeType.create("jco_upgrades", "block_upgrade", UpgradeJeiRecipe.class);
    private static final int WIDTH = 176, HEIGHT = 104;
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

        int visibleMaterials = Math.min(recipe.materials().size(), recipe.materials().size() > 6 ? 5 : 6);
        for (int i = 0; i < recipe.materials().size(); i++) {
            var need = recipe.materials().get(i);
            if (i < visibleMaterials) addIngredient(builder.addInputSlot(4 + i * 28, 45).setStandardSlotBackground(), need);
            else addIngredient(builder.addInvisibleIngredients(RecipeIngredientRole.INPUT), need);
        }
        for (int i = 0; i < recipe.work().size(); i++) {
            var action = recipe.work().get(i);
            if (i < 2) {
                var slot = builder.addSlot(RecipeIngredientRole.CATALYST, 38 + i * 82, 77).setStandardSlotBackground();
                addIngredient(slot, action);
                slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.literal(
                    action.count() + "x " + action.input().toLowerCase() + " click" +
                    (action.kind().equals("accelerator") ? " (optional)" : ""))));
            } else addIngredient(builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST), action);
        }
    }

    private static void addIngredient(mezz.jei.api.gui.builder.IIngredientAcceptor<?> slot, UpgradeJeiRecipe.Need need) {
        if (need.selector().startsWith("#")) {
            var tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(need.selector().substring(1)));
            slot.addIngredients(Ingredient.of(tag));
        } else slot.addItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(need.selector())).getDefaultInstance());
    }

    @Override public void draw(UpgradeJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics gui, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        gui.drawString(font, "→", 86, 13, 0xFF777777, false);
        int visibleMaterials = Math.min(recipe.materials().size(), recipe.materials().size() > 6 ? 5 : 6);
        for (int i = 0; i < visibleMaterials; i++)
            gui.drawString(font, Integer.toString(recipe.materials().get(i).count()), 15 + i * 28, 64, 0xFF555555, true);
        if (recipe.materials().size() > visibleMaterials)
            gui.drawString(font, "+" + (recipe.materials().size() - visibleMaterials), 151, 51, 0xFF555555, false);
        for (int i = 0; i < Math.min(2, recipe.work().size()); i++) {
            var action = recipe.work().get(i);
            InputIcons.draw(gui, action.input(), 10 + i * 82, 80);
            gui.drawString(font, "×" + action.count(), 59 + i * 82, 82, 0xFF555555, false);
        }
        if (recipe.work().size() > 2)
            gui.drawString(font, "+" + (recipe.work().size() - 2), 158, 83, 0xFF555555, false);
        if (recipe.buildTime() > 0) gui.renderItem(new ItemStack(Items.CLOCK), 149, 8);
    }

    @Override public void getTooltip(ITooltipBuilder lines, UpgradeJeiRecipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (mouseX >= 65 && mouseX < 113 && mouseY >= 7 && mouseY < 30) {
            lines.add(Component.literal(recipe.title()));
            if (!recipe.description().isBlank()) lines.add(Component.literal(recipe.description()));
            if (recipe.placedIncomplete()) lines.add(Component.literal("Finish after placement"));
        } else if (recipe.buildTime() > 0 && mouseX >= 149 && mouseX < 166 && mouseY >= 8 && mouseY < 25) {
            lines.add(Component.literal((recipe.buildTime() / 20) + "s"));
        } else if (recipe.materials().size() > 6 && mouseX >= 148 && mouseY >= 43 && mouseY < 70) {
            for (int i = 5; i < recipe.materials().size(); i++) {
                var need = recipe.materials().get(i);
                lines.add(Component.literal(need.count() + "× " + need.label()));
            }
        } else if (recipe.work().size() > 2 && mouseX >= 156 && mouseY >= 75) {
            for (int i = 2; i < recipe.work().size(); i++) {
                var work = recipe.work().get(i);
                lines.add(Component.literal(work.count() + "× " + work.label() + " · " + work.input().toLowerCase() + " click"));
            }
        }
    }
}
