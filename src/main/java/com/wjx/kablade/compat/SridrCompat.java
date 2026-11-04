package com.wjx.kablade.compat;

import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import java.util.UUID;

/** Optional sridr integration for damage that does not already use ATTACK_DAMAGE. */
public final class SridrCompat {
    // Read Minecraft's actual protected UUID instead of assuming a fixed value.
    private static final UUID WEAPON_DAMAGE_UUID = WeaponAttributeAccess.damageUuid();

    private SridrCompat() {
    }

    public static UUID weaponDamageUuid() {
        return WEAPON_DAMAGE_UUID;
    }

    /** Capture once when casting; pass the scaled damage to delayed hits/summons. */
    public static float damageMultiplier(LivingEntity owner) {
        return damageMultiplier(owner.getMainHandItem());
    }

    public static float damageMultiplier(ItemStack blade) {
        if (!ModList.get().isLoaded("sridr") || !(blade.getItem() instanceof ItemSlashBlade)) {
            return 1.0F;
        }
        return blade.getCapability(ItemSlashBlade.BLADESTATE).map(state -> {
            float base = state.getBaseAttackModifier();
            if (state.isBroken() || !Float.isFinite(base) || base <= 0.0F) {
                return 1.0F;
            }
            // Query the actual item modifiers so sridr's configuration and
            // UpdateAttackEvent listeners are respected without linking its classes.
            for (AttributeModifier modifier : blade.getAttributeModifiers(EquipmentSlot.MAINHAND)
                    .get(Attributes.ATTACK_DAMAGE)) {
                if (WEAPON_DAMAGE_UUID.equals(modifier.getId())
                        && modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                    float ratio = (float) (modifier.getAmount() / base);
                    return Float.isFinite(ratio) && ratio >= 0.0F ? ratio : 1.0F;
                }
            }
            return 1.0F;
        }).orElse(1.0F);
    }

    /** Apply after the entire SA formula, including its fixed damage and log bonus. */
    public static float scaleDamage(LivingEntity owner, float damage) {
        return damage * damageMultiplier(owner);
    }

    public static double scaleDamage(LivingEntity owner, double damage) {
        return damage * damageMultiplier(owner);
    }

    /** Access the inherited vanilla constant without reflection or a mixin. */
    private static final class WeaponAttributeAccess extends Item {
        private WeaponAttributeAccess() {
            super(new Properties());
        }

        private static UUID damageUuid() {
            return BASE_ATTACK_DAMAGE_UUID;
        }
    }
}
