package com.anantaya.smartcgolem.client;

/** Linear interpolation of the 5.05s Blockbench keyframes from the user.
 * All coordinates are Bedrock-style model units; convert Y sign for Java.
 */
public final class StretchKeyframes {
    private StretchKeyframes() {}
    public record Pose(float bodyY, float bodyScale, float legY, float legScale) {}
    private static final float[] TIMES = {0f, 0.4f, 4.2f, 5f};
    private static final float[] BODY_Y = {0f, 9f, 44f, 64f};
    private static final float[] BODY_SCALE = {1f, .8f, .8f, 1f};
    private static final float[] LEG_Y = {0f, 9f, 44f, 64f};
    private static final float[] LEG_SCALE = {1f, 2.8f, 9.8f, 13.8f};
    private static float interpolate(float[] a, float time) {
        if (time <= TIMES[0]) return a[0];
        for (int i=1;i<TIMES.length;i++) {
            if (time <= TIMES[i]) {
                float part = (time - TIMES[i-1])/(TIMES[i]-TIMES[i-1]);
                return a[i-1] + (a[i]-a[i-1])*part;
            }
        }
        return a[a.length-1];
    }
    public static Pose sample(float normalized) {
        float t = Math.max(0f, Math.min(1f, normalized))*5.05f;
        return new Pose(interpolate(BODY_Y,t),interpolate(BODY_SCALE,t),
                interpolate(LEG_Y,t),interpolate(LEG_SCALE,t));
    }
}
