package dev.jco.upgrades.client;
import dev.jco.upgrades.ImpactPayload;
import net.minecraft.client.Minecraft;
public final class BlockImpact {
 public static void accept(ImpactPayload p){var mc=Minecraft.getInstance();if(mc.level==null||!mc.level.dimension().location().toString().equals(p.dimension())||!mc.level.hasChunkAt(p.pos())||!Float.isFinite(p.amplitude())||p.amplitude()<=0||p.amplitude()>.2F||p.ticks()<1||p.ticks()>40)return;
  ImpactPulse.accept(p);
 }
}
