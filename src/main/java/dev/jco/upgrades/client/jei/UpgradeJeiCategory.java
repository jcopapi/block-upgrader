package dev.jco.upgrades.client.jei;

import java.util.Arrays;
import java.util.List;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.ingredients.IIngredientRenderer;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;

/** Block transformation, aligned material list, and optional work in vanilla JEI slots. */
public final class UpgradeJeiCategory implements IRecipeCategory<UpgradeJeiRecipe> {
    public static final RecipeType<UpgradeJeiRecipe> TYPE = RecipeType.create("jco_upgrades", "block_upgrade", UpgradeJeiRecipe.class);
    private static final int WIDTH = 190, HEIGHT = 139;
    private final IDrawable icon, largeOutputBackground;
    private final IIngredientRenderer<ItemStack> largeOutput;

    public UpgradeJeiCategory(mezz.jei.api.helpers.IJeiHelpers helpers) {
        var gui = helpers.getGuiHelper();
        icon = gui.createDrawableItemStack(new ItemStack(Items.IRON_PICKAXE));
        largeOutputBackground = new ScaledDrawable(gui.getOutputSlot(), 2);
        largeOutput = new LargeItemRenderer(helpers.getIngredientManager().getIngredientRenderer(VanillaTypes.ITEM_STACK));
    }
    @Override public RecipeType<UpgradeJeiRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.literal("Upgrade"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }
    @Override public ResourceLocation getRegistryName(UpgradeJeiRecipe recipe) { return recipe.id(); }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, UpgradeJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(34, 15).addItemStack(recipe.source()).setStandardSlotBackground();
        builder.addOutputSlot(124, 45).addItemStack(recipe.result())
            .setCustomRenderer(VanillaTypes.ITEM_STACK, largeOutput)
            .setBackground(largeOutputBackground, -2, -2);
        for (int i = 1; i < recipe.outputs().size(); i++)
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(recipe.outputs().get(i));

        int visibleMaterials = Math.min(recipe.materials().size(), recipe.materials().size() > 6 ? 5 : 6);
        for (int i = 0; i < recipe.materials().size(); i++) {
            var material = recipe.materials().get(i);
            if (i < visibleMaterials)
                addMaterial(builder.addInputSlot(14 + (i % 2) * 28, 62 + (i / 2) * 24).setStandardSlotBackground(), material);
            else addMaterial(builder.addInvisibleIngredients(RecipeIngredientRole.INPUT), material);
        }
        for (int i = 0; i < recipe.work().size(); i++) {
            var action = recipe.work().get(i);
            if (i < 3) {
                var slot = builder.addSlot(RecipeIngredientRole.CATALYST, 88, 62 + i * 24).setStandardSlotBackground();
                addTool(slot, action);
                slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.literal(
                    action.count() + "x " + action.label() + (action.kind().equals("accelerator") ? " (accelerates)" : ""))));
            } else addTool(builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST), action);
        }
    }

    private static void addMaterial(mezz.jei.api.gui.builder.IIngredientAcceptor<?> slot, UpgradeJeiRecipe.Need need) {
        if (need.selector().startsWith("#")) {
            var tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(need.selector().substring(1)));
            slot.addItemStacks(Arrays.stream(Ingredient.of(tag).getItems()).map(stack -> stack.copyWithCount(need.count())).toList());
        } else slot.addItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(need.selector())).getDefaultInstance().copyWithCount(need.count()));
    }

    private static void addTool(mezz.jei.api.gui.builder.IIngredientAcceptor<?> slot, UpgradeJeiRecipe.Need need) {
        if (need.selector().startsWith("#")) {
            var tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(need.selector().substring(1)));
            slot.addIngredients(Ingredient.of(tag));
        } else slot.addItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(need.selector())).getDefaultInstance());
    }

    @Override public void draw(UpgradeJeiRecipe recipe, IRecipeSlotsView slots, GuiGraphics gui, double mouseX, double mouseY) {
        gui.drawString(Minecraft.getInstance().font, ">", 90, 24, 0xFF555555, false);
        if (recipe.materials().size() > 6)
            gui.drawString(Minecraft.getInstance().font, "+" + (recipe.materials().size() - 5), 43, 120, 0xFF555555, false);
        if (recipe.work().size() > 3)
            gui.drawString(Minecraft.getInstance().font, "+" + (recipe.work().size() - 3), 109, 117, 0xFF555555, false);
    }

    @Override public void getTooltip(ITooltipBuilder lines, UpgradeJeiRecipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (mouseX >= 75 && mouseX < 115 && mouseY >= 15 && mouseY < 42) {
            lines.add(Component.literal(recipe.title()));
            if (!recipe.description().isBlank()) lines.add(Component.literal(recipe.description()));
            if (recipe.placedIncomplete()) lines.add(Component.literal("Finish after placement"));
        } else if (recipe.materials().size() > 6 && mouseX >= 40 && mouseX < 72 && mouseY >= 113) {
            for (int i = 5; i < recipe.materials().size(); i++) {
                var material = recipe.materials().get(i);
                lines.add(Component.literal(material.count() + "x " + material.label()));
            }
        } else if (recipe.work().size() > 3 && mouseX >= 105 && mouseX < 125 && mouseY >= 112) {
            for (int i = 3; i < recipe.work().size(); i++) {
                var action = recipe.work().get(i);
                lines.add(Component.literal(action.count() + "x " + action.label()));
            }
        }
    }

    private record ScaledDrawable(IDrawable original, int scale) implements IDrawable {
        @Override public int getWidth() { return original.getWidth() * scale; }
        @Override public int getHeight() { return original.getHeight() * scale; }
        @Override public void draw(GuiGraphics gui, int x, int y) {
            gui.pose().pushPose();
            try {
                gui.pose().translate(x, y, 0);
                gui.pose().scale(scale, scale, 1);
                original.draw(gui, 0, 0);
            } finally { gui.pose().popPose(); }
        }
    }

    private record LargeItemRenderer(IIngredientRenderer<ItemStack> original) implements IIngredientRenderer<ItemStack> {
        @Override public int getWidth() { return 48; }
        @Override public int getHeight() { return 48; }
        @Override public void render(GuiGraphics gui, ItemStack stack) {
            gui.pose().pushPose();
            try {
                gui.pose().scale(3, 3, 1);
                original.render(gui, stack);
            } finally { gui.pose().popPose(); }
        }
        @Override public List<Component> getTooltip(ItemStack stack, TooltipFlag flag) { return original.getTooltip(stack, flag); }
    }
}
