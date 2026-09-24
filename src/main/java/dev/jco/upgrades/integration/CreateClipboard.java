package dev.jco.upgrades.integration;
import dev.jco.upgrades.*;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.clipboard.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.*;
public final class CreateClipboard {
 public static void save(ItemStack stack,UpgradeDefinition d){
  var old=stack.getOrDefault(AllDataComponents.CLIPBOARD_CONTENT,ClipboardContent.EMPTY);
  if(old.readOnly())throw new IllegalArgumentException("Clipboard is read-only");
  var pages=new ArrayList<List<ClipboardEntry>>(old.pages());
  var rows=new ArrayList<ClipboardEntry>();rows.add(new ClipboardEntry(false,Component.literal(d.title()+" Materials")));
  for(var m:d.materials()){
   var display=m.feedback().display(UpgradeRuntime.representative(m.item()));
   rows.add(new ClipboardEntry(false,Component.literal(m.count()+"\u00d7 "+UpgradeRuntime.label(m.item(),display))));
  }
  int first=pages.size();for(int i=0;i<rows.size();i+=10)pages.add(List.copyOf(rows.subList(i,Math.min(rows.size(),i+10))));
  if(pages.size()>50)throw new IllegalArgumentException("Clipboard full (50 pages)");
  stack.set(AllDataComponents.CLIPBOARD_CONTENT,old.setPages(pages).setPreviouslyOpenedPage(first).setType(ClipboardOverrides.ClipboardType.WRITTEN));
 }
}
