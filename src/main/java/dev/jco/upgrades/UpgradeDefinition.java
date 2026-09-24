package dev.jco.upgrades;

import java.util.*;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

public final class UpgradeDefinition {
    private String reverseTool="#jco:hammers";private int reverseHits=3;
    public UpgradeDefinition downgrade(String tool,int hits){editable();net.minecraft.resources.ResourceLocation.parse(tool.startsWith("#")?tool.substring(1):tool);if(hits<1||hits>1000000)throw new IllegalArgumentException("Downgrade hits 1..1000000");reverseTool=tool;reverseHits=hits;return this;}
    public String reverseTool(){return reverseTool;}public int reverseHits(){return reverseHits;}

    private boolean manualCompletes, placedIncomplete;public UpgradeDefinition manualCompletes(boolean value){editable();manualCompletes=value;return this;}public boolean manualCompletes(){return manualCompletes;}
    /** Begin construction when a player places the ordinary source block. */
    public UpgradeDefinition placedIncomplete(boolean value){editable();placedIncomplete=value;return this;}public boolean placedIncomplete(){return placedIncomplete;}
    public record Material(StackMatcher item, int count, Feedback feedback) {}
    public record Stage(String type, StackMatcher item, int actions, int consume, int damage, Feedback feedback) {}
    public final String id;
    private Block source, target;
    private int buildTime;private final List<Accelerator> accelerators=new ArrayList<>();
    public UpgradeDefinition buildTime(int ticks){editable();if(ticks<0||ticks>1000000000)throw new IllegalArgumentException("Build time 0..1000000000 ticks");buildTime=ticks;return this;}public int buildTime(){return buildTime;}
    public List<Accelerator> accelerators(){return Collections.unmodifiableList(accelerators);}
    public UpgradeDefinition acceleratorTool(String item,int actions,int reduction,Consumer<Accelerator> c){return accelerator(item,actions,reduction,1,0,c);}
    public UpgradeDefinition acceleratorItem(String item,int actions,int reduction,Consumer<Accelerator> c){return accelerator(item,actions,reduction,0,1,c);}
    private UpgradeDefinition accelerator(String item,int actions,int reduction,int damage,int consume,Consumer<Accelerator> c){editable();if(accelerators.size()>=32)throw new IllegalArgumentException("Max 32 accelerators");var a=new Accelerator(item,actions,reduction,damage,consume);c.accept(a);a.freeze();accelerators.add(a);return this;}

    private final List<net.minecraft.world.item.ItemStack> outputs=new ArrayList<>();
    private boolean removeBlock;
    private Consumer<CompletionContext> onComplete;
    public UpgradeDefinition output(net.minecraft.world.item.ItemStack stack){editable();if(stack.isEmpty()||stack.getCount()>1000000||outputs.size()>=64)throw new IllegalArgumentException("Invalid output");outputs.add(stack.copy());return this;}
    public UpgradeDefinition removeBlockOnComplete(boolean value){editable();removeBlock=value;return this;}
    public UpgradeDefinition onComplete(Consumer<CompletionContext> callback){editable();onComplete=Objects.requireNonNull(callback);return this;}
    public boolean itemOutput(){return target==null;}
    public boolean removesBlock(){return removeBlock;}
    public List<net.minecraft.world.item.ItemStack> outputs(){return outputs.stream().map(net.minecraft.world.item.ItemStack::copy).toList();}
    public Consumer<CompletionContext> completionCallback(){return onComplete;}

