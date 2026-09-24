package dev.jco.upgrades.client;
import net.minecraft.resources.ResourceLocation;import net.minecraft.client.gui.GuiGraphics;
/** Swap these two 16x16 resources in a resource pack; action logic is texture-independent. */
public final class InputIcons {
 public static final ResourceLocation LEFT=ResourceLocation.parse("jco:textures/gui/input/left_click.png"),RIGHT=ResourceLocation.parse("jco:textures/gui/input/right_click.png");
 public static boolean matches(net.minecraft.world.item.ItemStack stack,String selector){if(selector.isEmpty()||stack.isEmpty())return false;return selector.startsWith("#")?stack.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,ResourceLocation.parse(selector.substring(1)))):stack.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(selector)));}
 public static int draw(GuiGraphics g,String input,int x,int y){boolean both="BOTH".equals(input);g.blit("LEFT".equals(input)||both?LEFT:RIGHT,x,y,0,0,12,12,12,12);if(both)g.blit(RIGHT,x+11,y,0,0,12,12,12,12);return both?24:13;}
}
