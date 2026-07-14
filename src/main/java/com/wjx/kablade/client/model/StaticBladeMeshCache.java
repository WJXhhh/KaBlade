package com.wjx.kablade.client.model;

import com.wjx.kablade.config.ModConfig;
import mods.flammpfeil.slashblade.client.model.obj.Face;
import mods.flammpfeil.slashblade.client.model.obj.GroupObject;
import mods.flammpfeil.slashblade.client.model.obj.TextureCoordinate;
import mods.flammpfeil.slashblade.client.model.obj.Vertex;
import mods.flammpfeil.slashblade.client.model.obj.WavefrontObject;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 将 SlashBlade 的静态 OBJ 部件上传到 VBO，避免每帧重复遍历 Face 并提交顶点。
 */
public final class StaticBladeMeshCache {
    private static final long MAX_CACHE_BYTES = 96L * 1024L * 1024L;
    private static final int VERTEX_STRIDE = DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL.getSize();
    private static final int ENCHANT_COLOR = 0xFF8040CC;
    private static final float TEXTURE_OFFSET = 0.0005F;

    private static final Map<MeshKey, MeshEntry> CACHE =
            new LinkedHashMap<MeshKey, MeshEntry>(128, 0.75F, true);
    private static long cachedBytes;

    private StaticBladeMeshCache() {
    }