    private final List<Material> materials = new ArrayList<>();
    private final List<Stage> stages = new ArrayList<>();
    private boolean preserveProperties = true, frozen;
    private Consumer<TransferContext> transfer;
    private String transferMode="";
    private net.minecraft.nbt.CompoundTag resultData=new net.minecraft.nbt.CompoundTag();
    /** Merge arbitrary block-entity data into the result after the transfer. */
    public UpgradeDefinition resultData(net.minecraft.nbt.CompoundTag data){editable();Objects.requireNonNull(data);for(String key:List.of("id","x","y","z"))if(data.contains(key))throw new IllegalArgumentException("Result data cannot replace block-entity identity: "+key);resultData=data.copy();return this;}
    public UpgradeDefinition resultData(String snbt){try{return resultData(net.minecraft.nbt.TagParser.parseTag(snbt));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException ex){throw new IllegalArgumentException("Invalid result data",ex);}}
    public net.minecraft.nbt.CompoundTag resultData(){return resultData.copy();}
    private String title="",description="";
    private net.minecraft.world.item.ItemStack display=net.minecraft.world.item.ItemStack.EMPTY;
    private double hudRange=6;
    private final Feedback completion=new Feedback().blockImpact(.075,9).sound("minecraft:block.anvil.use",.8F,1.15F).particles("minecraft:happy_villager",12,.4,.04);
    public UpgradeDefinition title(String value){editable();if(value.length()>160)throw new IllegalArgumentException("Title too long");title=value;return this;}
    public UpgradeDefinition description(String value){editable();if(value.length()>1024)throw new IllegalArgumentException("Description too long");description=value;return this;}
    public UpgradeDefinition displayItem(net.minecraft.world.item.ItemStack value){editable();display=value.copyWithCount(1);return this;}
    public UpgradeDefinition hudRange(double value){editable();if(!Double.isFinite(value)||value<1||value>32)throw new IllegalArgumentException("HUD range 1..32");hudRange=value;return this;}
    public UpgradeDefinition completionFeedback(Consumer<Feedback> configure){editable();configure.accept(completion);return this;}
    public Feedback completion(){return completion;}
    public String title(){return title.isEmpty()?(target!=null?target.getName():!outputs.isEmpty()?outputs.getFirst().getHoverName():source.getName()).getString():title;}
    public String description(){return description;}
    public double hudRange(){return hudRange;}
    public net.minecraft.world.item.ItemStack display(){return display.isEmpty()?(target!=null?target.asItem().getDefaultInstance():!outputs.isEmpty()?outputs.getFirst().copyWithCount(1):source.asItem().getDefaultInstance()):display.copy();}

