package com.neroferno.krm_revo.client.vfx;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Client-side camera shake system.
 *
 * Called by {@link TransformVFXHelper} when the player transforms or lands a
 * heavy unarmed hit. Decays smoothly over time so it doesn't feel jarring.
 *
 * Approach: We add a sinusoidal offset to the view angles each tick, with
 * intensity decaying by a configurable factor. No Lodestone dependency needed.
 */
@EventBusSubscriber(modid = "krm_revo", value = Dist.CLIENT)
public class ScreenShakeHandler {

    // â”€â”€â”€ State â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Current shake intensity (0 = none). */
    private static float intensity = 0f;

    /** How fast the shake decays each tick (0-1). 0.85 = 85% retained per tick. */
    private static final float DECAY = 0.82f;

    /** Minimum intensity before we snap to zero (avoids infinite micro-vibration). */
    private static final float THRESHOLD = 0.01f;

    /** Accumulator used for the sinusoidal wave direction. */
    private static float time = 0f;

    // â”€â”€â”€ Public API â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Trigger a shake. Safe to call from the server-side packet handler if
     * invoked inside ClientPacketListener context, or from client tick listeners.
     *
     * @param amount Shake magnitude. Typical values:
     *               0.5f = light impact, 1.5f = heavy hit, 3.0f = transformation burst
     */
    public static void shake(float amount) {
        intensity = Math.max(intensity, amount); // don't reset if already shaking harder
    }

    // â”€â”€â”€ Rendering Hook â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Adds our screen shake offset to the camera yaw/pitch every render frame,
     * then decays the intensity.
     *
     * ViewportEvent.ComputeCameraAngles fires before the final view matrix is
     * assembled, making it the correct hook for camera-space effects.
     */
    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        // Allow camera shake in all perspectives (F5 included)
        if (intensity < THRESHOLD) {
            intensity = 0f;
            return;
        }

        time += 0.8f;
        float offsetX = (float) Math.sin(time * 3.0) * intensity;
        float offsetY = (float) Math.cos(time * 2.3) * intensity * 0.6f;

        event.setYaw(event.getYaw() + offsetX);
        event.setPitch(event.getPitch() + offsetY);

        // Decay
        intensity *= DECAY;
        if (intensity < THRESHOLD) intensity = 0f;
    }
}

