package dev.jco.upgrades.client;
import net.minecraft.client.Minecraft;import net.minecraft.client.gui.GuiGraphics;import net.minecraft.nbt.*;import net.minecraft.network.chat.Component;import net.minecraft.world.item.ItemStack;
public final class SidePanels {
 public static float opacity=1,pulse,carouselOffset;
 private static int tint(int color){return ((int)(((color>>>24)&255)*opacity)<<24)|(color&0xffffff);}
 private static void box(GuiGraphics g,int x,int y,int w,int h,boolean active){}

 public static void draw(GuiGraphics g,CompoundTag n,boolean detail,boolean clipboard){
  var mc=Minecraft.getInstance();if(mc.level==null)return;float scale=.65F*dev.jco.upgrades.UpgradeClientConfig.hudScale();int available=g.guiWidth()/2-52;if(available<80)return;int width=Math.min(230,(int)(available/scale));boolean locked=n.getBoolean("locked"),timed=n.contains("duration"),described=!n.getString("description").isBlank();
  var carousel=n.getList("carousel",10);int index=n.getInt("index");boolean wheel=!locked&&carousel.size()>1;
  var rows=n.getList("rows",10);int maxRows=Math.min(rows.size(),Math.max(1,(int)(g.guiHeight()/scale-140)/20));int header=wheel?45:27,rowY=header+(wheel?7:6)+(timed?10:0)+(described?12:0),height=rowY+maxRows*20+(n.getBoolean("blockUpgrade")?20:0)+(clipboard?20:0)+25;
  int x=n.getBoolean("reverse")?g.guiWidth()/2-44-(int)(width*scale):g.guiWidth()/2+44,y=Math.max(24,Math.min(g.guiHeight()/2-26,g.guiHeight()-(int)(height*scale)-18));g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);g.setColor(1,1,1,opacity);

  box(g,0,0,34,34,locked);g.pose().pushPose();g.pose().translate(2,2,0);g.pose().scale(1.875F,1.875F,1);g.renderItem(ItemStack.parseOptional(mc.level.registryAccess(),n.getCompound("icon")),0,0);g.pose().popPose();
  box(g,42,0,width-42,header,locked);if(wheel){for(int offset=-2;offset<=2;offset++){float relative=offset*17+carouselOffset;if(relative< -23||relative>29)continue;var route=carousel.getCompound(Math.floorMod(index+offset,carousel.size()));float emphasis=(float)Math.clamp(1-Math.abs(relative)/25,.2,1);float textScale=.82F+.18F*emphasis;g.pose().pushPose();g.pose().translate(47,5+relative,0);g.pose().scale(textScale,textScale,1);g.drawString(mc.font,mc.font.plainSubstrByWidth(route.getString("title"),(int)((width-55)/textScale)),0,0,tint(((int)(255*emphasis)<<24)|0xD8CBB3));g.pose().popPose();}}else g.drawString(mc.font,mc.font.plainSubstrByWidth(n.getString("title"),width-55),47,5,tint(locked?0xFFA2C2AB:0xFFE5CEAA));
  if(locked)g.fill(43,header-2,width-1,header,tint(0xFF6E9279));
  if(described)g.drawString(mc.font,mc.font.plainSubstrByWidth(n.getString("description"),width-47),43,header+2,tint(0xFF9AA7B2));
  if(timed){int yy=rowY-9;g.fill(43,yy,width,yy+3,tint(0xFF303942));g.fill(43,yy,43+(int)((width-43)*n.getDouble("smoothProgress")),yy+3,tint(0xFFBFA46E));}
  if(maxRows>0)box(g,42,rowY,width-42,maxRows*20+4,locked);
  for(int i=0;i<maxRows;i++){var row=rows.getCompound(i);var item=ItemStack.parseOptional(mc.level.registryAccess(),row.getCompound("icon"));int yy=rowY+3+i*20;boolean done=row.getInt("have")>=row.getInt("need");if(pulse>0)g.fill(43,yy,width-1,yy+18,tint(((int)(pulse*35)<<24)|0x9BB69F));int advance=InputIcons.draw(g,row.getString("input"),45,yy+2);g.renderItem(item,45+advance,yy);String count=row.getInt("have")+"/"+row.getInt("need");int right=width-mc.font.width(count)-5;String label=row.contains("label")?row.getString("label"):item.getHoverName().getString();g.drawString(mc.font,Component.literal(mc.font.plainSubstrByWidth(label,Math.max(0,right-66-advance))).withStyle(style->style.withStrikethrough(done&&row.getBoolean("material"))),64+advance,yy+4,tint(done?0xFF8DAD94:0xFFCCD4DF));g.drawString(mc.font,count,right,yy+4,tint(0xFFE5CEAA));}
  if(wheel)g.drawString(mc.font,(index+1)+"/"+carousel.size(),5,39,tint(0xFF9AA7B2));
  if(n.getBoolean("blockUpgrade")){int yy=rowY+maxRows*20+6;InputIcons.draw(g,"RIGHT",43,yy);String hint=locked?(n.getBoolean("canCancel")?"Cancel":"Build"):"Select";g.drawString(mc.font,hint,58,yy+2,tint(0xFFD5DACF));}
  if(clipboard&&!n.getBoolean("reverse")){int hintY=rowY+maxRows*20+(n.getBoolean("blockUpgrade")?23:5);g.renderItem(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("create:clipboard")).getDefaultInstance(),43,hintY);InputIcons.draw(g,"RIGHT",61,hintY+2);g.drawString(mc.font,"Save",77,hintY+4,tint(0xFF9AA7B2));}
  g.setColor(1,1,1,1);g.pose().popPose();
 }
}
