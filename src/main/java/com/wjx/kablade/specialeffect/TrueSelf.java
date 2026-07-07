package com.wjx.kablade.specialeffect;

import com.wjx.kablade.Main;
import com.wjx.kablade.init.KabladeCapabilities;
import com.wjx.kablade.init.ModSpecialEffects;
import com.wjx.kablade.util.SaTargeting;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.specialeffects.SpecialEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 真我 —— 澄凝之钥专属特殊效果。
 * <p>
 * 从 1.12.2 {@code SETrueSelf} 移植而来：
 * 命中敌人时 30% 概率叠加「预知」层（上限 3）；
 * 每层使造成的伤害 +10%；
 * 每 100 tick（5 秒）自动消退一层。
 * <p>
 * 状态通过 {@link com.wjx.kablade.data.IPlayerPropertyData} capability 存储，
 * 键名为 {@value #PROP_KEY}。
 */
@Mod.EventBusSubscriber(modid = Main.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TrueSelf extends SpecialEffect {

    /** capability key，对应 HUD 属性注册。 */
    public static final String PROP_KEY = "foresight";

    private static final int MAX_STACKS = 3;
    private static final float STACK_CHANCE = 0.30F;
    private static final int DECAY_INTERVAL = 100; // ticks（5 秒）

    public TrueSelf() {
        super(-1, true, true);
    }

    // ── 命中叠加 ───────────────────────────────────────────────────

    @SubscribeEvent
    public static void onHit(mods.flammpfeil.slashblade.event.SlashBladeEvent.HitEvent event) {
        LivingEntity user = event.getUser();
        if (!(user instanceof Player player)) return;
        if (user.level().isClientSide()) return;

        ItemStack blade = event.getBlade();
        if (!hasTrueSelf(blade)) return;
        if (!SaTargeting.canDamage(player, event.getTarget())) return;
        if (player.getRandom().nextFloat() >= STACK_CHANCE) return;

        player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                .ifPresent(cap -> {
                    int cur = cap.get(PROP_KEY);
                    if (cur < MAX_STACKS) {
                        cap.set(PROP_KEY, cur + 1);
                    }
                });
    }

    // ── 100 tick 自动消退 ──────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.side.isClient()) return;
        Player player = event.player;

        if (player.level().getGameTime() % DECAY_INTERVAL != 0) return;

        player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                .ifPresent(cap -> {
                    int cur = cap.get(PROP_KEY);
                    if (cur > 0) {
                        cap.set(PROP_KEY, cur - 1);
                    }
                });
    }

    // ── 伤害加成 ──────────────────────────────────────────────────

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        if (!hasTrueSelfOnHand(player)) return;
        if (!SaTargeting.canDamage(player, event.getEntity())) return;

        player.getCapability(KabladeCapabilities.PLAYER_PROPERTY_DATA)
                .ifPresent(cap -> {
                    int stacks = cap.get(PROP_KEY);
                    if (stacks > 0) {
                        event.setAmount(event.getAmount() * (1.0F + stacks * 0.1F));
                    }
                });
    }

    // ── 工具方法 ───────────────────────────────────────────────────

    private static boolean hasTrueSelf(ItemStack stack) {
        return stack.getCapability(ItemSlashBlade.BLADESTATE)
                .map(state -> state.hasSpecialEffect(ModSpecialEffects.TRUE_SELF.getId()))
                .orElse(false);
    }

    private static boolean hasTrueSelfOnHand(Player player) {
        return hasTrueSelf(player.getMainHandItem());
    }
}
