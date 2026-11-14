package com.neroferno.krm_onore.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.neroferno.krm_onore.client.renderer.SuitVisualRenderer;
import com.neroferno.krm_onore.combat.RiderFistTiming;
import com.neroferno.krm_onore.item.ModItems;
import com.neroferno.krm_onore.item.SuitVisualItem;
import com.neroferno.krm_onore.network.TransformationHelper;
import com.neroferno.krm_onore.attachment.ModAttachments;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.GeckoLibConstants;
import software.bernie.geckolib.animatable.GeoItem;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings({"null", "removal"})
public class SuitRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final Map<AbstractClientPlayer, ItemStack> SUIT_STACKS = new WeakHashMap<>();
    private static final Map<AbstractClientPlayer, Integer> NEXT_GROWING_ATTACK = new WeakHashMap<>();
    /** Hasta que tick de juego el cuerpo del jugador se renderiza mirando hacia donde mira la camara/cabeza. */
    private static final Map<AbstractClientPlayer, Long> FACE_LOOK_UNTIL = new WeakHashMap<>();
    /** Hasta que tick de juego esta corriendo el Especial 3: ningun golpe ni patada puede reemplazar su animacion. */
    private static final Map<AbstractClientPlayer, Long> SPECIAL_UNTIL = new WeakHashMap<>();
    private static final int SPECIAL_LOCK_TICKS = 28;
    /** Hasta que tick de juego se mantiene la pose de bloqueo (block, congelada en 0.25 s). */
    private static final Map<AbstractClientPlayer, Long> BLOCK_UNTIL = new WeakHashMap<>();
    /** Hasta que tick dura el bloqueo, haya pose o no (en primera persona no se anima). Impide iniciar golpes. */
    private static final Map<AbstractClientPlayer, Long> BLOCK_LOCK_UNTIL = new WeakHashMap<>();
    private static final int ATTACK_FACE_TAIL_TICKS = 6;
    private static final int KICK_FACE_TICKS = 14;
    private static final AtomicLong NEXT_ANIMATABLE_ID = new AtomicLong(Long.MAX_VALUE - 1_000_000L);
    private final SuitVisualRenderer renderer;

    public SuitRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> renderLayerParent) {
        super(renderLayerParent);
        this.renderer = new SuitVisualRenderer();
    }

    public static ItemStack getSuitStack(AbstractClientPlayer player) {
        return SUIT_STACKS.computeIfAbsent(player, key -> {
            ItemStack stack = new ItemStack(ModItems.KUUGA_SUIT_VISUAL.get());
            stack.set(GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT.get(), NEXT_ANIMATABLE_ID.getAndIncrement());
            return stack;
        });
    }

    /** Mantiene el cuerpo alineado con la mirada durante {@code ticks} ticks (golpes y patadas). */
    private static void faceLookFor(AbstractClientPlayer player, int ticks) {
        FACE_LOOK_UNTIL.put(player, player.level().getGameTime() + ticks);
    }

    /** true mientras un ataque Growing esta en curso: el cuerpo debe girar hacia donde mira la cabeza. */
    public static boolean isFacingLook(AbstractClientPlayer player) {
        Long until = FACE_LOOK_UNTIL.get(player);
        if (until == null) {
            return false;
        }
        if (player.level().getGameTime() > until) {
            FACE_LOOK_UNTIL.remove(player);
            return false;
        }
        return true;
    }

    private static boolean isSpecialLocked(AbstractClientPlayer player) {
        Long until = SPECIAL_UNTIL.get(player);
        if (until == null) {
            return false;
        }
        if (player.level().getGameTime() >= until) {
            SPECIAL_UNTIL.remove(player);
            return false;
        }
        return true;
    }

    public static void playGrowingPunch(AbstractClientPlayer player, int attackIndex) {
        if (attackIndex < 0 || attackIndex >= 3 || isSpecialLocked(player)) {
            return;
        }

        NEXT_GROWING_ATTACK.put(player, (attackIndex + 1) % 3);

        String animation = switch (attackIndex) {
            case 0 -> "punch1L";
            case 1 -> "punch2L";
            default -> "punch3L";
        };
        faceLookFor(player, RiderFistTiming.animationTicks(false, attackIndex)
                + RiderFistTiming.animationBlendTicks() + ATTACK_FACE_TAIL_TICKS);
        triggerGrowingAttack(player, animation);
    }

    public static void playGrowingHeavyPunch(AbstractClientPlayer player, int attackIndex) {
        if (attackIndex < 0 || attackIndex >= 3 || isSpecialLocked(player)) {
            return;
        }

        NEXT_GROWING_ATTACK.put(player, (attackIndex + 1) % 3);
        String animation = switch (attackIndex) {
            case 0 -> "punch1H";
            case 1 -> "punch2H";
            default -> "punch3H";
        };
        faceLookFor(player, RiderFistTiming.animationTicks(true, attackIndex)
                + RiderFistTiming.animationBlendTicks() + ATTACK_FACE_TAIL_TICKS);
        // En primera persona solo se ve el brazo: el traje usa una variante propia (_fp) del golpe pesado,
        // con menos barrido y mas lenta. El cinturon y la tercera persona siguen con la animacion normal.
        String suitAnimation = isLocalFirstPerson(player) ? animation + "_fp" : animation;
        triggerGrowingAttack(player, suitAnimation, animation);
    }

    /**
     * Bloqueo rapido al retroceder: pose de block mantenida en su punto maximo (0.25 s) {@code holdTicks} ticks.
     * Si ya esta bloqueando solo se renueva el tiempo (sin reiniciar la animacion). Las piernas no tienen keys en
     * esta animacion, asi que siguen moviendose con la caminata normal. En primera persona no se anima para no
     * tapar la vista con los brazos; el dano reducido lo decide el servidor igualmente.
     */
    public static void markBlocking(AbstractClientPlayer player, int holdTicks) {
        BLOCK_LOCK_UNTIL.put(player, player.level().getGameTime() + holdTicks);
    }

    public static void clearBlocking(AbstractClientPlayer player) {
        BLOCK_LOCK_UNTIL.remove(player);
    }

    /** Bloquear y pegar no se puede a la vez: mientras esto sea true los clics de golpe se ignoran. */
    public static boolean isBlocking(AbstractClientPlayer player) {
        Long until = BLOCK_LOCK_UNTIL.get(player);
        return until != null && player.level().getGameTime() < until;
    }

    public static void playGrowingBlock(AbstractClientPlayer player, int holdTicks) {
        if (isSpecialLocked(player)
                || !player.hasData(ModAttachments.RIDER_STATE)
                || player.getData(ModAttachments.RIDER_STATE).getForm() != 1
                || isLocalFirstPerson(player)) {
            return;
        }

        long now = player.level().getGameTime();
        Long until = BLOCK_UNTIL.get(player);
        boolean alreadyHolding = until != null && now < until;
        BLOCK_UNTIL.put(player, now + holdTicks);
        if (!alreadyHolding) {
            triggerGrowingAttack(player, "block_hold");
        }
    }

    /** Suelta la pose de bloqueo: vuelve de 0.25 s al reposo con la segunda mitad de la animacion. */
    public static void releaseGrowingBlock(AbstractClientPlayer player) {
        if (BLOCK_UNTIL.remove(player) != null) {
            triggerGrowingAttack(player, "block_release");
        }
    }

    /** Vence la pose de bloqueo cuando se acaba el tiempo (se llama cada tick de cliente). */
    public static void tickGrowingBlock(AbstractClientPlayer player) {
        Long until = BLOCK_UNTIL.get(player);
        if (until != null && player.level().getGameTime() >= until) {
            releaseGrowingBlock(player);
        }
    }

    public static void playGrowingKick(AbstractClientPlayer player) {
        if (isSpecialLocked(player)
                || !player.hasData(ModAttachments.RIDER_STATE)
                || player.getData(ModAttachments.RIDER_STATE).getForm() != 1) {
            return;
        }

        faceLookFor(player, KICK_FACE_TICKS);
        triggerGrowingAttack(player, "kick");
    }

    /** Especial 3: el cuerpo gira hacia donde mira mientras dura la animacion (1.35 s = 27 ticks + cola). */
    public static void playGrowingSpecial(AbstractClientPlayer player, String animation) {
        if (!player.hasData(ModAttachments.RIDER_STATE)
                || player.getData(ModAttachments.RIDER_STATE).getForm() != 1) {
            return;
        }

        if ("especial2_cancel".equals(animation) || "especial1_cancel".equals(animation)) {
            // Especial 2 interrumpido (p. ej. por un proyectil): suelta el bloqueo de animaciones y vuelve a reposo.
            SPECIAL_UNTIL.remove(player);
            FACE_LOOK_UNTIL.remove(player);
            triggerGrowingAttack(player, animation);
            return;
        }

        // Especial 3 dura 1.35 s; las dos fases del Especial 2 usan su propia duracion.
        int lockTicks = switch (animation) {
            case "especial2_1" -> com.neroferno.krm_onore.combat.Especial2Combat.stanceTicks();
            case "especial2_2" -> com.neroferno.krm_onore.combat.Especial2Combat.strikeTicks();
            case "especial1_1", "especial1_2", "especial1_loop", "especial1_end" ->
                    com.neroferno.krm_onore.combat.Especial1Combat.clientLockTicks(animation);
            default -> SPECIAL_LOCK_TICKS;
        };
        SPECIAL_UNTIL.put(player, player.level().getGameTime() + lockTicks);
        BLOCK_UNTIL.remove(player);
        BLOCK_LOCK_UNTIL.remove(player);
        faceLookFor(player, lockTicks);
        triggerGrowingAttack(player, animation);
    }

    /** Hit-stop: congela (o reanuda) las animaciones del traje y del cinturon de este jugador. */
    public static void setAnimationFrozen(AbstractClientPlayer player, boolean frozen) {
        ItemStack stack = getSuitStack(player);
        if (stack.getItem() instanceof SuitVisualItem item) {
            var manager = item.getAnimatableInstanceCache().getManagerForId(GeoItem.getId(stack));
            var controller = manager.getAnimationControllers().get("suit");
            if (controller != null) {
                controller.transitionLength(RiderFistTiming.animationBlendTicks());
                controller.setAnimationSpeed(frozen ? 0.0001D : 1.0D);
            }
        }
        BeltRenderLayer.setAnimationFrozen(player, frozen);
    }

    private static boolean isLocalFirstPerson(AbstractClientPlayer player) {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        return player == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
    }

    private static void triggerGrowingAttack(AbstractClientPlayer player, String animation) {
        triggerGrowingAttack(player, animation, animation);
    }

    private static void triggerGrowingAttack(AbstractClientPlayer player, String suitAnimation, String beltAnimation) {
        // Cualquier otra animacion (golpe, patada, especial) reemplaza la pose de bloqueo: no se suelta despues encima.
        if (!suitAnimation.startsWith("block_")) {
            BLOCK_UNTIL.remove(player);
        }
        setAnimationFrozen(player, false);
        ItemStack stack = getSuitStack(player);
        SuitVisualItem animatable = (SuitVisualItem) stack.getItem();
        var manager = animatable.getAnimatableInstanceCache().getManagerForId(GeoItem.getId(stack));
        var controller = manager.getAnimationControllers().get("suit");
        if (controller != null) {
            controller.transitionLength(RiderFistTiming.animationBlendTicks());
        }
        animatable.triggerArmorAnim(player, GeoItem.getId(stack), "suit", suitAnimation);
        BeltRenderLayer.playGrowingPunch(player, beltAnimation);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!TransformationHelper.isTransformed(player)) {
            return;
        }

        ItemStack suitStack = getSuitStack(player);

        // Prepare the GeckoLib renderer to sync rotations with the vanilla player model skeleton
        PlayerModel<AbstractClientPlayer> parentModel = this.getParentModel();
        parentModel.setAllVisible(true);
        this.renderer.prepForRender(player, suitStack, EquipmentSlot.CHEST, parentModel);

        int form = player.hasData(ModAttachments.RIDER_STATE) ? player.getData(ModAttachments.RIDER_STATE).getForm() : 1;
        if (SuitVisualRenderer.usesRevealAnimation(form)) {
            float elapsedTicks = SuitVisualRenderer.getGrowingRevealElapsedTicks(player, partialTick);
            if (elapsedTicks < SuitVisualRenderer.GROWING_REVEAL_TOTAL_TICKS) {
                this.renderer.setGrowingRevealProgress(elapsedTicks);
                parentModel.setAllVisible(true);
            } else {
                this.renderer.clearGrowingRevealProgress();
                parentModel.setAllVisible(false);
            }
        } else {
            this.renderer.clearGrowingRevealProgress();
            parentModel.setAllVisible(false);
        }

        ResourceLocation textureRes = form == 1
            ? ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/armor/growing_suit.png")
            : ResourceLocation.fromNamespaceAndPath("krm_revo", "textures/armor/kuuga_suit.png");

        RenderType renderType = this.renderer.getRenderType((SuitVisualItem) suitStack.getItem(),
                                                            textureRes,
                                                            bufferSource, partialTick);

        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

        // Render the model immediately
        this.renderer.renderToBuffer(poseStack, vertexConsumer, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }
}
