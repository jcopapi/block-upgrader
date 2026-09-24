package dev.jco.upgrades.mixin;

import dev.jco.upgrades.UpgradeRuntime;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
public abstract class UpgradeRemovalMixin {
    @Inject(method="setBlockState",at=@At("RETURN"))
    private void jco$removed(BlockPos pos, BlockState state, boolean moving, CallbackInfoReturnable<BlockState> cir) {
        var chunk=(LevelChunk)(Object)this;
        if(cir.getReturnValue()!=null && cir.getReturnValue().getBlock()!=state.getBlock() && chunk.getLevel() instanceof ServerLevel level)
            UpgradeRuntime.removed(level,pos);
    }
}