    /**
     * 返回 true 表示已经使用 VBO 完成绘制，调用方应取消原始 Tessellator 路径。
     */
    public static synchronized boolean render(GroupObject group) {
        if (!ModConfig.GeneralConf.EnableSlashBladeModelWarmup
                || !OpenGlHelper.useVbo() || group == null || group.faces == null || group.faces.isEmpty()) {
            return false;
        }

        MeshEntry entry = getOrCreate(group, Face.defaultColor);
        if (entry == null) {
            return false;
        }

        entry.buffer.bindBuffer();
        try {
            GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glEnableClientState(GL11.GL_COLOR_ARRAY);
            GL11.glEnableClientState(GL11.GL_NORMAL_ARRAY);

            GL11.glVertexPointer(3, GL11.GL_FLOAT, VERTEX_STRIDE, 0L);
            GL11.glTexCoordPointer(2, GL11.GL_FLOAT, VERTEX_STRIDE, 12L);
            GL11.glColorPointer(4, GL11.GL_UNSIGNED_BYTE, VERTEX_STRIDE, 20L);
            GL11.glNormalPointer(GL11.GL_BYTE, VERTEX_STRIDE, 24L);

            entry.buffer.drawArrays(group.glDrawingMode);
        } finally {
            GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
            GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
            GlStateManager.resetColor();
            OpenGlHelper.setClientActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glDisableClientState(GL11.GL_TEXTURE_COORD_ARRAY);
            GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY);
            entry.buffer.unbindBuffer();
        }
        return true;
    }

    /**
     * 预建物品栏最常用的部件。白色是普通渲染，紫色是附魔 glint 的第二遍渲染。
     */
    public static synchronized int prewarmGuiParts(WavefrontObject model) {
        if (!ModConfig.GeneralConf.EnableSlashBladeModelWarmup
                || !OpenGlHelper.useVbo() || model == null || model.groupObjects == null) {
            return 0;
        }

        int built = 0;
        for (GroupObject group : model.groupObjects) {
            if (group == null || !isGuiPart(group.name)) {
                continue;
            }
            if (getOrCreate(group, 0xFFFFFFFF) != null) {
                built++;
            }
            if (!isLuminousPart(group.name) && getOrCreate(group, ENCHANT_COLOR) != null) {
                built++;
            }
        }
        return built;
    }

    public static synchronized void clear() {
        for (MeshEntry entry : CACHE.values()) {
            entry.buffer.deleteGlBuffers();
        }
        CACHE.clear();
        cachedBytes = 0L;
    }

    public static synchronized long getCachedBytes() {
        return cachedBytes;
    }

    private static boolean isGuiPart(String name) {
        if (name == null) {
            return false;
        }
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.equals("item_blade")
                || normalized.equals("item_bladens")
                || normalized.equals("item_damaged")
                || normalized.equals("item_blade_luminous")
                || normalized.equals("item_bladens_luminous")
                || normalized.equals("item_damaged_luminous");
    }

    private static boolean isLuminousPart(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).endsWith("_luminous");
    }

    private static MeshEntry getOrCreate(GroupObject group, int color) {
        MeshKey key = new MeshKey(group, color);
        MeshEntry cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        MeshEntry created = build(group, color);
        if (created == null) {
            return null;
        }
        if (created.bytes > MAX_CACHE_BYTES) {
            created.buffer.deleteGlBuffers();
            return null;
        }

        CACHE.put(key, created);
        cachedBytes += created.bytes;
        trimToLimit();
        return CACHE.get(key);
    }

    private static MeshEntry build(GroupObject group, int color) {
        int vertexCount = 0;
        for (Face face : group.faces) {
            if (face != null && face.vertices != null) {
                vertexCount += face.vertices.length;
            }
        }
        if (vertexCount == 0) {
            return null;
        }

        BufferBuilder builder = new BufferBuilder(Math.max(256, vertexCount * VERTEX_STRIDE));
        builder.begin(group.glDrawingMode, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);

        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;
        int a = color >>> 24 & 255;

        for (Face face : group.faces) {
            appendFace(builder, face, r, g, b, a);
        }
        builder.finishDrawing();

        ByteBuffer data = builder.getByteBuffer();
        int bytes = builder.getVertexCount() * VERTEX_STRIDE;
        data.position(0);
        data.limit(bytes);

        VertexBuffer buffer = new VertexBuffer(DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
        try {
            buffer.bufferData(data);
            return new MeshEntry(buffer, bytes);
        } catch (RuntimeException ex) {
            buffer.deleteGlBuffers();
            return null;
        }
    }

    private static void appendFace(BufferBuilder builder, Face face, int r, int g, int b, int a) {
        if (face == null || face.vertices == null || face.vertices.length == 0) {
            return;
        }
        if (face.faceNormal == null) {
            face.faceNormal = face.calculateFaceNormal();
        }

        TextureCoordinate[] coordinates = face.textureCoordinates;
        int coordinateCount = coordinates == null ? 0 : coordinates.length;
        float averageU = 0.0F;
        float averageV = 0.0F;
        for (int i = 0; i < coordinateCount; i++) {
            averageU += coordinates[i].u;
            averageV += coordinates[i].v;
        }
        if (coordinateCount > 0) {
            averageU /= coordinateCount;
            averageV /= coordinateCount;
        }

        for (int i = 0; i < face.vertices.length; i++) {
            Vertex vertex = face.vertices[i];
            builder.pos(vertex.x, vertex.y, vertex.z);

            if (i < coordinateCount) {
                TextureCoordinate coordinate = coordinates[i];
                float offsetU = coordinate.u > averageU ? -TEXTURE_OFFSET : TEXTURE_OFFSET;
                float offsetV = coordinate.v > averageV ? -TEXTURE_OFFSET : TEXTURE_OFFSET;
                builder.tex(coordinate.u + offsetU, coordinate.v + offsetV);
            } else {
                builder.tex(0.0D, 0.0D);
            }
            builder.color(r, g, b, a);

            if (face.vertexNormals != null && i < face.vertexNormals.length) {
                Vertex normal = face.vertexNormals[i];
                builder.normal(normal.x * -1.05F, normal.y * -1.05F, normal.z * -1.05F);
            } else {
                builder.normal(face.faceNormal.x, face.faceNormal.y, face.faceNormal.z);
            }
            builder.endVertex();
        }
    }

    private static void trimToLimit() {
        Iterator<Map.Entry<MeshKey, MeshEntry>> iterator = CACHE.entrySet().iterator();
        while (cachedBytes > MAX_CACHE_BYTES && iterator.hasNext()) {
            MeshEntry entry = iterator.next().getValue();
            cachedBytes -= entry.bytes;
            entry.buffer.deleteGlBuffers();
            iterator.remove();
        }
    }

    private static final class MeshKey {
        private final GroupObject group;
        private final int color;

        private MeshKey(GroupObject group, int color) {
            this.group = group;
            this.color = color;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MeshKey)) {
                return false;
            }
            MeshKey other = (MeshKey) obj;
            return group == other.group && color == other.color;
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(group) + color;
        }
    }

    private static final class MeshEntry {
        private final VertexBuffer buffer;
        private final int bytes;

        private MeshEntry(VertexBuffer buffer, int bytes) {
            this.buffer = buffer;
            this.bytes = bytes;
        }
    }
}
