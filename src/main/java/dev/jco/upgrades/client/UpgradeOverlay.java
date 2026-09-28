package dev.jco.upgrades.client;
import dev.jco.upgrades.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
@EventBusSubscriber(modid="jco_upgrades",value=Dist.CLIENT)
public final class UpgradeOverlay {
 private static boolean useHeld;
 @SubscribeEvent public static void release(InputEvent.MouseButton.Post e){if(e.getAction()==0)useHeld=false;}
 private static long switched;private static String last="";
 public static void accept(UpgradePayload p){if(!last.equals(p.view().getString("id"))){last=p.view().getString("id");switched=System.nanoTime();}WorldIndicator.accept(p);}
 private static UpgradePayload active(){
  var mc=Minecraft.getInstance();
  if(mc.player==null||mc.level==null||mc.screen!=null||mc.options.hideGui||!(mc.hitResult instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK)return null;
  var p=WorldIndicator.lookup(hit.getBlockPos());
  if(p==null||p.view().isEmpty()||!net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(hit.getBlockPos()).getBlock()).toString().equals(p.view().getString("block"))||mc.player.position().distanceTo(Vec3.atCenterOf(hit.getBlockPos()))>p.view().getDouble("range"))return null;
  if(p.view().getBoolean("completed"))return null;
  boolean hammer=InputIcons.matches(mc.player.getMainHandItem(),"#jco:hammers")||(p.view().contains("reverse")&&InputIcons.matches(mc.player.getMainHandItem(),p.view().getCompound("reverse").getString("tool")));
  boolean forward=previewTool(p.view())||hammer||holdingClipboard()||p.view().getBoolean("incomplete")||p.view().getBoolean("locked")||p.view().getBoolean("compact");
  if(!forward){if(!p.view().contains("reverse"))return null;var reverse=p.view().copy();reverse.putBoolean("reverseOnly",true);reverse.put("rows",new ListTag());reverse.putInt("routes",0);return new UpgradePayload(p.dimension(),p.pos(),p.lines(),p.display(),p.remaining(),reverse);}
  return p;
 }
 private static boolean holdingClipboard(){
  var mc=Minecraft.getInstance();
  return mc.player!=null&&net.neoforged.fml.ModList.get().isLoaded("create")&&net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(mc.player.getMainHandItem().getItem()).toString().equals("create:clipboard");
 }
 private static boolean previewTool(CompoundTag view){
  var mc=Minecraft.getInstance();if(mc.player==null)return false;var stack=mc.player.getMainHandItem();
  if(stack.has(net.minecraft.core.component.DataComponents.TOOL))return true;
  for(var raw:view.getList("tools",8))if(InputIcons.matches(stack,raw.getAsString()))return true;
  return false;
 }
 @SubscribeEvent public static void attack(InputEvent.InteractionKeyMappingTriggered e){if(e.isCanceled()||!e.isAttack()&&!e.isUseItem()||e.isUseItem()&&e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND)return;var packet=active();if(packet==null)return;var n=packet.view();var pos=packet.pos();var stack=Minecraft.getInstance().player.getMainHandItem();if(e.isUseItem()&&n.contains("reverse")&&InputIcons.matches(stack,n.getCompound("reverse").getString("tool"))){e.setCanceled(true);e.setSwingHand(false);if(useHeld)return;useHeld=true;PacketDistributor.sendToServer(new WorkPayload(pos,2));return;} String expected=e.isAttack()?"LEFT":"RIGHT";var rows=n.getList("rows",10);boolean cancel=e.isUseItem()&&n.getBoolean("locked")&&Minecraft.getInstance().player.isShiftKeyDown()&&stack.isEmpty();boolean allowed=cancel||e.isUseItem()&&n.getBoolean("blockUpgrade")&&holdingClipboard()||e.isUseItem()&&!n.getBoolean("locked")&&previewTool(n);for(var raw:rows){var row=(CompoundTag)raw;String input=row.getString("input");if((input.equals(expected)||input.equals("BOTH"))&&row.getInt("have")<row.getInt("need")&&InputIcons.matches(stack,row.getString("matcher")))allowed=true;}if(!allowed)return;e.setCanceled(true);e.setSwingHand(false);if(e.isUseItem()){if(useHeld)return;useHeld=true;}PacketDistributor.sendToServer(new WorkPayload(pos,e.isAttack()?0:1));}

 @SubscribeEvent public static void scroll(InputEvent.MouseScrollingEvent e){
  var p=active();if(p==null)return;
  if(previewTool(p.view())&&p.view().getInt("routes")>1&&!p.view().getBoolean("locked")){e.setCanceled(true);if(e.getScrollDeltaY()!=0)PacketDistributor.sendToServer(new SelectRoutePayload(p.pos(),e.getScrollDeltaY()>0?-1:1));}
 }
 @SubscribeEvent public static void register(RegisterGuiLayersEvent e){e.registerAboveAll(ResourceLocation.parse("jco_upgrades:overlay"),(gui,delta)->{
  var p=active();var mc=Minecraft.getInstance();var n=p==null?null:p.view().copy();if(n!=null&&n.contains("remaining"))n.putLong("remaining",Math.max(0,n.getLong("remaining")-(long)(WorldIndicator.age(p.pos())*20)));if(mc.player==null||mc.level==null||mc.screen!=null){HudMotion.clear();return;}
  if(n!=null&&n.contains("reverse")&&(mc.player.isShiftKeyDown()||InputIcons.matches(mc.player.getMainHandItem(),n.getCompound("reverse").getString("tool")))){SidePanels.opacity=1;SidePanels.pulse=0;SidePanels.carouselOffset=0;SidePanels.draw(gui,n.getCompound("reverse"),false,false);}
  HudMotion.draw(gui,n!=null&&(n.getBoolean("compact")||n.getBoolean("reverseOnly"))?null:n,previewTool(n==null?new CompoundTag():n),holdingClipboard());
 });}
}
