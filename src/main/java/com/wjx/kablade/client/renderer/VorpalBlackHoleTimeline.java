package com.wjx.kablade.client.renderer;

import net.minecraft.util.math.MathHelper;

/**
 * 连续 30 FPS 时间线函数库。
 * <p>
 * 全生命周期（0 ~ 56 tick = 0 ~ 84 帧）无断层动画设计：
 * - 阶段一（0 ~ 24 帧）：虚空撕裂蓄力、种子奇点与空间吸积流旋转；
 * - 过渡期（24 ~ 28.5 帧）：能量过载蓄积、高斯脉冲形变与红黑突刺激射；
 * - 阶段二（28.5 ~ 42 帧）：横向空间斩裂、玫红贯穿光束、胸口冲击波与连环震波环；
 * - 阶段三（40 ~ 84 帧）：6 次能量脉冲共振、虚空细裂隙与终末坍缩大爆炸。
 */
public final class VorpalBlackHoleTimeline {

    public static final float REFERENCE_FPS = 30.0F;
    public static final float FRAMES_PER_TICK = REFERENCE_FPS / 20.0F; // 1.5F
    public static final float LAST_FRAME = 84.0F; // 对应 56 tick
    public static final float TWO_PI = (float) (Math.PI * 2.0);

    private VorpalBlackHoleTimeline() {
    }

    public static float frame(float ageTicks) {
        return MathHelper.clamp(ageTicks * FRAMES_PER_TICK, 0.0F, LAST_FRAME);
    }

    public static float smooth(float edge0, float edge1, float value) {
        if (edge0 == edge1) {
            return value < edge0 ? 0.0F : 1.0F;
        }
        float t = MathHelper.clamp((value - edge0) / (edge1 - edge0), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    public static float plateau(float frame, float start, float riseEnd, float holdEnd, float end) {
        if (frame < start || frame > end) {
            return 0.0F;
        }
        if (frame < riseEnd) {
            return smooth(start, riseEnd, frame);
        }
        if (frame <= holdEnd) {
            return 1.0F;
        }
        return 1.0F - smooth(holdEnd, end, frame);
    }

    public static float gaussian(float frame, float center, float sigma) {
        float x = (frame - center) / sigma;
        return (float) Math.exp(-0.5F * x * x);
    }

    public static float easeOutBack(float t) {
        t = MathHelper.clamp(t, 0.0F, 1.0F) - 1.0F;
        float c1 = 1.70158F;
        return 1.0F + (c1 + 1.0F) * t * t * t + c1 * t * t;
    }

    public static float lerp(float t, float a, float b) {
        return a + (b - a) * t;
    }

    // ── 核心视界与吸积体积（全程无断层保持） ──

    public static float vortexCore(float f) {
        if (f < 19.7F) {
            return 0.45F + smooth(0.0F, 19.7F, f) * 0.55F;
        }
        if (f <= 78.0F) {
            return 1.0F;
        }
        return 1.0F - smooth(78.0F, 84.0F, f);
    }

    public static float vortexSeed(float f) {
        if (f < 19.7F) {
            return 0.38F + smooth(0.0F, 19.7F, f) * 0.42F;
        }
        if (f < 24.0F) {
            return lerp(smooth(19.7F, 24.0F, f), 0.80F, 1.0F);
        }
        return 0.0F;
    }

    public static float vortexSpiral(float f) {
        if (f < 19.8F) {
            return smooth(2.0F, 19.8F, f);
        }
        if (f <= 78.0F) {
            return 1.0F;
        }
        return 1.0F - smooth(78.0F, 84.0F, f);
    }

    public static float vortexArcs(float f) {
        if (f < 14.0F) {
            return smooth(6.0F, 14.0F, f) * 0.65F;
        }
        if (f <= 76.0F) {
            return 0.85F + MathHelper.sin(f * 0.35F) * 0.15F;
        }
        return (1.0F - smooth(76.0F, 84.0F, f)) * 0.85F;
    }

    public static float vortexNeedles(float f) {
        if (f < 16.0F) {
            return smooth(4.0F, 16.0F, f) * 0.55F;
        }
        if (f <= 76.0F) {
            return 1.0F;
        }
        return 1.0F - smooth(76.0F, 84.0F, f);
    }

    public static float vortexSmoke(float f) {
        if (f < 16.0F) {
            return smooth(0.0F, 16.0F, f) * 0.75F;
        }
        if (f <= 78.0F) {
            return 1.0F;
        }
        return 1.0F - smooth(78.0F, 84.0F, f);
    }

    public static float vortexEmbers(float f) {
        if (f < 16.0F) {
            return smooth(2.0F, 16.0F, f) * 0.6F;
        }
        if (f <= 80.0F) {
            return 1.0F;
        }
        return 1.0F - smooth(80.0F, 84.0F, f);
    }

    // ── 阶段斩击与大爆发飘带 ──

    public static float entryRibbon(float f) {
        return plateau(f, 14.0F, 18.2F, 24.2F, 28.4F);
    }

    public static float weaponTrail(float f) {
        return Math.max(plateau(f, 12.0F, 17.0F, 25.0F, 29.0F),
                plateau(f, 28.0F, 30.0F, 70.0F, 78.0F) * 0.88F);
    }

    public static float preFlash(float f) {
        return plateau(f, 25.0F, 27.2F, 29.6F, 31.0F);
    }

    public static float redSpikes(float f) {
        return plateau(f, 27.5F, 29.3F, 34.0F, 38.0F);
    }

    public static float blackSpikes(float f) {
        return plateau(f, 27.2F, 29.2F, 34.5F, 38.5F);
    }

    public static float impactCore(float f) {
        return plateau(f, 28.0F, 29.4F, 35.0F, 40.0F);
    }

    public static float shockwave(float f) {
        return plateau(f, 28.5F, 29.5F, 36.0F, 42.0F);
    }

    public static float horizontalSever(float f) {
        return plateau(f, 27.5F, 29.7F, 35.0F, 38.5F);
    }

    public static float magentaBeam(float f) {
        return plateau(f, 29.5F, 31.4F, 36.0F, 40.0F);
    }

    public static float speedStreaks(float f) {
        return plateau(f, 31.0F, 34.0F, 42.0F, 48.0F);
    }

    public static float followupCrescent(float f) {
        return plateau(f, 32.0F, 34.8F, 44.0F, 50.0F);
    }

    public static float thinCuts(float f) {
        return plateau(f, 32.0F, 35.0F, 76.0F, 82.0F);
    }

    public static float debris(float f) {
        return plateau(f, 28.0F, 30.5F, 76.0F, 84.0F);
    }

    // ── 6 次能量脉冲波（Frame 36, 42, 48, 54, 60, 66） ──

    public static float pulseWave(float frame, int pulseIndex) {
        float center = 36.0F + pulseIndex * 6.0F;
        return gaussian(frame, center, 1.45F);
    }

    public static float cameraImpact(float f) {
        float baseImpact = gaussian(f, 29.25F, 0.48F) * 0.72F
                + gaussian(f, 31.1F, 0.95F) * 0.55F
                + gaussian(f, 33.0F, 1.35F) * 0.24F;
        for (int p = 0; p < 6; p++) {
            baseImpact += pulseWave(f, p) * (0.28F + p * 0.04F);
        }
        return MathHelper.clamp(baseImpact, 0.0F, 1.0F);
    }
}
