package com.wjx.kablade.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collects interleaved procedural effect geometry, then submits one material at a
 * time to the caller's buffer source. No real consumer survives a getBuffer call
 * for another material, even when the caller reuses a single BufferBuilder.
 * Only POSITION_COLOR and POSITION_COLOR_TEX effects belong in this batch.
 */
public final class EffectVertexBatch implements MultiBufferSource {
    private final Map<RenderType, Collector> layers = new LinkedHashMap<>();

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        return layers.computeIfAbsent(type, key -> {
            boolean textured = key.format().equals(DefaultVertexFormat.POSITION_COLOR_TEX);
            if (!textured && !key.format().equals(DefaultVertexFormat.POSITION_COLOR)) {
                throw new IllegalArgumentException("Unsupported procedural effect format: " + key);
            }
            return new Collector(textured);
        });
    }

    /** Preserves first-requested material order and vertex order within each material. */
    public void submit(MultiBufferSource destination) {
        for (Map.Entry<RenderType, Collector> layer : layers.entrySet()) {
            Collector collector = layer.getValue();
            if (collector.size != 0) {
                collector.submit(destination.getBuffer(layer.getKey()));
            }
        }
        layers.clear();
    }

    static final class Collector implements VertexConsumer {
        private static final int STRIDE = 9;
        private final boolean textured;
        private float[] vertices = new float[STRIDE * 256];
        private int size;
        private float x, y, z, u, v;
        private int red = 255, green = 255, blue = 255, alpha = 255;
        private boolean defaultColor;

        Collector(boolean textured) {
            this.textured = textured;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            this.x = (float) x;
            this.y = (float) y;
            this.z = (float) z;
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            if (defaultColor) {
                throw new IllegalStateException("Cannot override default vertex color");
            }
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.alpha = alpha;
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            throw new UnsupportedOperationException("Effect batches do not contain overlay coordinates");
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            throw new UnsupportedOperationException("Effect batches do not contain lightmap coordinates");
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            throw new UnsupportedOperationException("Effect batches do not contain normals");
        }

        @Override
        public void endVertex() {
            if (size + STRIDE > vertices.length) {
                vertices = Arrays.copyOf(vertices, vertices.length * 2);
            }
            vertices[size++] = x;
            vertices[size++] = y;
            vertices[size++] = z;
            vertices[size++] = red;
            vertices[size++] = green;
            vertices[size++] = blue;
            vertices[size++] = alpha;
            vertices[size++] = u;
            vertices[size++] = v;
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            defaultColor = false;
            color(red, green, blue, alpha);
            defaultColor = true;
        }

        @Override
        public void unsetDefaultColor() {
            defaultColor = false;
        }

        void submit(VertexConsumer output) {
            for (int i = 0; i < size; i += STRIDE) {
                VertexConsumer vertex = output.vertex(vertices[i], vertices[i + 1], vertices[i + 2])
                        .color((int) vertices[i + 3], (int) vertices[i + 4],
                                (int) vertices[i + 5], (int) vertices[i + 6]);
                if (textured) {
                    vertex = vertex.uv(vertices[i + 7], vertices[i + 8]);
                }
                vertex.endVertex();
            }
        }
    }
}
