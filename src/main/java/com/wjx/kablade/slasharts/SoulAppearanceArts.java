package com.wjx.kablade.slasharts;

import com.wjx.kablade.entity.JizoMitamaSoulEntity;
import com.wjx.kablade.util.MathFunc;
import mods.flammpfeil.slashblade.capability.slashblade.ISlashBladeState;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.slasharts.SlashArts;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Function;

/** 御魂示现：召唤地藏御魂，并在显形蓄势后完成一次正前方下劈。 */
public final class SoulAppearanceArts extends SlashArts {

    private static final float BASE_DAMAGE = 30.0F;
    private static final float EXTRA_DAMAGE_FACTOR = 18.0F;
    private static final float DAMAGE_MULTIPLIER = 2.0F;

    public SoulAppearanceArts(Function<LivingEntity, ResourceLocation> state) {
        super(state);
    }

    @Override
    public ResourceLocation doArts(ArtsType type, LivingEntity user) {
        if (user.level().isClientSide() || type == ArtsType.Fail) {
            return super.doArts(type, user);
        }

        float bladeAttack = user.getMainHandItem().getCapability(ItemSlashBlade.BLADESTATE)
                .map(ISlashBladeState::getBaseAttackModifier)
                .orElse(4.0F);
        float damage = (BASE_DAMAGE
                + (float) MathFunc.amplifierCalc(bladeAttack, EXTRA_DAMAGE_FACTOR))
                * DAMAGE_MULTIPLIER;
        JizoMitamaSoulEntity.spawn((ServerLevel) user.level(), user, damage);
        return super.doArts(type, user);
    }
}
