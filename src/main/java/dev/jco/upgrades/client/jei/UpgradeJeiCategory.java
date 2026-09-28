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

/** Recipe card styled around the source, result, construction, and optional work. */
public final class UpgradeJeiCategory implements IRecipeCategory<UpgradeJeiRecipe> {
    public static final RecipeType<UpgradeJeiRecipe> TYPE = RecipeType.create("jco_upgrades", "block_upgrade", UpgradeJeiRecipe.class);
    private static final int WIDTH = 228, HEIGHT = 174;
    private static final int SOURCE_X = 18, RESULT_X = 82, BLOCK_Y = 24;
    private static final int MATERIAL_X = 14;
    private static final int WORK_Y = 150;
    private static final int VISIBLE_WORK = 2;
    private static final int BACK = 0xFF111820, PANEL = 0xFF1D252C;
    private static final int EDGE = 0xFF475057, SHADE = 0xFF0B1116;
    private static final int TEXT = 0xFFE6E9E9, MUTED = 0xFFAFBAC0;
    private final IDrawable icon, recipeArrow;
    private final IIngredientRenderer<ItemStack> blockRenderer;

    public UpgradeJeiCategory(mezz.jei.api.helpers.IJeiHelpers helpers) {
        var gui = helpers.getGuiHelper();
        icon = gui.createDrawableItemStack(new ItemStack(Items.IRON_PICKAXE));
        recipeArrow = gui.getRecipeArrow();
        var itemRenderer = helpers.getIngredientManager().getIngredientRenderer(VanillaTypes.ITEM_STACK);
        blockRenderer = new ScaledItemRenderer(itemRenderer, 1.75F, -1F, -2F);
    }
    @Override public RecipeType<UpgradeJeiRecipe> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.literal("Upgrade"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }
    @Override public ResourceLocation getRegistryName(UpgradeJeiRecipe recipe) { return recipe.id(); }

