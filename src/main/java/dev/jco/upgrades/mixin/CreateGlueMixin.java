package dev.jco.upgrades.mixin;
import dev.jco.upgrades.*;import net.minecraft.core.BlockPos;import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="com.simibubi.create.content.contraptions.glue.SuperGlueSelectionPacket",remap=false)
public abstract class CreateGlueMixin {
 @Shadow public abstract BlockPos from();@Shadow public abstract BlockPos to();
 @Inject(method="handle",at=@At("HEAD"),cancellable=true)private void jco$deny(ServerPlayer player,CallbackInfo ci){
  var box=net.minecraft.world.phys.AABB.encapsulatingFullBlocks(from(),to());
  for(var entry:UpgradeData.get(player.serverLevel()).entries.entrySet())if(entry.getValue().incomplete&&box.contains(net.minecraft.world.phys.Vec3.atCenterOf(BlockPos.of(entry.getKey())))){ci.cancel();return;}
 }
}
