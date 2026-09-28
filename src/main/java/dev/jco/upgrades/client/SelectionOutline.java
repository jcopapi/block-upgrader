package dev.jco.upgrades.client;

import dev.jco.upgrades.PairedBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Outline every half of the selected upgrade, including double chests and tall blocks. */
@EventBusSubscriber(modid = "jco_upgrades", value = Dist.CLIENT)
public final class SelectionOutline {
    private SelectionOutline() {}

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        var mc = Minecraft.getInstance();
        var selection = UpgradeOverlay.active();
        if (mc.level == null || mc.player == null || selection == null
            || selection.view().getBoolean("reverseOnly")
            || !mc.player.isShiftKeyDown() && !selection.view().getBoolean("locked")) return;
        BlockPos first = selection.pos();
        BlockPos partner = PairedBlocks.partner(mc.level, first);
        if (partner == null) return;
        var camera = event.getCamera().getPosition();
        var buffer = mc.renderBuffers().bufferSource();
        var lines = buffer.getBuffer(RenderType.lines());
        for (var pos : new BlockPos[] {first, partner}) {
            var shape = mc.level.getBlockState(pos).getShape(mc.level, pos);
            if (shape.isEmpty()) continue;
            var bounds = shape.bounds().inflate(.008).move(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
            LevelRenderer.renderLineBox(event.getPoseStack(), lines, bounds, .55F, .94F, .70F, .9F);
        }
        buffer.endBatch(RenderType.lines());
    }
}
