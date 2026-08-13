/*
 * HackAI 开源项目
 * 开源作者联系方式：
 * QQ：1513583976
 * 邮箱：atlasca3@gmail.com
 */

package com.hack.ai.arraylist.animation;

public enum Easing {
    LINEAR {
        @Override
        public float transform(float progress) { return progress; }
    },
    EASE_OUT_SINE {
        @Override
        public float transform(float progress) {
            return (float) Math.sin((progress * Math.PI) / 2.0d);
        }
    },
    EASE_OUT_CUBIC {
        @Override
        public float transform(float progress) {
            float inverse = 1.0f - progress;
            return 1.0f - inverse * inverse * inverse;
        }
    },
    EASE_IN_OUT_CUBIC {
        @Override
        public float transform(float progress) {
            if (progress < 0.5f) return 4.0f * progress * progress * progress;
            float shifted = -2.0f * progress + 2.0f;
            return 1.0f - shifted * shifted * shifted / 2.0f;
        }
    },
    EASE_IN_OUT_QUINT {
        @Override
        public float transform(float progress) {
            if (progress < 0.5f)
                return 16.0f * progress * progress * progress * progress * progress;
            float shifted = -2.0f * progress + 2.0f;
            return 1.0f - shifted * shifted * shifted * shifted * shifted / 2.0f;
        }
    },
    EASE_OUT_EXPO {
        @Override
        public float transform(float progress) {
            if (progress >= 1.0f) return 1.0f;
            return (float) (1.0d - Math.pow(2.0d, -10.0d * progress));
        }
    },
    EASE_OUT_BACK {
        @Override
        public float transform(float progress) {
            float c1 = 1.70158f;
            float c3 = c1 + 1f;
            float t = progress - 1f;
            return t * t * (c3 * t + c1) + 1f;
        }
    },
    EASE_OUT_ELASTIC {
        @Override
        public float transform(float progress) {
            if (progress == 0.0f || progress == 1.0f) return progress;
            double period = (2.0d * Math.PI) / 3.0d;
            return (float) (Math.pow(2.0d, -10.0f * progress)
                    * Math.sin((progress * 10.0f - 0.75f) * period) + 1.0d);
        }
    },
    DECELERATE {
        @Override
        public float transform(float progress) {
            float inverse = 1.0f - progress;
            return 1.0f - inverse * inverse;
        }
    };

    public abstract float transform(float progress);

    public float apply(float progress) {
        return transform(clamp(progress));
    }

    private static float clamp(float value) {
        if (value < 0.0f) return 0.0f;
        if (value > 1.0f) return 1.0f;
        return value;
    }
}
