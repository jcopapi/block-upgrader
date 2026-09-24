package dev.jco.upgrades.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Small vanilla-rendered labels projected above the targeted block. */
@EventBusSubscriber(modid="jco_upgrades",value=Dist.CLIENT)
public final class WorldLabels {
    private static Matrix4f projection,view;
    private static Vec3 camera;
    @SubscribeEvent public static void capture(RenderLevelStageEvent e) {
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        projection=new Matrix4f(e.getProjectionMatrix());view=new Matrix4f(e.getModelViewMatrix());camera=e.getCamera().getPosition();
    }
    public static void draw(GuiGraphics gui,Vec3 world,net.minecraft.nbt.CompoundTag data) {
        var mc=Minecraft.getInstance();if(projection==null||view==null||camera==null||mc.level==null||mc.player==null||mc.options.hideGui)return;
        double distance=world.distanceTo(camera);if(distance>20)return;
        var ray=mc.level.clip(new ClipContext(camera,world,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player));
        if(ray.getType()!=net.minecraft.world.phys.HitResult.Type.MISS && ray.getLocation().distanceTo(camera)+.1<distance)return;
        Vec3 offset=world.subtract(camera);
        var clip=new Vector4f((float)offset.x,(float)offset.y,(float)offset.z,1).mul(view).mul(projection);
        if(clip.w<=0 || clip.z/clip.w>1 || clip.z/clip.w< -1)return;
        float x=(clip.x/clip.w+1)*gui.guiWidth()/2F,y=(1-clip.y/clip.w)*gui.guiHeight()/2F;
        if(x<0||x>gui.guiWidth()||y<0||y>gui.guiHeight())return;
        float scale=(float)Math.clamp(6/Math.max(6,distance),.45,1)*dev.jco.upgrades.UpgradeClientConfig.hudScale();
        gui.pose().pushPose();
        try {
            gui.pose().translate(x,y,0);gui.pose().scale(scale,scale,1);
            var rows=data.getList("rows",10);int yy=-12;
            for(var raw:rows){var row=(net.minecraft.nbt.CompoundTag)raw;if(row.getBoolean("material"))continue;var count=row.getInt("have")+"/"+row.getInt("need");int iconWidth="BOTH".equals(row.getString("input"))?24:13;int left=-(iconWidth+16+4+mc.font.width(count))/2;int width=InputIcons.draw(gui,row.getString("input"),left,yy+2);gui.renderItem(ItemStack.parseOptional(mc.level.registryAccess(),row.getCompound("icon")),left+width,yy);gui.drawString(mc.font,count,left+width+19,yy+4,0xFFE5CEAA,true);yy+=18;}
            if(data.contains("duration")){float progress=1-data.getLong("remaining")/(float)Math.max(1,data.getInt("duration"));gui.fill(-25,yy,25,yy+3,0xAA303942);gui.fill(-25,yy,-25+(int)(50*Math.clamp(progress,0,1)),yy+3,0xFFBFA46E);}

        } finally {gui.pose().popPose();}
    }
}
