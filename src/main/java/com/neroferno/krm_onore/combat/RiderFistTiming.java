package com.neroferno.krm_onore.combat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.neroferno.krm_onore.KRMRevoMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Datos configurables para los ataques Growing. Los tiempos de cada ataque son offsets en ticks; el tiempo
 * global de mezcla se suma a impactos, avances y transiciones visuales.
 */
public final class RiderFistTiming {
    public static final int DEFAULT_ANIMATION_BLEND_TICKS = 2;
    private static final String[] ATTACK_NAMES = {
            "punch1_light", "punch2_light", "punch3_light",
            "punch1_heavy", "punch2_heavy", "punch3_heavy"
    };

    private static volatile AttackDefinition[] attacks = defaultAttacks();
    private static volatile int comboResetTicks = 20;
    private static volatile int animationBlendTicks = DEFAULT_ANIMATION_BLEND_TICKS;

    private RiderFistTiming() {}

    public static void registerReloadListener(net.neoforged.neoforge.event.AddReloadListenerEvent event) {
        event.addListener(new SimpleJsonResourceReloadListener(KRMRevoMod.GSON, "rider_attacks") {
            @Override
            protected void apply(Map<ResourceLocation, JsonElement> resources,
                                 ResourceManager resourceManager,
                                 ProfilerFiller profiler) {
                loadDatapack(resources);
            }
        });
    }

    public static AttackDefinition attack(boolean heavy, int attackIndex) {
        if (attackIndex < 0 || attackIndex >= 3) {
            throw new IllegalArgumentException("Attack index must be between 0 and 2: " + attackIndex);
        }
        return attacks[attackIndex + (heavy ? 3 : 0)];
    }

    public static int animationTicks(boolean heavy, int attackIndex) {
        return attack(heavy, attackIndex).animationTicks();
    }

    public static int maxAnimationTicks(boolean heavy) {
        int maxTicks = 0;
        for (int attackIndex = 0; attackIndex < 3; attackIndex++) {
            maxTicks = Math.max(maxTicks, animationTicks(heavy, attackIndex));
        }
        return maxTicks;
    }

    public static int hitStopTicks(boolean heavy, int attackIndex) {
        return attack(heavy, attackIndex).hitStopTicks();
    }

    public static float shakeAmount(boolean heavy, int attackIndex) {
        return attack(heavy, attackIndex).shakeAmount();
    }

    public static int comboResetTicks() {
        return comboResetTicks;
    }

    public static int animationBlendTicks() {
        return animationBlendTicks;
    }

    public static String serializeForSync() {
        JsonObject root = new JsonObject();
        JsonObject settings = new JsonObject();
        settings.addProperty("combo_reset_ticks", comboResetTicks);
        settings.addProperty("animation_blend_ticks", animationBlendTicks);
        root.add("settings", settings);

        JsonObject attackData = new JsonObject();
        for (int i = 0; i < ATTACK_NAMES.length; i++) {
            attackData.add(ATTACK_NAMES[i], toJson(attacks[i]));
        }
        root.add("attacks", attackData);
        return KRMRevoMod.GSON.toJson(root);
    }

