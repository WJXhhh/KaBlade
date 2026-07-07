package com.wjx.kablade.Entity.Render;

import com.wjx.kablade.Entity.EntitySlashDimensionAdd;
import net.minecraft.client.renderer.entity.Render;
import com.google.common.collect.Maps;
import mods.flammpfeil.slashblade.ItemSlashBladeWrapper;
import mods.flammpfeil.slashblade.client.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.model.obj.Face;
import mods.flammpfeil.slashblade.client.model.obj.GroupObject;
import mods.flammpfeil.slashblade.client.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.entity.EntityBladeStand;
import mods.flammpfeil.slashblade.entity.EntitySlashDimension;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import mods.flammpfeil.slashblade.util.ResourceLocationRaw;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import java.awt.*;
import java.util.EnumSet;
import java.util.Map;

public class RenderSlashDimensionAdd extends Render<Entity> {
    static public WavefrontObject model = null;

    static public ResourceLocationRaw modelLocation = new ResourceLocationRaw("flammpfeil.slashblade","model/util/slashdim.obj");
    static public ResourceLocationRaw textureLocation = new ResourceLocationRaw("flammpfeil.slashblade","model/util/slashdim.png");

    private String lastModelStr;
    private String lastTextureStr;

    public RenderSlashDimensionAdd(RenderManager renderManager) {
        super(renderManager);
    }

    private TextureManager engine(){
        return this.renderManager.renderEngine;
    }

    @Override
    public void doRender(Entity entity, double x, double y, double z, float yaw, float partialRenderTick) {
        EntitySlashDimensionAdd dim = (EntitySlashDimensionAdd) entity;
        String modelStr = dim.getModel();
        String textureStr = dim.getTexture();
        if (!modelStr.equals(lastModelStr)) {
            modelLocation = new ResourceLocationRaw("flammpfeil.slashblade", modelStr);
            lastModelStr = modelStr;
        }
        if (!textureStr.equals(lastTextureStr)) {
            textureLocation = new ResourceLocationRaw("flammpfeil.slashblade", textureStr);
            lastTextureStr = textureStr;
        }
        if(renderOutlines){
            GlStateManager.disableLighting();
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.disableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);

            GlStateManager.enableColorMaterial();
            float cycleTicks = 40.0f;
            float b = Math.abs((entity.ticksExisted % cycleTicks) / cycleTicks - 0.5f) + 0.5f;
            GlStateManager.enableOutlineMode(Color.getHSBColor(0, 0.0f,b).getRGB());
        }

        renderModel(entity, x, y, z, yaw, partialRenderTick);

        if(renderOutlines){
            GlStateManager.disableOutlineMode();
            GlStateManager.disableColorMaterial();

            GlStateManager.enableLighting();
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.enableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        }

    }
    public void renderModel(Entity entity, double x, double y, double z, float yaw, float partialRenderTick) {
        if(model == null){
            model = new WavefrontObject(modelLocation);
        }

        this.bindEntityTexture(entity);

        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_TEXTURE_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_COLOR_BUFFER_BIT);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        //GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);


        int color = 0x5555FF;

        int lifetime = 20;

        if(entity instanceof EntitySlashDimensionAdd) {
            color = ((EntitySlashDimensionAdd) entity).getColor();
            lifetime = ((EntitySlashDimensionAdd)entity).getLifeTime();
        }

        boolean inverse = color < 0;

        double deathTime = lifetime;
        double baseAlpha = Math.sin(Math.PI * 0.5 * (Math.min(deathTime, (lifetime - (entity.ticksExisted) - partialRenderTick)) / deathTime));
        int seed = ((EntitySlashDimensionAdd) entity).particleSeed;

        int baseColor = color;
        // 纯算术替换 AWT Color: RGB → HSB 色相加0.5，饱和不变，亮度0.2 → RGB
        float r = ((baseColor >> 16) & 0xFF) / 255f;
        float g = ((baseColor >> 8) & 0xFF) / 255f;
        float b = (baseColor & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float h;
        if (delta == 0f) {
            h = 0f;
        } else if (max == r) {
            h = ((g - b) / delta) % 6f;
        } else if (max == g) {
            h = (b - r) / delta + 2f;
        } else {
            h = (r - g) / delta + 4f;
        }
        h /= 6f;
        if (h < 0) h += 1f;
        // 色相加0.5，饱和度保持，亮度固定0.2
        float newH = (0.5f + h) % 1f;
        int baseColorRGB = java.awt.Color.HSBtoRGB(newH, delta / max, 0.2f) & 0xFFFFFF;
        baseColor = baseColorRGB | (int)(0x66 * baseAlpha) << 24;

        GL11.glTranslatef((float) x, (float) y, (float) z);

        float rotParTicks = 40.0f;
        float rot = ((entity.ticksExisted % rotParTicks) / rotParTicks) * 360.f + partialRenderTick * (360.0f / rotParTicks);

        //GL11.glRotatef(rot, 0, 1, 0);

        float scale = 0.01f;
        GL11.glScalef(scale, scale, scale);


        float lastx = OpenGlHelper.lastBrightnessX;
        float lasty = OpenGlHelper.lastBrightnessY;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);

        Face.setColor(baseColor);

        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.glBlendEquation(GL14.GL_FUNC_REVERSE_SUBTRACT);
        //GlStateManager.glBlendEquation(GL14.GL_FUNC_SUBTRACT);

        GL11.glPushMatrix();
        model.renderPart("base");
        GL11.glScaled(0.9, 0.9, 0.9);
        model.renderPart("base");
        GL11.glPopMatrix();


        int loop = 2;
        for(int i=0; i<loop; i++) {
            GL11.glPushMatrix();
            float ticks = 15;
            float wave = (entity.ticksExisted + (ticks / (float)loop * i) + partialRenderTick) % ticks;
            double waveScale = 1.0 + 0.03 * wave;
            GL11.glScaled(waveScale, waveScale, waveScale);
            Face.setColor((baseColor & 0xFFFFFF) | (int) (0x88 * ((ticks - wave) / ticks)) << 24);
            model.renderPart("base");
            GL11.glPopMatrix();
        }

        GlStateManager.glBlendEquation(GL14.GL_FUNC_ADD);


        int windCount = 5;
        for(int i = 0; i < windCount; i++){
            GL11.glPushMatrix();

            GL11.glRotated((360.0 / windCount) * i, 1, 0, 0);
            GL11.glRotated(30.0f , 0, 1, 0);

            double rotWind = 360.0 / 20.0;

            double offsetBase = 7;

            double offset = i * offsetBase;

            double motionLen = offsetBase * (windCount - 1);

            double ticks = entity.ticksExisted + partialRenderTick + seed;
            double offsetTicks = ticks + offset;
            double progress = (offsetTicks % motionLen) / motionLen;

            double rad = (Math.PI) * 2.0;
            rad *= progress;

            Face.setColor(color & 0xFFFFFF | (int)(Math.min(0,0xFF * Math.sin(rad))) << 24);

            double windScale = 0.4 + progress;
            GL11.glScaled(windScale,windScale,windScale);

            GL11.glRotated(rotWind * offsetTicks, 0, 0, 1);
            model.renderPart("wind");

            GL11.glPopMatrix();
        }

        Face.resetColor();

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastx, lasty);


        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LIGHTING);
        //GL11.glEnable(GL11.GL_TEXTURE_2D);

        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }

    @Override
    protected ResourceLocationRaw getEntityTexture(Entity p_110775_1_) {
        return textureLocation;
    }
}
