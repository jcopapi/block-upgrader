package dev.jco.upgrades.mixin;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo @Mixin(targets="com.simibubi.create.content.contraptions.glue.SuperGlueSelectionHandler",remap=false)
public abstract class CreateGlueClientMixin {
 @Shadow private net.minecraft.core.BlockPos firstPos;
 @Shadow private java.util.Set<net.minecraft.core.BlockPos> currentCluster;
 @Shadow private int clusterCooldown;
 @Inject(method="onMouseInput",at=@At("HEAD"),cancellable=true)private void jco$work(boolean attack,CallbackInfoReturnable<Boolean> ci){var mc=net.minecraft.client.Minecraft.getInstance();if(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit&&dev.jco.upgrades.client.IncompleteOverlay.contains(hit.getBlockPos())){currentCluster=null;firstPos=null;clusterCooldown=0;ci.setReturnValue(false);}}
}
