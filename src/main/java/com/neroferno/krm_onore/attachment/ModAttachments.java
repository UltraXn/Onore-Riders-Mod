package com.neroferno.krm_onore.attachment;

import com.neroferno.krm_onore.KRMRevoMod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

@SuppressWarnings("null")
public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, KRMRevoMod.MODID);

    // 1-slot inventory just for the Driver Belt
    public static final Supplier<AttachmentType<ItemStackHandler>> RIDER_INVENTORY = ATTACHMENT_TYPES.register("rider_inventory",
            () -> AttachmentType.serializable(() -> new ItemStackHandler(1)).copyOnDeath().build());
}
