package dev.jco.upgrades.mixin;
import dev.jco.upgrades.UpgradeRuntime;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity")
public abstract class IncompleteTickerMixin {
 @Shadow @Final private BlockEntity blockEntity;
 @Inject(method="tick",at=@At("HEAD"),cancellable=true)
 private void pause(CallbackInfo ci){if(blockEntity.getLevel() instanceof ServerLevel level&&UpgradeRuntime.incomplete(level,blockEntity.getBlockPos()))ci.cancel();}
}
