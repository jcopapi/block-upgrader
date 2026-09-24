package dev.jco.upgrades.client.jei;

import com.mojang.logging.LogUtils;
import dev.jco.upgrades.client.RecipeCatalogClient;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

/** Optional JEI plugin; this class is only discovered when JEI is installed. */
@JeiPlugin
public final class UpgradeJeiPlugin implements IModPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static UpgradeJeiPlugin instance;
    private IJeiRuntime runtime;
    private List<UpgradeJeiRecipe> displayed = List.of();
    private long appliedRevision = -1;

    public UpgradeJeiPlugin() { instance = this; }
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.parse("jco_upgrades:jei"); }
    @Override public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new UpgradeJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }
    @Override public void registerRecipes(IRecipeRegistration registration) {
        displayed = read();
        registration.addRecipes(UpgradeJeiCategory.TYPE, displayed);
        if (Minecraft.getInstance().level != null) appliedRevision = RecipeCatalogClient.revision();
    }
    @Override public void onRuntimeAvailable(IJeiRuntime jei) {
        runtime = jei;
        refresh();
    }
    @Override public void onRuntimeUnavailable() {
        runtime = null;
        displayed = List.of();
        appliedRevision = -1;
    }

    public static void refresh() { if (instance != null) instance.refreshNow(); }
    private void refreshNow() {
        if (runtime == null || Minecraft.getInstance().level == null || appliedRevision == RecipeCatalogClient.revision()) return;
        var manager = runtime.getRecipeManager();
        if (!displayed.isEmpty()) manager.hideRecipes(UpgradeJeiCategory.TYPE, displayed);
        displayed = read();
        if (!displayed.isEmpty()) manager.addRecipes(UpgradeJeiCategory.TYPE, displayed);
        appliedRevision = RecipeCatalogClient.revision();
    }

    private static List<UpgradeJeiRecipe> read() {
        var level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        var raw = RecipeCatalogClient.current().getList("recipes", 10);
        var recipes = new ArrayList<UpgradeJeiRecipe>(raw.size());
        for (var entry : raw) {
            try { recipes.add(UpgradeJeiRecipe.read((net.minecraft.nbt.CompoundTag) entry, level.registryAccess())); }
            catch (RuntimeException ex) { LOGGER.error("Cannot display Block Upgrader JEI recipe", ex); }
        }
        return List.copyOf(recipes);
    }
}