    public static void applySyncedData(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            applyDefinitions(root);
        } catch (IllegalArgumentException | IllegalStateException | JsonParseException exception) {
            KRMRevoMod.LOGGER.error("Received invalid Growing attack data from the server.", exception);
        }
    }

    private static void loadDatapack(Map<ResourceLocation, JsonElement> resources) {
        JsonObject root = new JsonObject();
        JsonElement settings = null;
        JsonObject attackData = new JsonObject();
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            ResourceLocation id = entry.getKey();
            JsonElement element = entry.getValue();
            String path = id.getPath();
            if ("settings".equals(path)) {
                if (element.isJsonObject()) {
                    settings = element;
                } else {
                    KRMRevoMod.LOGGER.error("Ignoring invalid Growing attack settings at {}: expected an object.", id);
                }
            } else if (isAttackName(path)) {
                attackData.add(path, element);
            }
        }
        root.add("settings", settings == null ? new JsonObject() : settings);
        root.add("attacks", attackData);
        applyDefinitions(root);
    }

    private static void applyDefinitions(JsonObject root) {
        AttackDefinition[] updatedAttacks = defaultAttacks();
        int updatedComboResetTicks = 20;
        int updatedAnimationBlendTicks = DEFAULT_ANIMATION_BLEND_TICKS;

        if (root.has("settings") && root.get("settings").isJsonObject()) {
            JsonObject settings = root.getAsJsonObject("settings");
            if (settings.has("combo_reset_ticks")) {
                try {
                    updatedComboResetTicks = nonNegativeInt(settings, "combo_reset_ticks");
                } catch (IllegalArgumentException exception) {
                    KRMRevoMod.LOGGER.error("Invalid Growing combo reset time; using its default: {}",
                            exception.getMessage());
                }
            }
            if (settings.has("animation_blend_ticks")) {
                try {
                    updatedAnimationBlendTicks = nonNegativeInt(settings, "animation_blend_ticks");
                } catch (IllegalArgumentException exception) {
                    KRMRevoMod.LOGGER.error("Invalid Growing animation blend; using its default: {}",
                            exception.getMessage());
                }
            }
        }

        JsonObject attackData = root.has("attacks") && root.get("attacks").isJsonObject()
                ? root.getAsJsonObject("attacks")
                : new JsonObject();
        for (int i = 0; i < ATTACK_NAMES.length; i++) {
            String name = ATTACK_NAMES[i];
            if (!attackData.has(name)) {
                continue;
            }
            try {
                if (!attackData.get(name).isJsonObject()) {
                    throw new IllegalArgumentException("Expected an object.");
                }
                updatedAttacks[i] = parseAttack(attackData.getAsJsonObject(name));
            } catch (IllegalArgumentException | IllegalStateException | JsonParseException exception) {
                KRMRevoMod.LOGGER.error("Invalid Growing attack data '{}'; keeping its defaults: {}",
                        name, exception.getMessage());
            }
        }

        attacks = updatedAttacks;
        comboResetTicks = updatedComboResetTicks;
        animationBlendTicks = updatedAnimationBlendTicks;
    }

    private static AttackDefinition parseAttack(JsonObject json) {
        int animationTicks = positiveInt(json, "animation_ticks");
        JsonArray phaseJson = required(json, "hitbox_phases").getAsJsonArray();
        if (phaseJson.isEmpty()) {
            throw new IllegalArgumentException("'hitbox_phases' must contain at least one phase.");
        }
        List<HitboxPhase> phases = new ArrayList<>(phaseJson.size());
        int previousEndTick = -1;
        for (JsonElement element : phaseJson) {
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("Every hitbox phase must be an object.");
            }
            JsonObject phase = element.getAsJsonObject();
            int startTick = nonNegativeInt(phase, "start_tick");
            int endTick = nonNegativeInt(phase, "end_tick");
            double width = positiveBoundedDouble(phase, "width", 8.0D);
            double height = positiveBoundedDouble(phase, "height", 8.0D);
            double depth = positiveBoundedDouble(phase, "depth", 8.0D);
            double startOffset = nonNegativeBoundedDouble(phase, "start_offset", 12.0D);
            double endOffset = nonNegativeBoundedDouble(phase, "end_offset", 12.0D);
            if (endTick < startTick || startTick < previousEndTick) {
                throw new IllegalArgumentException("Hitbox phases must be sorted and have a valid tick range.");
            }
            if (endTick > animationTicks) {
                throw new IllegalArgumentException("Hitbox phases cannot extend past 'animation_ticks'.");
            }
            phases.add(new HitboxPhase(startTick, endTick, width, height, depth, startOffset, endOffset));
            previousEndTick = endTick;
        }

        double damage = nonNegativeFloatValue(json, "damage");
        double lungeStartTick = nonNegativeDouble(json, "lunge_start_tick");
        double lungeEndTick = nonNegativeDouble(json, "lunge_end_tick");
        double lungeStrength = nonNegativeDouble(json, "lunge_strength");
        float extraKnockback = nonNegativeFloat(json, "extra_knockback");
        int hitStopTicks = nonNegativeInt(json, "hit_stop_ticks");
        float shakeAmount = nonNegativeFloat(json, "shake");
        Reaction reaction = Reaction.valueOf(required(json, "reaction").getAsString().toUpperCase(java.util.Locale.ROOT));
        int reactionTicks = nonNegativeInt(json, "reaction_ticks");
        double launchVelocity = nonNegativeDouble(json, "launch_velocity");
        if (lungeEndTick <= lungeStartTick) {
            throw new IllegalArgumentException("'lunge_end_tick' must be greater than 'lunge_start_tick'.");
        }
        if (reaction != Reaction.NONE && reactionTicks == 0) {
            throw new IllegalArgumentException("'reaction_ticks' must be greater than zero for an active reaction.");
        }
        if (reaction != Reaction.LAUNCHER && launchVelocity != 0.0D) {
            throw new IllegalArgumentException("'launch_velocity' is only valid for a launcher reaction.");
        }

        return new AttackDefinition(animationTicks, phases, damage, lungeStartTick, lungeEndTick,
                lungeStrength, extraKnockback, hitStopTicks, shakeAmount, reaction, reactionTicks, launchVelocity);
    }

    private static JsonElement required(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalArgumentException("Missing required field '" + key + "'.");
        }
        return json.get(key);
    }

    private static int positiveInt(JsonObject json, String key) {
        int value = exactInt(required(json, key), key);
        if (value <= 0) {
            throw new IllegalArgumentException("'" + key + "' must be greater than zero.");
        }
        return value;
    }

    private static int nonNegativeInt(JsonObject json, String key) {
        int value = exactInt(required(json, key), key);
        if (value < 0) {
            throw new IllegalArgumentException("'" + key + "' must not be negative.");
        }
        return value;
    }

    private static int exactInt(JsonElement element, String key) {
        double value = number(element, key);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE || value != Math.rint(value)) {
            throw new IllegalArgumentException("'" + key + "' must be an integer.");
        }
        return (int) value;
    }

    private static double nonNegativeDouble(JsonObject json, String key) {
        double value = number(required(json, key), key);
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalArgumentException("'" + key + "' must be a finite non-negative number.");
        }
        return value;
    }

    private static double positiveBoundedDouble(JsonObject json, String key, double maxValue) {
        double value = nonNegativeDouble(json, key);
        if (value <= 0.0D || value > maxValue) {
            throw new IllegalArgumentException("'" + key + "' must be greater than zero and at most " + maxValue + ".");
        }
        return value;
    }

    private static double nonNegativeBoundedDouble(JsonObject json, String key, double maxValue) {
        double value = nonNegativeDouble(json, key);
        if (value > maxValue) {
            throw new IllegalArgumentException("'" + key + "' must be at most " + maxValue + ".");
        }
        return value;
    }

    private static double number(JsonElement element, String key) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("'" + key + "' must be a JSON number.");
        }
        return element.getAsDouble();
    }

    private static float nonNegativeFloat(JsonObject json, String key) {
        return (float) nonNegativeFloatValue(json, key);
    }

    private static double nonNegativeFloatValue(JsonObject json, String key) {
        double value = nonNegativeDouble(json, key);
        if (value > Float.MAX_VALUE) {
            throw new IllegalArgumentException("'" + key + "' is too large.");
        }
        return value;
    }

    private static boolean isAttackName(String name) {
        for (String attackName : ATTACK_NAMES) {
            if (attackName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static JsonObject toJson(AttackDefinition attack) {
        JsonObject json = new JsonObject();
        json.addProperty("animation_ticks", attack.animationTicks());
        JsonArray phases = new JsonArray();
        attack.phases().forEach(phase -> {
            JsonObject phaseJson = new JsonObject();
            phaseJson.addProperty("start_tick", phase.startTick());
            phaseJson.addProperty("end_tick", phase.endTick());
            phaseJson.addProperty("width", phase.width());
            phaseJson.addProperty("height", phase.height());
            phaseJson.addProperty("depth", phase.depth());
            phaseJson.addProperty("start_offset", phase.startOffset());
            phaseJson.addProperty("end_offset", phase.endOffset());
            phases.add(phaseJson);
        });
        json.add("hitbox_phases", phases);
        json.addProperty("damage", attack.damage());
        json.addProperty("lunge_start_tick", attack.lungeStartTick());
        json.addProperty("lunge_end_tick", attack.lungeEndTick());
        json.addProperty("lunge_strength", attack.lungeStrength());
        json.addProperty("extra_knockback", attack.extraKnockback());
        json.addProperty("hit_stop_ticks", attack.hitStopTicks());
        json.addProperty("shake", attack.shakeAmount());
        json.addProperty("reaction", attack.reaction().name().toLowerCase(java.util.Locale.ROOT));
        json.addProperty("reaction_ticks", attack.reactionTicks());
        json.addProperty("launch_velocity", attack.launchVelocity());
        return json;
    }

    private static AttackDefinition[] defaultAttacks() {
        return new AttackDefinition[] {
                new AttackDefinition(12, List.of(new HitboxPhase(6, 9, 0.9D, 1.2D, 1.0D, 0.35D, 2.2D)), 5.0D, 1.28D, 9.28D, 0.21D, 0.1F, 2, 0.35F, Reaction.NONE, 0, 0.0D),
                new AttackDefinition(12, List.of(new HitboxPhase(6, 9, 0.9D, 1.2D, 1.0D, 0.35D, 2.2D)), 5.0D, 3.36D, 10.08D, 0.21D, 0.1F, 2, 0.35F, Reaction.NONE, 0, 0.0D),
                new AttackDefinition(12, List.of(new HitboxPhase(6, 9, 1.1D, 1.3D, 1.2D, 0.35D, 2.4D)), 5.0D, 2.08D, 8.64D, 0.42D, 0.1F, 3, 0.9F, Reaction.KNOCKDOWN, 18, 0.0D),
                new AttackDefinition(15, List.of(new HitboxPhase(7, 10, 1.0D, 1.3D, 1.1D, 0.35D, 2.3D)), 7.0D, 5.0D, 13.334D, 0.21D, 0.4F, 3, 0.7F, Reaction.NONE, 0, 0.0D),
                new AttackDefinition(15, List.of(new HitboxPhase(7, 10, 1.0D, 1.3D, 1.1D, 0.35D, 2.3D)), 7.0D, 4.2D, 13.4D, 0.21D, 0.4F, 3, 0.7F, Reaction.NONE, 0, 0.0D),
                new AttackDefinition(20, List.of(new HitboxPhase(9, 14, 1.3D, 1.5D, 1.4D, 0.4D, 2.8D)), 7.0D, 4.445D, 17.779D, 0.88D, 0.4F, 4, 1.5F, Reaction.LAUNCHER, 12, 0.8D)
        };
    }

    public record AttackDefinition(int animationTicks, List<HitboxPhase> phases, double damage,
                                   double lungeStartTick, double lungeEndTick, double lungeStrength,
                                   float extraKnockback, int hitStopTicks, float shakeAmount,
                                   Reaction reaction, int reactionTicks, double launchVelocity) {
        public AttackDefinition {
            phases = List.copyOf(phases);
        }
    }

    public record HitboxPhase(int startTick, int endTick, double width, double height, double depth,
                              double startOffset, double endOffset) {}

    public enum Reaction {
        NONE,
        STUN,
        KNOCKDOWN,
        LAUNCHER
    }
}
