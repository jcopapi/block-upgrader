package dev.jco.upgrades;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class UpgradeClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.DoubleValue HUD_SCALE = BUILDER
        .comment("Scale of Block Upgrader's on-screen recipe panels. 1.0 is the default size.")
        .defineInRange("hudScale", 1.0, 0.5, 2.0);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private UpgradeClientConfig() {}
    public static void register(ModContainer container) { container.registerConfig(ModConfig.Type.CLIENT, SPEC, "block_upgrader-client.toml"); }
    public static float hudScale() { return HUD_SCALE.get().floatValue(); }
}
