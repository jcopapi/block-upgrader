package dev.jco.upgrades.kubejs;

import dev.jco.upgrades.*;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.*;
import java.util.*;
import java.util.function.Consumer;

public final class UpgradesPlugin implements KubeJSPlugin {
    private Map<String, UpgradeDefinition> candidate;
    private Set<String> disabled;
    private boolean invalid;
    private long generation;
    @Override public void beforeScriptsLoaded(ScriptManager m) {
        if(m.scriptType==ScriptType.SERVER) { candidate=new LinkedHashMap<>(); disabled=new LinkedHashSet<>(); invalid=false; generation++; }
    }
    @Override public void afterScriptsLoaded(ScriptManager m) {
        if(m.scriptType==ScriptType.SERVER) {
            if(!invalid && m.scriptType.console.errors.isEmpty()) Upgrades.replaceScripts(candidate,disabled);
            else m.scriptType.console.warn("Block Upgrader: reload failed; previous definitions retained");
            candidate=null;
            disabled=null;
        }
    }
    @Override public void registerBindings(BindingRegistry b) {
        if(b.type()==ScriptType.SERVER) b.add("BlockUpgrades", new Api(generation));
    }
    public final class Api {
        private final long current;
        private Api(long current) { this.current=current; }
        public void create(String id, Consumer<UpgradeDefinition> configure) {
            if(candidate==null || current!=generation) throw new IllegalStateException("create belongs at top level in server_scripts");
            try {
                var d=new UpgradeDefinition(id); configure.accept(d); d.freeze();
                if(candidate.putIfAbsent(d.id,d)!=null) throw new IllegalArgumentException("Duplicate upgrade: "+id);
            } catch(RuntimeException e) { invalid=true; throw e; }
        }
        /** Hide a bundled/datapack route without replacing its JSON file. */
        public void disable(String id) {
            if(disabled==null || current!=generation) throw new IllegalStateException("disable belongs at top level in server_scripts");
            disabled.add(net.minecraft.resources.ResourceLocation.parse(id).toString());
        }
        public java.util.List<String> ids() { return java.util.List.copyOf(Upgrades.definitions().keySet()); }
    }
}
