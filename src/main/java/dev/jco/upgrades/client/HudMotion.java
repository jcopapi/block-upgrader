package dev.jco.upgrades.client;
import net.minecraft.nbt.CompoundTag;import net.minecraft.client.gui.GuiGraphics;
final class HudMotion {
 private static CompoundTag previous;private static long last=System.nanoTime(),change=last;private static double alpha;private static double shownProgress;private static String id="";private static int materialHash,routeIndex;private static double wheel;
 static void draw(GuiGraphics gui,CompoundTag current,boolean detail,boolean clipboard){long now=System.nanoTime();double dt=Math.min(.1,(now-last)/1E9);last=now;
  alpha+=(current==null?0:1-alpha)*(current==null?0:1-Math.exp(-dt*22));if(current==null)alpha*=Math.exp(-dt*35);
  if(current!=null){if(!id.equals(current.getString("id"))){wheel=Math.signum(current.getInt("index")-routeIndex)*16;id=current.getString("id");change=now;shownProgress=0;}else if(materialHash!=current.getList("rows",10).hashCode())change=now;routeIndex=current.getInt("index");materialHash=current.getList("rows",10).hashCode();previous=current.copy();}
  if(previous==null||alpha<.02)return;double progress=previous.contains("duration")?1-Math.clamp(previous.getLong("remaining")/(double)Math.max(1,previous.getInt("duration")),0,1):0;shownProgress+=(progress-shownProgress)*(1-Math.exp(-dt*12));previous.putDouble("smoothProgress",shownProgress);
  gui.pose().pushPose();gui.pose().translate((1-alpha)*7,0,0);wheel*=Math.exp(-dt*16);SidePanels.carouselOffset=(float)wheel;SidePanels.opacity=(float)alpha;SidePanels.pulse=(float)Math.max(0,1-(now-change)/180_000_000D);SidePanels.draw(gui,previous,detail,clipboard);gui.pose().popPose();
 }
 static void clear(){previous=null;alpha=0;}
}