    public UpgradeDefinition(String id) { this.id = ResourceLocation.parse(id).toString(); }
    private void editable() { if (frozen) throw new IllegalStateException("Definition is frozen"); }
    private Block blockId(String id) {
        var key = ResourceLocation.parse(id);
        if (!BuiltInRegistries.BLOCK.containsKey(key)) throw new IllegalArgumentException("Unknown block: " + id);
        var block = BuiltInRegistries.BLOCK.get(key);
        if (block.defaultBlockState().isAir()) throw new IllegalArgumentException("Air cannot be an upgrade endpoint");
        return block;
    }
    public UpgradeDefinition block(String id) { editable(); source = blockId(id); return this; }
    public UpgradeDefinition result(String id) { editable(); target = blockId(id); return this; }
    public UpgradeDefinition material(String item, int count) {
        return material(item,count,f -> {});
    }
    public UpgradeDefinition material(String item,int count,Consumer<Feedback> configure) {
        editable();positive(count);var f=new Feedback();configure.accept(f);f.freeze();materials.add(new Material(new StackMatcher(item),count,f));return this;
    }
    public UpgradeDefinition work(String item, int actions) { return stage("TOOL_ACTION", item, actions, 0, 1); }
    public UpgradeDefinition work(String item,int actions,Consumer<Feedback> configure) {return stage("TOOL_ACTION",item,actions,0,1,configure);}
    public UpgradeDefinition apply(String item) { return stage("ITEM_APPLICATION", item, 1, 1, 0); }
    public UpgradeDefinition apply(String item,Consumer<Feedback> configure) {return stage("ITEM_APPLICATION",item,1,1,0,configure);}
    public UpgradeDefinition stage(String type, String item, int actions, int consume, int damage) {
        return stage(type,item,actions,consume,damage,f -> {});
    }
    public UpgradeDefinition stage(String type, String item, int actions, int consume, int damage,Consumer<Feedback> configure) {
        editable(); positive(actions);
        if (!Upgrades.STAGE_TYPES.containsKey(type)) throw new IllegalArgumentException("Unknown stage type: " + type);
        if (consume < 0 || damage < 0 || consume > 64) throw new IllegalArgumentException("Invalid consume/damage");
        var f=new Feedback();configure.accept(f);f.freeze();stages.add(new Stage(type, new StackMatcher(item), actions, consume, damage,f)); return this;
    }
    public UpgradeDefinition preserveProperties(boolean preserve) { editable(); preserveProperties = preserve; return this; }
    public UpgradeDefinition transfer(Consumer<TransferContext> callback) { editable(); transfer = Objects.requireNonNull(callback); return this; }
    /** Built-in, data-pack-safe transfer policies. Custom Java/KubeJS callbacks remain available. */
    public UpgradeDefinition transferMode(String mode) {
        editable();
        if(!mode.equals("none")&&!mode.equals("copy_data")&&!mode.equals("copy_inventory")&&!mode.equals("sophisticated_storage"))throw new IllegalArgumentException("Unknown transfer mode: "+mode);
        transferMode=mode;
        transfer=switch(mode){case "none"->null;case "copy_data"->TransferContext::copyData;case "copy_inventory"->TransferContext::copyInventory;default->dev.jco.upgrades.integration.SophisticatedStorage::prepare;};
        return this;
    }
    public String transferMode(){return transferMode;}
    private static void positive(int n) { if (n < 1 || n > 1000000) throw new IllegalArgumentException("Count must be 1..1000000"); }
    public void freeze() {
        if (source == null || (target == null && outputs.isEmpty() && onComplete==null) || (source == target && !placedIncomplete) || (materials.isEmpty() && stages.isEmpty() && buildTime==0))
            throw new IllegalArgumentException(id + ": source/result and at least one material, stage or timer required");
        if(placedIncomplete && (source!=target || !outputs.isEmpty() || removeBlock || transfer!=null || !resultData.isEmpty()))
            throw new IllegalArgumentException(id + ": placed_incomplete requires identical source/result and no transfer or output");
        if (materials.size() > 64 || stages.size() > 64) throw new IllegalArgumentException("Maximum 64 materials/stages");
        if(target!=null&&(!outputs.isEmpty()||onComplete!=null||removeBlock))throw new IllegalArgumentException("Choose result block OR item-output completion");if(!accelerators.isEmpty()&&buildTime==0)throw new IllegalArgumentException("Accelerators require buildTime");if(target==null&&!resultData.isEmpty())throw new IllegalArgumentException("Result data requires a result block");completion.freeze();frozen = true;
    }
    public Block source() { return source; }
    public Block target() { return target; }
    public List<Material> materials() { return Collections.unmodifiableList(materials); }
    public List<Stage> stages() { return Collections.unmodifiableList(stages); }
    public boolean preserve() { return preserveProperties; }
    public Consumer<TransferContext> transfer() { return transfer; }
    public String fingerprint() {
        return source().toString()+":"+String.valueOf(target())+":"+materials.stream().map(m->"Material[item="+m.item()+", count="+m.count()+"]").toList()+":"+stages.stream().map(s->"Stage[type="+s.type()+", item="+s.item()+", actions="+s.actions()+", consume="+s.consume()+", damage="+s.damage()+"]").toList()+(buildTime>0?":timer:"+buildTime+":"+accelerators.stream().map(Accelerator::fingerprint).toList():"")+(itemOutput()?":"+outputs+":"+removeBlock+":"+(onComplete!=null):"")+(transferMode.isEmpty()?"":":transfer:"+transferMode)+(resultData.isEmpty()?"":":result_data:"+resultData)+(placedIncomplete?":placed_incomplete":"");
    }
}
