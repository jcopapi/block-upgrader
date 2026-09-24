package dev.jco.upgrades;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
/** Operations stage a plan; the runtime commits only after a successful callback. */
public final class CompletionContext {
 private final ServerLevel level;private final BlockPos pos;private final ServerPlayer player;private final ItemStack tool;
 private final List<ItemStack> drops=new ArrayList<>();private boolean remove;
 public CompletionContext(ServerLevel level,BlockPos pos,ServerPlayer player,ItemStack tool){this.level=level;this.pos=pos.immutable();this.player=player;this.tool=tool.copy();}
 public ServerLevel level(){return level;}public BlockPos pos(){return pos;}public ServerPlayer player(){return player;}public BlockState state(){return level.getBlockState(pos);}public net.minecraft.world.level.block.Block block(){return state().getBlock();}public ItemStack tool(){return tool.copy();}
 public void removeBlock(){remove=true;}
 public void drop(ItemStack stack){if(stack.isEmpty())return;if(drops.size()>=64||stack.getCount()>1000000)throw new IllegalArgumentException("Too many outputs");drops.add(stack.copy());}
 public boolean removes(){return remove;}public List<ItemStack> outputs(){return drops.stream().map(ItemStack::copy).toList();}
}
