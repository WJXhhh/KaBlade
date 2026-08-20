package com.wjx.kablade.client.model;

import mods.flammpfeil.slashblade.client.model.obj.GroupObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 允许刀模通过同目录的 {@code *_luminous.png} 贴图追加全亮遮罩层，无需复制 OBJ 分组。
 *
 * <p>例如刀的基础贴图为 {@code model/example/tex.png} 时，只需添加
 * {@code model/example/tex_luminous.png}。SlashBlade 绘制 {@code blade_luminous}、
 * {@code sheath_luminous} 或物品栏对应分组时，本类会改为使用发光贴图重绘基础分组。</p>
 */
@SideOnly(Side.CLIENT)
public final class BladeLuminousTextureLayer implements IResourceManagerReloadListener {
    public static final BladeLuminousTextureLayer INSTANCE = new BladeLuminousTextureLayer();

    private static final String LUMINOUS_GROUP_SUFFIX = "_luminous";
    private static final String PNG_EXTENSION = ".png";
    private static final String LUMINOUS_TEXTURE_SUFFIX = "_luminous.png";

    private static final Field TEXTURE_OBJECTS_FIELD = findTextureObjectsField();

    /** 缺失结果也缓存，避免绝大多数没有发光遮罩的刀每帧触发资源包查找。 */
    private final Map<ResourceLocation, Optional<ResourceLocation>> luminousTextures =
            new ConcurrentHashMap<>();
    /** GL 纹理 ID 在一次资源加载周期内稳定，反查一次后即可直接复用。 */
    private final Map<Integer, ResourceLocation> boundTextures = new ConcurrentHashMap<>();

    private BladeLuminousTextureLayer() {
    }

    public static String getBaseTarget(String luminousTarget) {
        if (luminousTarget == null
                || luminousTarget.length() <= LUMINOUS_GROUP_SUFFIX.length()
                || !luminousTarget.regionMatches(true,
                luminousTarget.length() - LUMINOUS_GROUP_SUFFIX.length(),
                LUMINOUS_GROUP_SUFFIX, 0, LUMINOUS_GROUP_SUFFIX.length())) {
            return null;
        }
        return luminousTarget.substring(0,
                luminousTarget.length() - LUMINOUS_GROUP_SUFFIX.length());
    }

    /**
     * @return true 表示已完成贴图遮罩绘制，调用方应取消原始 {@code *_luminous} 分组绘制。
     */
    public boolean tryRender(List<GroupObject> baseGroups) {
        if (!hasRenderableGroup(baseGroups)) {
            return false;
        }

        TextureManager textureManager = Minecraft.getMinecraft().getTextureManager();
        ResourceLocation baseTexture = findBoundTexture(textureManager);
        if (baseTexture == null) {
            return false;
        }

        Optional<ResourceLocation> luminousTexture = findLuminousTexture(baseTexture);
        if (!luminousTexture.isPresent()) {
            return false;
        }

        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glPushAttrib(GL11.GL_POLYGON_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        try {
            // 基础模型与遮罩共面。使用 raster depth bias，避免第三人称和掉落物视角下闪烁。
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(-1.0F, -10.0F);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDepthMask(false);

            textureManager.bindTexture(luminousTexture.get());
            for (GroupObject group : baseGroups) {
                if (group != null && group.faces != null && !group.faces.isEmpty()) {
                    group.render();
                }
            }
        } finally {
            try {
                // 必须恢复基础贴图，否则同一刀模后续的鞘、附魔光效会串贴图。
                textureManager.bindTexture(baseTexture);
            } finally {
                // push/pop 仅修改原生 GL 状态，不触碰 GlStateManager 缓存，避免状态缓存失步。
                GL11.glPopAttrib();
                GlStateManager.setActiveTexture(activeTexture);
            }
        }
        return true;
    }

    private ResourceLocation findBoundTexture(TextureManager textureManager) {
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int textureId;
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        try {
            textureId = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        } finally {
            GlStateManager.setActiveTexture(activeTexture);
        }
        if (textureId <= 0) {
            return null;
        }

        ResourceLocation cached = boundTextures.get(textureId);
        if (cached != null) {
            ITextureObject cachedTexture = textureManager.getTexture(cached);
            if (cachedTexture != null && cachedTexture.getGlTextureId() == textureId) {
                return cached;
            }
            boundTextures.remove(textureId);
        }

        Map<ResourceLocation, ITextureObject> textureObjects = getTextureObjects(textureManager);
        if (textureObjects == null) {
            return null;
        }
        for (Map.Entry<ResourceLocation, ITextureObject> entry : textureObjects.entrySet()) {
            ITextureObject texture = entry.getValue();
            if (texture != null && texture.getGlTextureId() == textureId) {
                boundTextures.put(textureId, entry.getKey());
                return entry.getKey();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<ResourceLocation, ITextureObject> getTextureObjects(TextureManager textureManager) {
        if (TEXTURE_OBJECTS_FIELD == null) {
            return null;
        }
        try {
            return (Map<ResourceLocation, ITextureObject>) TEXTURE_OBJECTS_FIELD.get(textureManager);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private static Field findTextureObjectsField() {
        String[] names = {"mapTextureObjects", "field_110585_a"};
        for (String name : names) {
            try {
                Field field = TextureManager.class.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException | SecurityException ignored) {
                // 开发环境使用 MCP 名，混淆后的 Forge 环境使用 SRG 名。
            }
        }
        return null;
    }

    private Optional<ResourceLocation> findLuminousTexture(ResourceLocation baseTexture) {
        return luminousTextures.computeIfAbsent(baseTexture, texture -> {
            String path = texture.getPath();
            if (!path.endsWith(PNG_EXTENSION) || path.endsWith(LUMINOUS_TEXTURE_SUFFIX)) {
                return Optional.empty();
            }

            String luminousPath = path.substring(0, path.length() - PNG_EXTENSION.length())
                    + LUMINOUS_TEXTURE_SUFFIX;
            ResourceLocation luminous = new ResourceLocation(texture.getNamespace(), luminousPath);
            return resourceExists(luminous) ? Optional.of(luminous) : Optional.empty();
        });
    }

    private static boolean resourceExists(ResourceLocation location) {
        IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
        if (resourceManager == null) {
            return false;
        }
        try {
            IResource resource = resourceManager.getResource(location);
            try (InputStream ignored = resource.getInputStream()) {
                return true;
            }
        } catch (IOException ignored) {
            return false;
        }
    }

    private static boolean hasRenderableGroup(List<GroupObject> groups) {
        if (groups == null || groups.isEmpty()) {
            return false;
        }
        for (GroupObject group : groups) {
            if (group != null && group.faces != null && !group.faces.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        luminousTextures.clear();
        boundTextures.clear();
    }
}