    @Override public void setRecipe(IRecipeLayoutBuilder builder, UpgradeJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(SOURCE_X, BLOCK_Y).addItemStack(recipe.source())
            .setCustomRenderer(VanillaTypes.ITEM_STACK, blockRenderer);
        if (net.neoforged.fml.ModList.get().isLoaded("sophisticatedstorage")) {
            var variants = dev.jco.upgrades.integration.SophisticatedStorage.jeiWoodVariants(recipe.source());
            if (!variants.isEmpty()) builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(variants);
        }
        builder.addOutputSlot(RESULT_X, BLOCK_Y).addItemStack(recipe.result())
            .setCustomRenderer(VanillaTypes.ITEM_STACK, blockRenderer);
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
                var slot = builder.addInputSlot(MATERIAL_X + i * 23, materialY(recipe));
                addMaterial(slot, material);
                slot.addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.literal(material.count() + "x " + material.label())));
            }
            else addMaterial(builder.addInvisibleIngredients(RecipeIngredientRole.INPUT), material);
        }
        for (int i = 0; i < recipe.work().size(); i++) {
            var action = recipe.work().get(i);
            if (i < VISIBLE_WORK) {
                var slot = builder.addSlot(RecipeIngredientRole.CATALYST, 14 + i * 107, WORK_Y);
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
        panel(gui, 0, 0, WIDTH, HEIGHT);
        panel(gui, 6, 6, 222, 77);
        slot(gui, SOURCE_X - 3, BLOCK_Y - 3, 34);
        slot(gui, RESULT_X - 3, BLOCK_Y - 3, 34);
        recipeArrow.draw(gui, 50, 30);
        var titleLines = font.split(Component.literal(recipe.title()), 103);
        for (int i = 0; i < Math.min(2, titleLines.size()); i++)
            gui.drawString(font, titleLines.get(i), 117, 12 + i * 11, TEXT, false);
        int lineY = 16 + Math.min(2, titleLines.size()) * 11;
        for (var line : font.split(Component.literal(recipe.description()), 102)) {
            if (lineY > 61) break;
            gui.drawString(font, line, 117, lineY, MUTED, false);
            lineY += 11;
        }

        int materialRight = recipe.buildTime() > 0 ? 113 : 222;
        panel(gui, 6, 83, materialRight, recipe.work().isEmpty() ? 168 : 126);
        gui.drawString(font, "Materials", 12, 89, TEXT, false);
        for (int i = 0; i < Math.min(recipe.materials().size(), visibleMaterials(recipe)); i++)
            slot(gui, MATERIAL_X + i * 23 - 2, materialY(recipe) - 2, 20);
        if (recipe.materials().size() > visibleMaterials(recipe))
            gui.drawString(font, "+" + (recipe.materials().size() - visibleMaterials(recipe)), materialRight - 25, materialY(recipe) + 5, MUTED, false);

        if (recipe.buildTime() > 0) {
            panel(gui, 117, 83, 222, recipe.work().isEmpty() ? 168 : 126);
            gui.drawString(font, "Construction", 123, 89, TEXT, false);
            int timeY = recipe.work().isEmpty() ? 121 : 100;
            gui.renderItem(new ItemStack(Items.CLOCK), 123, timeY);
            gui.drawString(font, duration(recipe.buildTime()), 143, timeY + 4, TEXT, false);
            gui.fill(123, timeY + 22, 215, timeY + 25, SHADE);
            gui.fill(124, timeY + 23, 214, timeY + 24, EDGE);
        }

        if (!recipe.work().isEmpty()) {
            panel(gui, 6, 130, 222, 172);
            gui.drawString(font, recipe.work().stream().anyMatch(w -> w.kind().equals("accelerator")) ? "Work / Accelerators" : "Required work", 12, 134, TEXT, false);
            for (int i = 0; i < Math.min(recipe.work().size(), VISIBLE_WORK); i++) {
                var action = recipe.work().get(i);
                int x = 14 + i * 107;
                slot(gui, x - 2, WORK_Y - 2, 20);
                gui.drawString(font, fit(action.label(), 78), x + 20, WORK_Y - 1, TEXT, false);
                String detail = action.count() + " uses" + (action.reduction() > 0 ? "  -" + duration(action.reduction()) : "");
                gui.drawString(font, fit(detail, 78), x + 20, WORK_Y + 9, MUTED, false);
            }
            if (recipe.work().size() > VISIBLE_WORK)
                gui.drawString(font, "+" + (recipe.work().size() - VISIBLE_WORK), 205, 134, MUTED, false);
        }
    }

    @Override public void getTooltip(ITooltipBuilder lines, UpgradeJeiRecipe recipe, IRecipeSlotsView slots, double mouseX, double mouseY) {
        if (mouseX >= 117 && mouseX < 222 && mouseY >= 9 && mouseY < 77) {
            lines.add(Component.literal(recipe.title()));
            if (!recipe.description().isBlank()) lines.add(Component.literal(recipe.description()));
        } else if (recipe.materials().size() > visibleMaterials(recipe) && mouseX >= (recipe.buildTime() > 0 ? 88 : 197) && mouseX < (recipe.buildTime() > 0 ? 113 : 222) && mouseY >= materialY(recipe) - 2 && mouseY < materialY(recipe) + 20) {
            for (int i = visibleMaterials(recipe); i < recipe.materials().size(); i++) {
                var material = recipe.materials().get(i);
                lines.add(Component.literal(material.count() + "x " + material.label()));
            }
        } else if (recipe.work().size() > VISIBLE_WORK && mouseX >= 201 && mouseX < 222 && mouseY >= 130 && mouseY < 148) {
            for (int i = VISIBLE_WORK; i < recipe.work().size(); i++) {
                var action = recipe.work().get(i);
                lines.add(Component.literal(action.count() + " uses: " + action.label()));
            }
        }
    }

    private static int visibleMaterials(UpgradeJeiRecipe recipe) { return recipe.buildTime() > 0 ? 3 : 8; }
    private static int materialY(UpgradeJeiRecipe recipe) { return recipe.work().isEmpty() ? 122 : 100; }

    private static String duration(int ticks) {
        return ticks % 20 == 0 ? ticks / 20 + "s" : String.format(Locale.ROOT, "%.1fs", ticks / 20.0);
    }

    private static String fit(String value, int width) {
        var font = Minecraft.getInstance().font;
        if (font.width(value) <= width) return value;
        return font.plainSubstrByWidth(value, Math.max(0, width - font.width("..."))) + "...";
    }

    private static void panel(GuiGraphics gui, int left, int top, int right, int bottom) {
        gui.fill(left, top, right, bottom, SHADE);
        gui.fill(left + 1, top + 1, right - 1, bottom - 1, EDGE);
        gui.fill(left + 2, top + 2, right - 2, bottom - 2, PANEL);
        gui.fill(left + 3, top + 3, right - 3, bottom - 3, BACK);
    }

    private static void slot(GuiGraphics gui, int x, int y, int size) {
        gui.fill(x, y, x + size, y + size, SHADE);
        gui.fill(x + 1, y + 1, x + size - 1, y + size - 1, EDGE);
        gui.fill(x + 2, y + 2, x + size - 2, y + size - 2, PANEL);
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
