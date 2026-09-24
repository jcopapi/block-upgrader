package dev.jco.upgrades.client.jei;

import dev.jco.upgrades.client.InputIcons;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import mezz.jei.api.recipe.RecipeType;

/** A compact route diagram: source -> result, materials, then time and optional work. */
public final class UpgradeJeiCategory implements IRecipeCategory<UpgradeJeiRecipe> {
    public static final RecipeType<UpgradeJeiRecipe> TYPE = RecipeType.create("jco_upgrades", "block_upgrade", UpgradeJeiRecipe.class);
    private static final int WIDTH = 180, HEIGHT = 145;
    private static final int GOLD = 0xFFE3C58B, MUTED = 0xFFAAB4C4;
    private final IDrawable icon;

    public UpgradeJeiCategory(mezz.jei.api.helpers.IGuiHelper gui) {
        icon = gui.createDrawableItemStack(new ItemStack(Items.SMITHING_TABLE));
    }
    @Override public RecipeType<UpgradeJeiRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.literal("Block Upgrader"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }
    @Override public boolean needsRecipeBorder() { return false; }
    @Override public ResourceLocation getRegistryName(UpgradeJeiRecipe recipe) { return recipe.id(); }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, UpgradeJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(12, 38).addItemStack(recipe.source()).setStandardSlotBackground()
            .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.literal("Placed block")));
        builder.addOutputSlot(150, 38).addItemStack(recipe.result()).setOutputSlotBackground()
            .addRichTooltipCallback((slot, tooltip) -> tooltip.add(Component.literal("Upgrade result")));
        for (int i = 1; i < recipe.outputs().size(); i++)
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(recipe.outputs().get(i));
        for (int i = 0; i < recipe.materials().size(); i++) {
            var need = recipe.materials().get(i);
            if (i < 6) {
                var slot = builder.addInputSlot(6 + i * 28, 88).setStandardSlotBackground();
                addIngredient(slot, need);
                slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.literal("Required: " + need.count())));
            } else addIngredient(builder.addInvisibleIngredients(RecipeIngredientRole.INPUT), need);
        }
        for (int i = 0; i < recipe.work().size(); i++) {
            var action = recipe.work().get(i);
            if (i == 0) {
                var slot = builder.addSlot(RecipeIngredientRole.CATALYST, 6, 119).setStandardSlotBackground();
                addIngredient(slot, action);
                slot.addRichTooltipCallback((view, tooltip) -> {
                    tooltip.add(Component.literal(action.count() + " actions · " + action.input().toLowerCase() + " click"));
                    tooltip.add(Component.literal(action.kind().equals("accelerator") ? "Optional: speeds up construction" : "Required to finish construction"));
                });
            } else addIngredient(builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST), action);
        }
    }

    private static void addIngredient(mezz.jei.api.gui.builder.IIngredientAcceptor<?> slot, UpgradeJeiRecipe.Need need) {
        if (need.selector().startsWith("#")) {
            var tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(need.selector().substring(1)));
            slot.addIngredients(Ingredient.of(tag));
        } else {
            slot.addItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(need.selector())).getDefaultInstance());
        }
    }

    @Override public void draw(UpgradeJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics gui, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        gui.fill(0, 0, WIDTH, HEIGHT, 0xFF1E1C2A);
        gui.fill(1, 1, WIDTH - 1, 2, 0xFF8B704B);
        gui.fill(1, HEIGHT - 2, WIDTH - 1, HEIGHT - 1, 0xFF8B704B);
        gui.fill(1, 2, 2, HEIGHT - 2, 0xFF8B704B);
        gui.fill(WIDTH - 2, 2, WIDTH - 1, HEIGHT - 2, 0xFF8B704B);
        gui.fill(7, 29, WIDTH - 7, 30, 0xFF514663);
        gui.fill(7, 75, WIDTH - 7, 76, 0xFF514663);
        gui.drawString(font, font.plainSubstrByWidth(recipe.title(), WIDTH - 16), 8, 7, GOLD, false);
        if (!recipe.description().isBlank())
            gui.drawString(font, font.plainSubstrByWidth(recipe.description(), WIDTH - 16), 8, 19, MUTED, false);
        else gui.drawString(font, "IN-WORLD TRANSFORMATION", 8, 19, MUTED, false);
        gui.fill(39, 47, 138, 49, 0xFF746282);
        gui.fill(132, 44, 139, 52, 0xFF746282);
        gui.drawString(font, "SOURCE", 7, 61, MUTED, false);
        gui.drawString(font, recipe.outputs().isEmpty() ? "RESULT" : "OUTPUT", 139, 61, MUTED, false);
        gui.drawString(font, "MATERIALS", 7, 78, GOLD, false);
        for (int i = 0; i < Math.min(6, recipe.materials().size()); i++) {
            String count = Integer.toString(recipe.materials().get(i).count());
            gui.drawString(font, count, 17 + i * 28, 106, 0xFFE8D9B9, true);
        }
        if (recipe.materials().size() > 6)
            gui.drawString(font, "+" + (recipe.materials().size() - 6) + " more", 123, 78, MUTED, false);
        gui.fill(7, 115, WIDTH - 7, 116, 0xFF514663);
        if (recipe.work().isEmpty()) {
            gui.drawString(font, recipe.buildTime() == 0 ? "INSTANT" : "BUILD TIME  " + (recipe.buildTime() / 20) + "s", 8, 126, GOLD, false);
        } else {
            var action = recipe.work().getFirst();
            int start = InputIcons.draw(gui, action.input(), 31, 122);
            String line = action.label() + "  " + action.count() + "x";
            gui.drawString(font, font.plainSubstrByWidth(line, WIDTH - 69 - start), 33 + start, 126, GOLD, false);
            if (recipe.buildTime() > 0) gui.drawString(font, (recipe.buildTime() / 20) + "s", WIDTH - 27, 126, MUTED, false);
        }
    }

    @Override public void getTooltip(ITooltipBuilder lines, UpgradeJeiRecipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (mouseY < 30 && !recipe.description().isBlank()) {
            lines.add(Component.literal(recipe.description()));
        } else if (mouseY >= 76 && mouseY < 87 && recipe.materials().size() > 6) {
            for (int i = 6; i < recipe.materials().size(); i++) {
                var need = recipe.materials().get(i);
                lines.add(Component.literal(need.count() + "× " + need.label()));
            }
        } else if (mouseY >= 116) {
            if (recipe.buildTime() > 0) lines.add(Component.literal("Build time: " + recipe.buildTime() + " ticks"));
            for (int i = 1; i < recipe.work().size(); i++) {
                var work = recipe.work().get(i);
                lines.add(Component.literal(work.count() + "× " + work.label() + " · " + work.input().toLowerCase() + " click"));
            }
            if (!recipe.work().isEmpty()) lines.add(Component.literal(recipe.work().getFirst().kind().equals("accelerator") ? "Tool actions are optional" : "Tool actions are required"));
            if (recipe.transfer().equals("sophisticated_storage")) lines.add(Component.literal("Preserves wood, contents and upgrades"));
            else if (recipe.transfer().equals("copy_inventory")) lines.add(Component.literal("Preserves inventory contents"));
        }
    }
}
