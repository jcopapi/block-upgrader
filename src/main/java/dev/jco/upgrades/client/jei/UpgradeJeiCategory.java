package dev.jco.upgrades.client.jei;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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

/** Compact recipe content on JEI's own vanilla-style background. */
public final class UpgradeJeiCategory implements IRecipeCategory<UpgradeJeiRecipe> {
    public static final RecipeType<UpgradeJeiRecipe> TYPE = RecipeType.create("jco_upgrades", "block_upgrade", UpgradeJeiRecipe.class);
    private static final int WIDTH = 180, CONTENT_HEIGHT = 145;
    private static final int SOURCE_X = 8, RESULT_X = 65, BLOCK_Y = 13;
    private static final int MATERIAL_X = 8, MATERIAL_Y = 75;
    private static final int WORK_Y = 120;
    private static final int VISIBLE_WORK = 2;
    private static final int TEXT = 0xFF404040, MUTED = 0xFF686868;
    private final IDrawable icon, recipeArrow, blockBackground;
    private final IIngredientRenderer<ItemStack> blockRenderer;

    public UpgradeJeiCategory(mezz.jei.api.helpers.IJeiHelpers helpers) {
        var gui = helpers.getGuiHelper();
        icon = gui.createDrawableItemStack(new ItemStack(Items.IRON_PICKAXE));
        recipeArrow = gui.getRecipeArrow();
        blockBackground = new ScaledDrawable(gui.getSlotDrawable(), 1.5F);
        var itemRenderer = helpers.getIngredientManager().getIngredientRenderer(VanillaTypes.ITEM_STACK);
        blockRenderer = new ScaledItemRenderer(itemRenderer, 1.5F, -1F, -1F);
    }
    @Override public RecipeType<UpgradeJeiRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.literal("Upgrade"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    // JEI vertically centers each recipe in its page. Reserve the page height so the
    // visible content starts beside the first row instead of halfway down the screen.
    @Override public int getHeight() {
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return Math.max(CONTENT_HEIGHT, Math.min(300, screenHeight - 96));
    }
    @Override public ResourceLocation getRegistryName(UpgradeJeiRecipe recipe) { return recipe.id(); }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, UpgradeJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(SOURCE_X, BLOCK_Y).addItemStack(recipe.source())
            .setCustomRenderer(VanillaTypes.ITEM_STACK, blockRenderer)
            .setBackground(blockBackground, -2, -2);
        if (net.neoforged.fml.ModList.get().isLoaded("sophisticatedstorage")) {
            var variants = dev.jco.upgrades.integration.SophisticatedStorage.jeiWoodVariants(recipe.source());
            if (!variants.isEmpty()) builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(variants);
        }
        builder.addOutputSlot(RESULT_X, BLOCK_Y).addItemStack(recipe.result())
            .setCustomRenderer(VanillaTypes.ITEM_STACK, blockRenderer)
            .setBackground(blockBackground, -2, -2);
        if (net.neoforged.fml.ModList.get().isLoaded("sophisticatedstorage") && !recipe.fixedResultWood()) {
            var variants = dev.jco.upgrades.integration.SophisticatedStorage.jeiWoodVariants(recipe.result());
            if (!variants.isEmpty()) builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStacks(variants);
        }
        for (int i = 1; i < recipe.outputs().size(); i++)
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(recipe.outputs().get(i));

        int visibleMaterials = Math.min(recipe.materials().size(), visibleMaterials(recipe));
        for (int i = 0; i < recipe.materials().size(); i++) {
            var material = recipe.materials().get(i);
            if (i < visibleMaterials) {
                var slot = builder.addInputSlot(MATERIAL_X + i * 23, MATERIAL_Y).setStandardSlotBackground();
                addMaterial(slot, material);
                slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.literal(material.count() + "x " + material.label())));
            }
            else addMaterial(builder.addInvisibleIngredients(RecipeIngredientRole.INPUT), material);
        }
        for (int i = 0; i < recipe.work().size(); i++) {
            var action = recipe.work().get(i);
            if (i < VISIBLE_WORK) {
                var slot = builder.addSlot(RecipeIngredientRole.CATALYST, 8 + i * 88, WORK_Y).setStandardSlotBackground();
                addTool(slot, action);
                slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.literal(
                    action.count() + " uses: " + action.label() + (action.reduction() > 0 ? " (-" + duration(action.reduction()) + ")" : ""))));
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
        var font = Minecraft.getInstance().font;
        recipeArrow.draw(gui, 37, 17);
        var titleLines = font.split(Component.literal(recipe.title()), 76);
        for (int i = 0; i < Math.min(2, titleLines.size()); i++)
            gui.drawString(font, titleLines.get(i), 100, 4 + i * 11, TEXT, false);
        int lineY = 8 + Math.min(2, titleLines.size()) * 11;
        for (var line : font.split(Component.literal(recipe.description()), 76)) {
            if (lineY > 47) break;
            gui.drawString(font, line, 100, lineY, MUTED, false);
            lineY += 11;
        }

        gui.drawString(font, "Materials", 6, 61, TEXT, false);
        if (recipe.materials().size() > visibleMaterials(recipe))
            gui.drawString(font, "+" + (recipe.materials().size() - visibleMaterials(recipe)), recipe.buildTime() > 0 ? 79 : 151, 80, MUTED, false);

        if (recipe.buildTime() > 0) {
            gui.drawString(font, "Construction", 101, 61, TEXT, false);
            gui.renderItem(new ItemStack(Items.CLOCK), 101, 74);
            gui.drawString(font, duration(recipe.buildTime()), 122, 78, TEXT, false);
            gui.fill(101, 96, 176, 99, 0xFF8F8F8F);
            gui.fill(102, 97, 175, 98, 0xFFD0D0D0);
        }

        if (!recipe.work().isEmpty()) {
            gui.drawString(font, recipe.work().stream().anyMatch(w -> w.kind().equals("accelerator")) ? "Work / Accelerators" : "Required work", 6, 106, TEXT, false);
            for (int i = 0; i < Math.min(recipe.work().size(), VISIBLE_WORK); i++) {
                var action = recipe.work().get(i);
                int x = 8 + i * 88;
                gui.drawString(font, fit(action.label(), 60), x + 20, WORK_Y - 1, TEXT, false);
                String detail = action.count() + " uses" + (action.reduction() > 0 ? "  -" + duration(action.reduction()) : "");
                gui.drawString(font, fit(detail, 60), x + 20, WORK_Y + 9, MUTED, false);
            }
            if (recipe.work().size() > VISIBLE_WORK)
                gui.drawString(font, "+" + (recipe.work().size() - VISIBLE_WORK), 158, 106, MUTED, false);
        }
    }

    @Override public void getTooltip(ITooltipBuilder lines, UpgradeJeiRecipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (mouseX >= 100 && mouseX < 178 && mouseY >= 3 && mouseY < 56) {
            lines.add(Component.literal(recipe.title()));
            if (!recipe.description().isBlank()) lines.add(Component.literal(recipe.description()));
        } else if (recipe.materials().size() > visibleMaterials(recipe) && mouseX >= (recipe.buildTime() > 0 ? 76 : 148) && mouseX < (recipe.buildTime() > 0 ? 100 : 178) && mouseY >= 73 && mouseY < 96) {
            for (int i = visibleMaterials(recipe); i < recipe.materials().size(); i++) {
                var material = recipe.materials().get(i);
                lines.add(Component.literal(material.count() + "x " + material.label()));
            }
        } else if (recipe.work().size() > VISIBLE_WORK && mouseX >= 156 && mouseX < 178 && mouseY >= 103 && mouseY < 119) {
            for (int i = VISIBLE_WORK; i < recipe.work().size(); i++) {
                var action = recipe.work().get(i);
                lines.add(Component.literal(action.count() + " uses: " + action.label()));
            }
        }
    }

    private static int visibleMaterials(UpgradeJeiRecipe recipe) { return recipe.buildTime() > 0 ? 3 : 6; }

    private static String duration(int ticks) {
        return ticks % 20 == 0 ? ticks / 20 + "s" : String.format(Locale.ROOT, "%.1fs", ticks / 20.0);
    }

    private static String fit(String value, int width) {
        var font = Minecraft.getInstance().font;
        if (font.width(value) <= width) return value;
        return font.plainSubstrByWidth(value, Math.max(0, width - font.width("..."))) + "...";
    }

    private record ScaledDrawable(IDrawable original, float scale) implements IDrawable {
        @Override public int getWidth() { return Math.round(original.getWidth() * scale); }
        @Override public int getHeight() { return Math.round(original.getHeight() * scale); }
        @Override public void draw(GuiGraphics gui, int x, int y) {
            gui.pose().pushPose();
            try {
                gui.pose().translate(x, y, 0);
                gui.pose().scale(scale, scale, 1);
                original.draw(gui, 0, 0);
            } finally { gui.pose().popPose(); }
        }
    }

    private record ScaledItemRenderer(IIngredientRenderer<ItemStack> original, float scale, float offsetX, float offsetY) implements IIngredientRenderer<ItemStack> {
        @Override public int getWidth() { return Math.round(16 * scale); }
        @Override public int getHeight() { return Math.round(16 * scale); }
        @Override public void render(GuiGraphics gui, ItemStack stack) {
            gui.pose().pushPose();
            try {
                gui.pose().translate(offsetX, offsetY, 0);
                gui.pose().scale(scale, scale, 1);
                original.render(gui, stack);
            } finally { gui.pose().popPose(); }
        }
        @Override public List<Component> getTooltip(ItemStack stack, TooltipFlag flag) { return original.getTooltip(stack, flag); }
    }
}
