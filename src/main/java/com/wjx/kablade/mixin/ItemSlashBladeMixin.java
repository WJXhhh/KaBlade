package com.wjx.kablade.mixin;

import com.mojang.logging.LogUtils;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Example mixin into SlashBlade Resharped's blade item.
 *
 * It hooks {@link ItemSlashBlade#hurtEnemy(ItemStack, LivingEntity, LivingEntity)} —
 * called whenever a SlashBlade hits a living entity — and logs the event. Replace the
 * body with your addon's behaviour (extra effects, damage scaling, capability reads, etc.).
 *
 * To target a different SlashBlade class, add a new mixin class to this package and list
 * it under "mixins" (common), "client", or "server" in kablade.mixins.json.
 */
@Mixin(ItemSlashBlade.class)
public class ItemSlashBladeMixin {

    @Unique
    private static final Logger KABLADE_LOGGER = LogUtils.getLogger();

    @Inject(
            method = "hurtEnemy",
            at = @At("HEAD")
            // remap defaults to true: hurtEnemy is a vanilla method (Item.hurtEnemy -> m_7579_),
            // so the refmap must map it for the reobfuscated production jar to work.
            // Use remap = false ONLY when targeting a method SlashBlade itself adds (no SRG name).
    )
    private void kablade$onHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker,
                                     CallbackInfoReturnable<Boolean> cir) {
        KABLADE_LOGGER.debug("[Kablade] SlashBlade {} hit {}", attacker.getName().getString(),
                target.getName().getString());
    }
}
