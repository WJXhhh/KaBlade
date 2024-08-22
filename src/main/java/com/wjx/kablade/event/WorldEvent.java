package com.wjx.kablade.event;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.wjx.kablade.Entity.AbsEntityShield;
import com.wjx.kablade.Entity.EntityRaikiriBlade;
import com.wjx.kablade.Entity.EntitySummonedSwordBasePlus;
import com.wjx.kablade.Entity.EntityWine;
import com.wjx.kablade.Entity.Render.RenderRaikiriBlade;
import com.wjx.kablade.Lib;
import com.wjx.kablade.Main;
import com.wjx.kablade.SlashBlade.blades.bladeitem.MagicBlade;
import com.wjx.kablade.capability.CapabilityLoader;
import com.wjx.kablade.capability.CapabilitySlashPotion;
import com.wjx.kablade.capability.inters.IPotionInSlash;
import com.wjx.kablade.init.EnchantmentInit;
import com.wjx.kablade.init.ItemInit;
import com.wjx.kablade.init.PotionInit;
import com.wjx.kablade.network.*;
import com.wjx.kablade.util.*;
import com.wjx.kablade.util.handlers.PlayerThrowableHandler;
import com.wjx.kablade.util.interfaces.IKabladeOre;
import com.wjx.kablade.util.special_render.MagChaosBladeEffectRenderer;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.ResourceLocationRaw;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.Timer;
import java.util.*;

import static com.wjx.kablade.Lib.*;
import static com.wjx.kablade.Main.*;
import static com.wjx.kablade.SlashBlade.BladeLoader.ITEM_MAGIC;

//@Mod.EventBusSubscriber
public class WorldEvent {

    public WorldEvent() {
        MinecraftForge.EVENT_BUS.register(this);

        timer.schedule(task, 0, 5);
    }

    public static Set<Vector2O<Runnable, Integer>> tickSchedule = Sets.newHashSet();

    public static void addTickDelayTask(int tick, Runnable runnable) {
        tickSchedule.add(new Vector2O<>(runnable, tick));
    }


    public static ArrayList<Integer> auroraBladeColor = Lists.newArrayList();
    public static int auroraBladeColorIndex = 0;

    public static RotateAngleManager angleManager = new RotateAngleManager();


    public static Timer timer = new Timer();

    public static TimerTask task = new TimerTask() {
        @Override
        public void run() {
            angleManager.rotate(1f);
        }
    };

    int flagikow = 1;
    int flagweb = 1;


    ResourceLocation HuntingLockerIcon = new ResourceLocation(Main.MODID + ":textures/icon/hunting_locker.png");
    ResourceLocation SakuraBrand = new ResourceLocation(Main.MODID + ":textures/icon/sakura_brand.png");
    ResourceLocation MagChaosBladeEffectIcon = new ResourceLocation(MODID + ":textures/effect/tex_mag_chaos_blade_effect.png");

    @SuppressWarnings("unchecked")
    public static void loadEvent() {
        //auroraColor
        auroraBladeColor.clear();
        auroraBladeColorIndex = 0;
        int g = 196;
        int b = 255;
        while (g < 255) {
            int c = Lib.rgbToMetrication(0, g, b);
            auroraBladeColor.add(c);
            g++;
            b--;
        }
        ArrayList<Integer> array1 = (ArrayList<Integer>) auroraBladeColor.clone();
        Collections.reverse(array1);
        auroraBladeColor.addAll(array1);
    }

    public static Set<Class<? extends Entity>> antiEntity = Sets.newHashSet();

    @SubscribeEvent
    public void onWorldUpdate(TickEvent.WorldTickEvent event) {
        if (!auroraBladeColor.isEmpty()) {

            if (auroraBladeColorIndex >= (auroraBladeColor.size() - 2)) {
                auroraBladeColorIndex = 0;
            } else auroraBladeColorIndex++;
        }
        //MagChaosBladeEffectsSub
        if (event.phase == TickEvent.Phase.START) {
            if (!event.world.isRemote) {
                if (!MagChaosBladeEffectRenderer.magChaosBladeEffectRenderers.isEmpty()) {
                    for (Iterator<MagChaosBladeEffectRenderer> it = MagChaosBladeEffectRenderer.magChaosBladeEffectRenderers.iterator(); it.hasNext(); ) {
                        MagChaosBladeEffectRenderer i = it.next();
                        if (i.exitTick > 0) {
                            i.exitTick -= 1;
                            PACKET_HANDLER.sendToAll(new MessageMagChaosBladeEffectUpdate());
                        } else {
                            it.remove();
                            PACKET_HANDLER.sendToAll(new MessageMagChaosBladeEffectUpdate());
                        }
                    }
                }
                if (!tickSchedule.isEmpty()) {
                    for (Iterator<Vector2O<Runnable, Integer>> iterator = tickSchedule.iterator(); iterator.hasNext(); ) {
                        Vector2O<Runnable, Integer> v = iterator.next();
                        if (v.value <= 0) {
                            v.key.run();
                            iterator.remove();
                        } else {
                            v.value--;
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityItemJoinWorld(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        for (Class<? extends Entity> clazz : antiEntity) {
            if (clazz.isInstance(entity)) {
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public void onPlayerJoinWorld(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        if (!player.world.isRemote) {
            if (YesUpdate) {
                PACKET_HANDLER.sendTo(new MessageResetSend(), (EntityPlayerMP) player);
            }
        }
        if (!Main.isBladePostLoad) {
            Main.isBladePostLoad = true;
            ItemStack stack = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.plasma_kagehide", 1);
            stack.addEnchantment(EnchantmentInit.ENCHANTMENT_SLOW, 1);
            SlashBlade.BladeRegistry.put(new ResourceLocationRaw(bladestr, "wjx.blade.honkai.plasma_kagehide"), stack);
            addEnchantmentForBlade(EnchantmentInit.ENCHANTMENT_SLOW, 1, "wjx.blade.honkai.xuanyuan_katana");
        }
    }

    void addEnchantmentForBlade(Enchantment e, int level, String name) {
        ItemStack stack = SlashBlade.findItemStack(bladestr, name, 1);
        stack.addEnchantment(e, level);
        SlashBlade.BladeRegistry.put(new ResourceLocationRaw(bladestr, name), stack);
    }


    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof MagicBlade) {
            for (int x = 0; x < event.getToolTip().size(); ++x) {
                if (event.getToolTip().get(x).contains(I18n.translateToLocal("attribute.name.generic.attackDamage")) || event.getToolTip().get(x).contains(I18n.translateToLocal("Attack Damage"))) {
                    event.getToolTip().set(x, TextFormatting.BLUE + " +" + UpdateColor.makeColourRainbow(I18n.translateToLocal("info.damageguer1111.name")) + " " + TextFormatting.BLUE + I18n.translateToLocal("attribute.name.generic.attackDamage"));
                    return;
                }
            }
        }

    }

    @SubscribeEvent
    public void PlayerDeadProtect(LivingDeathEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            if (player.inventory.hasItemStack(new ItemStack(ITEM_MAGIC)) || player.getHeldItemMainhand().getItem() instanceof MagicBlade) {
                player.setHealth(player.getMaxHealth());
                player.deathTime = 0;
                player.isDead = false;
                player.preparePlayerToSpawn();
                if (event.isCancelable()) {
                    event.setCanceled(true);
                }

            }
        }
    }

    @SubscribeEvent
    public void entityDrop(LivingDropsEvent event){
        EntityLivingBase entityLivingBase = event.getEntityLiving();
        if(entityLivingBase instanceof EntityCreeper){
            if (((EntityCreeper) entityLivingBase).getPowered()){
                entityLivingBase.entityDropItem(new ItemStack(ItemInit.ELECTRO_SIGNET),0f);
            }
        }
    }

    @SubscribeEvent
    public void PlayerHurtProtect(LivingHurtEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            if (player.inventory.hasItemStack(new ItemStack(ITEM_MAGIC)) || player.getHeldItemMainhand().getItem() instanceof MagicBlade) {
                player.setHealth(player.getMaxHealth());
                player.deathTime = 0;
                player.isDead = false;
                if (event.isCancelable()) {
                    event.setCanceled(true);
                }

            }
        }
    }


    @SubscribeEvent
    public void PlayerUpdattingProtect(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntityLiving() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            if (player.inventory.hasItemStack(new ItemStack(ITEM_MAGIC)) || player.getHeldItemMainhand().getItem() instanceof MagicBlade) {
                if (player.isDead || player.getHealth() <= 0) {
                    player.setHealth(player.getMaxHealth());
                    player.deathTime = 0;
                    player.isDead = false;
                    player.preparePlayerToSpawn();
                }

            }
        }

    }


    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();

        World world = entity.world;
        NBTTagCompound KaBladeCompound = KaBladeEntityProperties.getPropCompound(entity);
        if (!entity.world.isRemote) {
            if (entity.getEntityData().getInteger("frost_blade_1") > 0) {
                if (!entity.world.isRemote) {
                    entity.getEntityData().setInteger("frost_blade_1", entity.getEntityData().getInteger("frost_blade_1") - 1);
                    entity.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 60, 2));
                }
            }
            if (entity.hasCapability(CapabilityLoader.SlashPotion, null)) {
                IPotionInSlash slash = entity.getCapability(CapabilityLoader.SlashPotion, null);
                Capability.IStorage<IPotionInSlash> storage = CapabilityLoader.SlashPotion.getStorage();
                NBTTagCompound compound = (NBTTagCompound) storage.writeNBT(CapabilityLoader.SlashPotion, slash, null);
                if (compound != null && compound.getInteger(NBT_SLOW_LEVEL) > 0) {
                    if (compound.getInteger(NBT_SLOW_TIME) > 0) {
                        compound.setInteger(NBT_SLOW_TIME, compound.getInteger(NBT_SLOW_TIME) - 1);
                        int state1;
                        int state2;
                        if (world.rand.nextBoolean()) {
                            state1 = 1;
                        } else state1 = -1;
                        if (world.rand.nextBoolean()) {
                            state2 = 1;
                        } else state2 = -1;
                        PACKET_HANDLER.sendToAll(new MessageSpawnParticle(EnumParticleTypes.SLIME, entity.posX + world.rand.nextDouble() * state1, entity.posY + (entity.height / 2), entity.posZ + world.rand.nextDouble() * state2, 0.0D, 0.0D, 0.0D));
                        if (compound.getInteger(NBT_SLOW_TIME) > 1) {
                            entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(compound.getDouble(NBT_ORIGIN_MOVEMENT) / (1 + compound.getInteger(NBT_SLOW_LEVEL) * 0.2));
                        }
                        if (compound.getInteger(NBT_SLOW_TIME) == 1) {
                            entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(compound.getDouble(NBT_ORIGIN_MOVEMENT));
                        }
                    }
                }
            }
            //WineBind
            {
                if (KaBladeCompound.getInteger(KaBladeEntityProperties.PROP_WINE_BIND) > 0) {
                    KaBladeCompound.setInteger(KaBladeEntityProperties.PROP_WINE_BIND, KaBladeEntityProperties.getPropCompound(entity).getInteger(KaBladeEntityProperties.PROP_WINE_BIND) - 1);
                    KaBladeEntityProperties.updateNBTForClient(entity);
                    Entity attacker = world.getEntityByID(KaBladeCompound.getInteger(KaBladeEntityProperties.PROP_WINE_BIND_ATTACKER));
                    float extraDamage = 0f;
                    if (attacker instanceof EntityLivingBase && ((EntityLivingBase) attacker).getHeldItemMainhand().getItem() instanceof ItemSlashBlade) {
                        extraDamage = (float)MathFunc.amplifierCalc(ItemSlashBlade.BaseAttackModifier.get(((EntityLivingBase) attacker).getHeldItemMainhand().getTagCompound()),4f);
                    }
                    if (world.getTotalWorldTime() % 20 == 0 && attacker != null && !attacker.isDead) {
                        if (attacker instanceof EntityLivingBase) {
                            entity.attackEntityFrom(DamageSource.causeMobDamage((EntityLivingBase) attacker), 3 + extraDamage);
                            EntitySummonedSwordBasePlus sword = new EntitySummonedSwordBasePlus(world, (EntityLivingBase) attacker, 4 + extraDamage, entity.posX + 1, entity.posY + entity.getEyeHeight() + 1, entity.posZ, 0f, 0f);
                            EntitySummonedSwordBasePlus sword2 = new EntitySummonedSwordBasePlus(world, (EntityLivingBase) attacker, 4 + extraDamage, entity.posX - 1, entity.posY + entity.getEyeHeight() + 1, entity.posZ, 0f, 0f);
                            sword.setColor(3388211);
                            sword2.setColor(3388211);
                            world.spawnEntity(sword);
                            world.spawnEntity(sword2);
                        }
                    }
                    boolean hasLocked = false;
                    for (Entity e : world.loadedEntityList) {
                        if (e instanceof EntityWine) {
                            if (e.getDataManager().get(EntityWine.targetID) == entity.getEntityId()) {
                                hasLocked = true;
                            }
                        }
                    }
                    if (hasLocked) {
                        entity.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, -1, 2));
                    } else {
                        int originX = (int) entity.posX;
                        int originY = (int) entity.posY;
                        int originZ = (int) entity.posZ;
                        ArrayList<BlockPos> poses = Lists.newArrayList();
                        ArrayList<BlockPos> correctPoses = Lists.newArrayList();
                        int dx = originX - 1;
                        int dy = originY - 1;
                        int dz = originZ - 1;
                        int mx = originX + 1;
                        int my = originY + 1;
                        int mz = originZ + 1;
                        boolean shouldBreak = false;
                        for (int i = 0; i < 6; i++) {
                            if (shouldBreak) {
                                break;
                            }
                            dx -= 1;
                            dy -= 1;
                            dz -= 1;
                            mx += 1;
                            my += 1;
                            mz += 1;
                            for (int j = dx; j < mx; j++) {
                                for (int k = dy; k < my; k++) {
                                    if (k <= 0) {
                                        continue;
                                    }
                                    for (int l = dz; l < mz; l++) {
                                        BlockPos pos1 = new BlockPos(j, k, l);
                                        if (poses.contains(pos1)) {
                                            continue;
                                        } else {
                                            if (world.getBlockState(pos1).getBlock() != Blocks.AIR) {
                                                correctPoses.add(pos1);
                                                shouldBreak = true;
                                                poses.add(pos1);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (!correctPoses.isEmpty()) {
                            for (int i = 0; i < 3; i++) {
                                BlockPos pos = correctPoses.get(world.rand.nextInt(correctPoses.size()));
                                EntityWine wine = new EntityWine(world, pos.getX(), pos.getY(), pos.getZ());
                                wine.getDataManager().set(EntityWine.targetID, entity.getEntityId());
                                world.spawnEntity(wine);
                            }
                        }
                    }
                }
            }
            //Thunder Crystal Attack
            {
                if (KaBladeCompound.hasKey(KaBladeEntityProperties.THUNDER_CRYSTAL_ATTACK)) {
                    if (KaBladeCompound.getInteger(KaBladeEntityProperties.THUNDER_CRYSTAL_ATTACK) > 0) {
                        KaBladeEntityProperties.doIntegerLower(KaBladeCompound, KaBladeEntityProperties.THUNDER_CRYSTAL_ATTACK);
                    }
                    if (KaBladeCompound.getInteger(KaBladeEntityProperties.THUNDER_CRYSTAL_ATTACK) <= 0) {
                        entity.world.addWeatherEffect(new EntityLightningBolt(entity.world, entity.posX, entity.posY, entity.posZ, false));
                        entity.world.createExplosion(null, entity.posX, entity.posY, entity.posZ, 2f, true);
                        KaBladeCompound.removeTag(KaBladeEntityProperties.THUNDER_CRYSTAL_ATTACK);
                    }
                }
            }
            //FreezeDomainBooster
            {
                if (KaBladeCompound.hasKey(KaBladeEntityProperties.FREEZE_DOMAIN_DAMAGE_BOOSTER)) {
                    if (KaBladeCompound.getInteger(KaBladeEntityProperties.FREEZE_DOMAIN_DAMAGE_BOOSTER) > 0) {
                        KaBladeEntityProperties.doIntegerLower(KaBladeCompound, KaBladeEntityProperties.FREEZE_DOMAIN_DAMAGE_BOOSTER);
                    } else {
                        KaBladeCompound.removeTag(KaBladeEntityProperties.FREEZE_DOMAIN_DAMAGE_BOOSTER);
                    }
                }
            }
        }


    }

    @SubscribeEvent
    public void PlayerUpdateEvent(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        World world = event.player.world;
        NBTTagCompound playerProperties = KaBladePlayerProp.getPropCompound(player);
        if (player.world.isRemote) {
            if (YesUpdate && !hasSendMessage) {
                hasSendMessage = true;
                player.sendStatusMessage(new TextComponentString("§6§l[斩无不断]§b检测到模组有更新。最新版本为：§6" + Main.GetUrlVersion), false);
                TextComponentString t = new TextComponentString("§9https://www.mcmod.cn/class/8128.html");
                t.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.mcmod.cn/class/8128.html")).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("点击前往链接")));
                player.sendStatusMessage(new TextComponentString("§6§l[斩无不断]§b可从MC百科下载或从其界面寻找蓝奏云下载地址：").appendSibling(t), false);
            }
        }
        //Chop Willow
        if (player.getEntityData().getBoolean("to_chop_willow")) {
            player.getEntityData().setInteger("chop_willow_retry_count", 0);
            player.getEntityData().setBoolean("to_chop_willow", false);
            player.getEntityData().setBoolean("start_chop_willow", true);
            player.getEntityData().setInteger("chop_willow", 10);
        }
        if (player.getEntityData().getBoolean("start_chop_willow")) {

            float extraDamage = (float) MathFunc.amplifierCalc((ItemSlashBlade.BaseAttackModifier.get((player.getHeldItemMainhand().getTagCompound()))),4f);

            if (player.getEntityData().getInteger("chop_willow") > -1) {
                player.getEntityData().setInteger("chop_willow", player.getEntityData().getInteger("chop_willow") - 1);
            } else {
                boolean retry = false;
                int RightEntityCount = 0;
                player.getEntityData().setBoolean("start_chop_willow", false);
                AxisAlignedBB bb = player.getEntityBoundingBox();
                bb = bb.grow(4.0D, 2D, 4.0D);
                bb = bb.offset(player.motionX, player.motionY, player.motionZ);
                List<Entity> list = world.getEntitiesInAABBexcluding(player, bb, input -> input != player && input.isEntityAlive());
                for (Entity entity : list) {
                    if (entity instanceof EntityLivingBase) {
                        entity.attackEntityFrom(DamageSource.causePlayerDamage(player), 4 + extraDamage);
                        RightEntityCount++;
                    }
                }
                if (RightEntityCount == 0) {
                    retry = true;
                }
                if (player.getEntityData().getInteger("chop_willow_retry_count") > 40) {
                    player.getEntityData().setInteger("chop_willow_retry_count", 41);
                    retry = false;
                }
                if (world.isRemote) {
                    double x1 = player.posX;
                    double y1 = player.posY;
                    double z1 = player.posZ;

                    for (int i = 0; i < 10; ++i) {
                        int state1;
                        int state2;
                        if (world.rand.nextBoolean()) {
                            state1 = 1;
                        } else state1 = -1;
                        if (world.rand.nextBoolean()) {
                            state2 = 1;
                        } else state2 = -1;
                        world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, x1 + (world.rand.nextDouble() * state1), y1 + world.rand.nextDouble() * (double) player.height / 2, z1 + (world.rand.nextDouble() * state2), 0.0D, 0.0D, 0.0D);
                    }
                }
                if (!retry) {
                    return;
                }
                if (player.getEntityData().getInteger("chop_willow_retry_count") <= 40) {
                    player.getEntityData().setBoolean("start_chop_willow", true);
                    player.getEntityData().setInteger("chop_willow_retry_count", player.getEntityData().getInteger("chop_willow_retry_count") + 1);
                }
            }
        }
        //SlashBladeColorUpdate
        {
            //Aurora
            if (!player.world.isRemote) {
                ItemStack stack = player.getHeldItemMainhand();
                if (stack.getItem() instanceof ItemSlashBlade) {
                    if (stack.hasTagCompound()) {
                        if (stack.getTagCompound() != null && stack.getTagCompound().getBoolean("isAurora")) {
                            ItemSlashBlade.SummonedSwordColor.set(stack.getTagCompound(), auroraBladeColor.get(auroraBladeColorIndex));
                        }
                    }
                }
            }
        }

        if (!world.isRemote) {
            if (event.phase == TickEvent.Phase.START) {
                //MagChaosBladeExtraAttack
                if (playerProperties.hasKey(KaBladePlayerProp.MAG_CHAOS_BLADE_EXTRA_ATTACK_TICK)) {
                    if (playerProperties.getInteger(KaBladePlayerProp.MAG_CHAOS_BLADE_EXTRA_ATTACK_TICK) > 0) {
                        KaBladeEntityProperties.doIntegerLower(playerProperties, KaBladePlayerProp.MAG_CHAOS_BLADE_EXTRA_ATTACK_TICK);
                    } else {
                        float extraDamage = (float) MathFunc.amplifierCalc((ItemSlashBlade.BaseAttackModifier.get(event.player.getHeldItemMainhand().getTagCompound())),20f);
                        playerProperties.removeTag(KaBladePlayerProp.MAG_CHAOS_BLADE_EXTRA_ATTACK_TICK);
                        MagChaosBladeEffectRenderer.magChaosBladeEffectRenderers.add(new MagChaosBladeEffectRenderer(player));
                        Main.PACKET_HANDLER.sendToAll(new MessageMagChaosBladeEffectUpdate());
                        double dist = 6;
                        Vec3d vec3d = player.getPositionEyes(1.0F);
                        Vec3d vec3d1 = player.getLook(1.0F);
                        Vec3d vec3d2 = vec3d.add(vec3d1.x * dist, vec3d1.y * dist, vec3d1.z * dist);
                        List<Entity> pointedEntity = Lists.newArrayList();
                        List<Entity> list = world.getEntitiesInAABBexcluding(player, player.getEntityBoundingBox().expand(vec3d1.x * dist, vec3d1.y * dist, vec3d1.z * dist).grow(1.0D, 1.0D, 1.0D), Predicates.and(EntitySelectors.NOT_SPECTATING, entity -> entity != null && entity.canBeCollidedWith() && (entity instanceof EntityPlayer || entity instanceof EntityLiving)));
                        double d2 = dist;
                        for (Entity entity1 : list) {
                            AxisAlignedBB axisalignedbb = entity1.getEntityBoundingBox().grow(entity1.getCollisionBorderSize());
                            RayTraceResult raytraceresult = axisalignedbb.calculateIntercept(vec3d, vec3d2);

                            if (axisalignedbb.contains(vec3d)) {
                                if (d2 >= 0.0D) {
                                    pointedEntity.add(entity1);
                                    d2 = 0.0D;
                                }
                            } else if (raytraceresult != null) {
                                double d3 = vec3d.distanceTo(raytraceresult.hitVec);

                                if (d3 < d2 || d2 == 0.0D) {
                                    if (entity1.getLowestRidingEntity() == player.getLowestRidingEntity() && !player.canRiderInteract()) {
                                        if (d2 == 0.0D) {
                                            pointedEntity.add(entity1);
                                        }
                                    } else {
                                        pointedEntity.add(entity1);
                                        d2 = d3;
                                    }
                                }
                            }
                        }
                        if (!pointedEntity.isEmpty()) {
                            for (Entity e : pointedEntity) {
                                if (e instanceof EntityLivingBase && !(e instanceof EntityPlayer)) {
                                    e.attackEntityFrom(DamageSource.causePlayerDamage(player), 20f + extraDamage);
                                    ((EntityLivingBase) e).addPotionEffect(new PotionEffect(PotionInit.PARALY, 100, 3));
                                }
                            }
                        }
                    }
                }
                //KamiOfWar
                {

                    if (playerProperties.getInteger(KaBladePlayerProp.KAMI_OF_WAR_COUNT) > 0) {
                        flagikow = 0;
                        if (playerProperties.getInteger(KaBladePlayerProp.KAMI_OF_WAR_TICK) <= 0) {
                            float extraDamage = (float) MathFunc.amplifierCalc((ItemSlashBlade.BaseAttackModifier.get(event.player.getHeldItemMainhand().getTagCompound())),8f);
                            KaBladeEntityProperties.doIntegerLower(playerProperties, KaBladePlayerProp.KAMI_OF_WAR_COUNT);
                            playerProperties.setInteger(KaBladePlayerProp.KAMI_OF_WAR_TICK, 20);
                            KaBladePlayerProp.updateNBTForClient(player);
                            world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 4.0F, (1.0F + (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F) * 0.7F);
                            world.addWeatherEffect(new EntityLightningBolt(world, player.posX, player.posY, player.posZ, true));

                            for (int i = 0; i < 40; ++i) {
                                Random r1 = new Random();
                                Random r2 = new Random(r1.nextLong());
                                int state1;
                                int state2;
                                if (r1.nextBoolean()) {
                                    state1 = 1;
                                } else state1 = -1;
                                if (r2.nextBoolean()) {
                                    state2 = 1;
                                } else state2 = -1;
                                PACKET_HANDLER.sendToAll(new MessageSpawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, player.posX + (world.rand.nextDouble() * 2 * state1), player.posY + world.rand.nextDouble() * (double) player.height, player.posZ + (world.rand.nextDouble() * 2 * state2), 0.0D, 0.0D, 0.0D));
                                PACKET_HANDLER.sendToAll(new MessageSpawnColorfulSmoke(player.posX + (world.rand.nextDouble() * 2 * state1), player.posY + world.rand.nextDouble() * (double) player.height, player.posZ + (world.rand.nextDouble() * 2 * state2), new Vec3f(1f, 0.945f, 0.333f), 2));
                            }
                            AxisAlignedBB bb = player.getEntityBoundingBox();
                            bb = bb.grow(5, 4, 5);
                            bb = bb.offset(player.motionX, player.motionY, player.motionZ);
                            List<Entity> entities = world.getEntitiesInAABBexcluding(player, bb, input -> input != player && input instanceof EntityLivingBase);
                            for (Entity e : entities) {
                                e.attackEntityFrom(DamageSource.causeExplosionDamage(player), 8f + extraDamage);
                                if (e instanceof EntityLivingBase) {
                                    EntityLivingBase en = (EntityLivingBase) e;
                                    en.addPotionEffect(new PotionEffect(PotionInit.PARALY, 40, 2));
                                }
                            }
                        } else {
                            KaBladeEntityProperties.doIntegerLower(playerProperties, KaBladePlayerProp.KAMI_OF_WAR_TICK);
                            KaBladePlayerProp.updateNBTForClient(player);
                        }
                    } else if (flagikow == 0) {
                        KaBladePlayerProp.updateNBTForClient(player);
                        flagikow = 1;
                    }
                }
                //WindEnchantmentBoost
                {
                    if (playerProperties.getInteger(KaBladePlayerProp.WIND_ENCHANTMENT_BOOST) > 0) {
                        flagweb = 0;
                        KaBladeEntityProperties.doIntegerLower(playerProperties, KaBladePlayerProp.WIND_ENCHANTMENT_BOOST);
                        KaBladePlayerProp.updateNBTForClient(player);
                        AbstractAttributeMap map = player.getAttributeMap();
                        if (map.getAttributeInstance(SharedMonsterAttributes.MOVEMENT_SPEED).getModifier(UUID_WIND_ENCHANTMENT) == null) {
                            map.getAttributeInstance(SharedMonsterAttributes.MOVEMENT_SPEED).applyModifier(new AttributeModifier(UUID_WIND_ENCHANTMENT, "wind_enchantment", 0.5, 1));
                        }
                        if (map.getAttributeInstance(SharedMonsterAttributes.ATTACK_SPEED).getModifier(UUID_WIND_ENCHANTMENT) == null) {
                            map.getAttributeInstance(SharedMonsterAttributes.ATTACK_SPEED).applyModifier(new AttributeModifier(UUID_WIND_ENCHANTMENT, "wind_enchantment", 0.25, 1));
                        }
                        if (map.getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE).getModifier(UUID_WIND_ENCHANTMENT) == null) {
                            map.getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE).applyModifier(new AttributeModifier(UUID_WIND_ENCHANTMENT, "wind_enchantment", 0.2, 1));
                        }
                        KaBladePlayerProp.updateNBTForClient(player);
                    } else {
                        AbstractAttributeMap map = player.getAttributeMap();
                        if (map.getAttributeInstance(SharedMonsterAttributes.MOVEMENT_SPEED).getModifier(UUID_WIND_ENCHANTMENT) != null) {
                            map.getAttributeInstance(SharedMonsterAttributes.MOVEMENT_SPEED).removeModifier(UUID_WIND_ENCHANTMENT);
                        }
                        if (map.getAttributeInstance(SharedMonsterAttributes.ATTACK_SPEED).getModifier(UUID_WIND_ENCHANTMENT) != null) {
                            map.getAttributeInstance(SharedMonsterAttributes.ATTACK_SPEED).removeModifier(UUID_WIND_ENCHANTMENT);
                        }
                        if (map.getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE).getModifier(UUID_WIND_ENCHANTMENT) != null) {
                            map.getAttributeInstance(SharedMonsterAttributes.ATTACK_DAMAGE).removeModifier(UUID_WIND_ENCHANTMENT);
                        }
                        if (flagweb == 0) {
                            KaBladePlayerProp.updateNBTForClient(player);
                            flagweb = 1;
                        }

                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void EntityShuaxinDizui(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        World world = entity.world;
        if (entity.getEntityData().getBoolean("dizui")) {
            if (entity.getEntityData().getInteger("dizuitime") > 0) {
                entity.getEntityData().setInteger("dizuitime", entity.getEntityData().getInteger("dizuitime") - 1);
                int state1;
                int state2;
                if (world.rand.nextBoolean()) {
                    state1 = 1;
                } else state1 = -1;
                if (world.rand.nextBoolean()) {
                    state2 = 1;
                } else state2 = -1;
                if (world.isRemote) {
                    world.spawnParticle(EnumParticleTypes.END_ROD, entity.posX + world.rand.nextDouble() * state1, entity.posY + (entity.height / 2), entity.posZ + world.rand.nextDouble() * state2, 0.0D, 0.0D, 0.0D);
                }
            } else if (entity.getEntityData().getInteger("dizuitime") <= 0) {
                entity.getEntityData().setBoolean("dizui", false);
            }
        }
        if (entity.getEntityData().getBoolean("dizuialive")) {
            entity.getEntityData().setBoolean("dizui", false);
            entity.getEntityData().setBoolean("dizuialive", false);
            EntityLivingBase attacker = entity.getLastAttackedEntity();
            //entity.setLastAttackedEntity(attacker);
            if (attacker instanceof EntityPlayer) {
                entity.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) attacker), 25);
            } else {
                entity.attackEntityFrom(DamageSource.causeMobDamage(attacker), 25);
            }
            for (int i = 0; i < 10; ++i) {
                Random r1 = new Random();
                Random r2 = new Random(r1.nextLong());
                int state1;
                int state2;
                if (r1.nextBoolean()) {
                    state1 = 1;
                } else state1 = -1;
                if (r2.nextBoolean()) {
                    state2 = 1;
                } else state2 = -1;
                world.rand.nextBoolean();
                world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, entity.posX + (world.rand.nextDouble() * 4 * state1), entity.posY + world.rand.nextDouble() * entity.height, entity.posZ + (world.rand.nextDouble() * 4 * state2), 0.0D, 0.0D, 0.0D);

            }
            AxisAlignedBB bb = entity.getEntityBoundingBox();
            bb = bb.grow(4.0D, 4.0D, 4.0D);
            //bb = bb.offset(entity.motionX, entity.motionY, entity.motionZ);
            List<Entity> list = entity.world.getEntitiesInAABBexcluding(entity, bb, input -> input.isEntityAlive() && !(entity instanceof EntityPlayer));
            if (!list.isEmpty()) {
                for (Entity entitys : list) {
                    if (entitys instanceof EntityLivingBase && !(entitys instanceof EntityPlayer)) {
                        //((EntityLivingBase) entitys).setLastAttackedEntity(attacker);
                        if (attacker instanceof EntityPlayer) {
                            entitys.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) attacker), 10);
                        } else {
                            entitys.attackEntityFrom(DamageSource.causeMobDamage(attacker), 10);
                        }
                        entitys.getEntityData().setInteger("dizuitime", 300);
                        entitys.getEntityData().setBoolean("dizui", true);
                        PACKET_HANDLER.sendToAll(new MessageDizuiKuo(entitys.getEntityId()));

                    }
                }

            }
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        IBlockState state = event.getState();
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (!world.isRemote) {
            if (state.getBlock() == Blocks.STONE) {
                if (Math.random() < 0.003) {
                    EntityItem item = new EntityItem(world, x, y, z, new ItemStack(ItemInit.GRAVITY_NUGGET));
                    world.spawnEntity(item);
                }
            }
            if (state.getBlock() instanceof IKabladeOre) {
                if (Math.random() < 0.01) {
                    EntityItem item = new EntityItem(world, x, y, z, new ItemStack(ItemInit.GRAVITY_NUGGET));
                    world.spawnEntity(item);
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        EntityLivingBase e = event.getEntityLiving();
        World world = e.getEntityWorld();
        NBTTagCompound kec = KaBladeEntityProperties.getPropCompound(e);
        //Shield
        if (!world.isRemote) {
            if (e instanceof EntityPlayer) {
                ArrayList<AbsEntityShield> list = Lists.newArrayList();
                EntityPlayer player = (EntityPlayer) e;
                List<Entity> entities = PlayerThrowableHandler.getAllThrowableForPlayer(world, player);
                if (!entities.isEmpty()) {
                    for (Entity e1 : entities) {
                        if (e1 instanceof AbsEntityShield) {
                            list.add((AbsEntityShield) e1);
                        }
                    }
                    if (!list.isEmpty()) {
                        float damage = event.getAmount();
                        for (AbsEntityShield shield : list) {
                            if (damage <= 0) {
                                break;
                            }
                            if (shield.getShieldBlood() > damage) {
                                shield.setShieldBlood(shield.getShieldBlood() - damage);
                                damage = 0;
                            }
                            if (shield.getShieldBlood() == damage) {
                                if (damage > 0) {
                                    shield.setShieldBlood(0);
                                }
                                damage = 0;
                            }
                            if (shield.getShieldBlood() < damage) {
                                shield.setShieldBlood(0);
                                damage -= shield.getShieldBlood();
                            }
                        }
                        event.setAmount(damage);
                    }
                }
            }
        }

        {
            //EnchantmentFreezyBlades
            if (!world.isRemote) {
                if (event.getSource().getImmediateSource() != null) {
                    if (event.getSource().getImmediateSource() instanceof EntityLivingBase) {
                        EntityLivingBase attacker = (EntityLivingBase) event.getSource().getImmediateSource();
                        ItemStack stack = attacker.getHeldItemMainhand();
                        int level = EnchantmentHelper.getEnchantmentLevel(EnchantmentInit.ENCHANTMENT_SLOW, stack);
                        if (level > 0) {
                            if (event.getEntityLiving() != null) {
                                if (stack.getItem() instanceof ItemSlashBlade) {
                                    if (event.getEntityLiving().hasCapability(CapabilityLoader.SlashPotion, null)) {
                                        Capability.IStorage<IPotionInSlash> storage = CapabilityLoader.SlashPotion.getStorage();
                                        IPotionInSlash potions = event.getEntityLiving().getCapability(CapabilityLoader.SlashPotion, null);
                                        if (potions != null) {
                                            NBTTagCompound compound = CapabilitySlashPotion.initNBT(potions);
                                            compound.setInteger(NBT_SLOW_LEVEL, level);
                                            compound.setInteger(NBT_SLOW_TIME, 50 * level);
                                            storage.readNBT(CapabilityLoader.SlashPotion, potions, null, compound);
                                        }
                                    }
                                } else
                                    event.getEntityLiving().addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 50 * level, level));
                            }
                        }
                    }
                }
            }
            //SakuraBrand
            if (!world.isRemote) {
                if (event.getSource().getImmediateSource() != null && KaBladeEntityProperties.getPropCompound(event.getEntityLiving()).getInteger(KaBladeEntityProperties.FALLING_PETALS) > 0) {
                    if (event.getSource().getImmediateSource() instanceof EntityLivingBase) {
                        EntityLivingBase attacker = (EntityLivingBase) event.getSource().getImmediateSource();
                        if (attacker instanceof EntityPlayer) {
                            event.setAmount(event.getAmount() * 2f);
                            event.getEntityLiving().addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 40, 2));
                        }
                    }
                }
            }
        }
        //PotionParaly
        {
            PotionEffect effect = e.getActivePotionEffect(PotionInit.PARALY);
            if (effect != null) {
                if (effect.getDuration() > 0 && effect.getAmplifier() > 0) {
                    event.setAmount(event.getAmount() * (1.2f * (1f + (effect.getAmplifier() * 0.1f))));
                }
            }
        }
        //FreezeDomainBooster
        {
            if (kec.hasKey(KaBladeEntityProperties.FREEZE_DOMAIN_DAMAGE_BOOSTER)) {
                if (event.getAmount() > 0)
                    event.setAmount(event.getAmount() * 1.4f);
            }
        }

    }

    @SubscribeEvent
    public void onAttachCapabilitiesEntity(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof EntityLivingBase) {
            ICapabilitySerializable<NBTTagCompound> provider = new CapabilitySlashPotion.ProviderEntity();
            event.addCapability(new ResourceLocation(Reference.MODID + ":" + "slash_potion"), provider);
        }
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityLivingBase) {
            EntityLivingBase livingBase = (EntityLivingBase) event.getEntity();
            if (livingBase.hasCapability(CapabilityLoader.SlashPotion, null)) {
                Capability.IStorage<IPotionInSlash> storage = CapabilityLoader.SlashPotion.getStorage();
                NBTTagCompound compound = (NBTTagCompound) Objects.requireNonNull(storage.writeNBT(CapabilityLoader.SlashPotion, livingBase.getCapability(CapabilityLoader.SlashPotion, null), null)).copy();
                compound.setDouble(NBT_ORIGIN_MOVEMENT, livingBase.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
                storage.readNBT(CapabilityLoader.SlashPotion, livingBase.getCapability(CapabilityLoader.SlashPotion, null), null, compound);
            }
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onPlayerRender(RenderWorldLastEvent event) {
        for (EntityPlayer p : Minecraft.getMinecraft().world.playerEntities) {
            if (p != null) {
                List<Entity> parts = PlayerThrowableHandler.getAllThrowableForPlayer(p.world, p);
                EntityPlayer c = Minecraft.getMinecraft().player;
                if (!parts.isEmpty()) {
                    for (Entity e1 : parts) {
                        if (e1 instanceof EntityRaikiriBlade) {
                            double vx, vy, vz;
                            if (p == c) {
                                vx = 0d;
                                vy = 0d;
                                vz = 0d;
                            } else {
                                vx = p.posX - c.posX;
                                vy = p.posY - c.posY;
                                vz = p.posZ - c.posZ;
                            }
                            GlStateManager.disableTexture2D();
                            GlStateManager.disableLighting();
                            GlStateManager.enableBlend();
                            float lastx = OpenGlHelper.lastBrightnessX;
                            float lasty = OpenGlHelper.lastBrightnessY;
                            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
                            GlStateManager.pushMatrix();
                            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                            GlStateManager.scale(1f, 1f, 1f);
                            GlStateManager.translate(vx, vy, vz);
                            GlStateManager.rotate(angleManager.getAngle(), 0f, 1f, 0f);

                            GlStateManager.color(0f, 1f, 1f, 0.5f);

                            RenderRaikiriBlade.mainModel.render(e1, event.getPartialTicks(), 0.0F, -0.1F, 0.0F, 0.0F, 0.0625F);
                            GlStateManager.popMatrix();
                            GlStateManager.disableBlend();
                            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastx, lasty);
                            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
                            GlStateManager.enableLighting();
                            GlStateManager.enableTexture2D();
                        }
                    }
                }
            }
        }
        //RenderMagChaosBladeEffect
        if (!MagChaosBladeEffectRenderer.magChaosBladeEffectRenderers.isEmpty()) {
            try {


                for (MagChaosBladeEffectRenderer renderer : MagChaosBladeEffectRenderer.magChaosBladeEffectRenderers) {
                    Entity e = Minecraft.getMinecraft().world.getEntityByID(renderer.playerID);
                    if (e instanceof EntityPlayer) {
                        EntityPlayer targetPlayer = (EntityPlayer) e;
                        EntityPlayer ownerPlayer = Minecraft.getMinecraft().player;
                        double vx, vy, vz;
                        if (targetPlayer == ownerPlayer) {
                            vx = 0d;
                            vy = 0d;
                            vz = 0d;
                        } else {
                            vx = targetPlayer.posX - ownerPlayer.posX;
                            vy = targetPlayer.posY - ownerPlayer.posY;
                            vz = targetPlayer.posZ - ownerPlayer.posZ;
                        }
                        GlStateManager.disableLighting();
                        GlStateManager.enableBlend();
                        float lastx = OpenGlHelper.lastBrightnessX;
                        float lasty = OpenGlHelper.lastBrightnessY;
                        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
                        GlStateManager.pushMatrix();
                        GlStateManager.translate(vx, vy, vz);
                        GlStateManager.rotate(targetPlayer.rotationYaw, 0f, -1f, 0f);
                        GlStateManager.rotate(-90f, 1f, 0f, 0f);
                        GlStateManager.translate(0f, -2f, 0f);
                        GlStateManager.translate(0f, 0f, 1f);
                        GlStateManager.rotate(180f, 0f, 0f, 1f);
                        GlStateManager.scale(4f, 4f, 4f);
                        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                        Minecraft.getMinecraft().getTextureManager().bindTexture(MagChaosBladeEffectIcon);
                        Tessellator tessellator = Tessellator.getInstance();
                        BufferBuilder buffer = tessellator.getBuffer();
                        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_NORMAL);
                        buffer.pos(-0.5d, -0.25d, 0).tex(0, 1).normal(0.0f, 1.0f, 0.0f).endVertex();
                        buffer.pos(0.5d, -0.25d, 0).tex(1, 1).normal(0.0f, 1.0f, 0.0f).endVertex();
                        buffer.pos(0.5d, 0.75d, 0).tex(1, 0).normal(0.0f, 1.0f, 0.0f).endVertex();
                        buffer.pos(-0.5d, 0.75d, 0).tex(0, 0).normal(0.0f, 1.0f, 0.0f).endVertex();
                        tessellator.draw();
                        GlStateManager.popMatrix();
                        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastx, lasty);
                        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
                        GlStateManager.disableBlend();
                        GlStateManager.enableLighting();
                    }
                }
            } catch (Exception e) {
                logger.error("MagChaosBladeEffectRenderer has a error but no large effect!\n" + e.getMessage());
            }
        }
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onLivingRender(RenderLivingEvent.Specials.Pre<EntityLivingBase> event) {
        EntityLivingBase entity = event.getEntity();
        if (entity.posX == 0 && entity.posY == 0 && entity.posZ == 0)
            return;
        Minecraft mc = Minecraft.getMinecraft();
        if (KaBladePlayerProp.getPropCompound(mc.player).hasKey(KaBladePlayerProp.LOCKING_ENTITY_UUID)) {
            if (EntityUUIDManager.getEntitiesFromUUID(KaBladePlayerProp.getPropCompound(mc.player).getString(KaBladePlayerProp.LOCKING_ENTITY_UUID), mc.player.world).contains(entity)) {
                GlStateManager.disableLighting();
                GlStateManager.enableBlend();
                GlStateManager.pushMatrix();
                GlStateManager.depthFunc(GL11.GL_ALWAYS);
                GlStateManager.color(1f, 1f, 1f, 0.8f);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                GlStateManager.depthMask(false);

                Minecraft.getMinecraft().getTextureManager().bindTexture(HuntingLockerIcon);
                Tessellator tess = Tessellator.getInstance();
                BufferBuilder buffer = tess.getBuffer();


                GlStateManager.translate(event.getX(), event.getY() + ((event.getEntity().getEntityBoundingBox().maxY - event.getEntity().getEntityBoundingBox().minY) / 2), event.getZ());
                GlStateManager.rotate(180f - mc.getRenderManager().playerViewY, 0, 1, 0);
                GlStateManager.rotate(-mc.getRenderManager().playerViewX, 1, 0, 0);


                buffer.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
                buffer.pos(-0.5d, -0.25d, 0).tex(0, 1).normal(0.0f, 1.0f, 0.0f).endVertex();
                buffer.pos(0.5d, -0.25d, 0).tex(1, 1).normal(0.0f, 1.0f, 0.0f).endVertex();
                buffer.pos(0.5d, 0.75d, 0).tex(1, 0).normal(0.0f, 1.0f, 0.0f).endVertex();
                buffer.pos(-0.5d, 0.75d, 0).tex(0, 0).normal(0.0f, 1.0f, 0.0f).endVertex();
                tess.draw();
                GlStateManager.depthMask(true);
                GlStateManager.depthFunc(GL11.GL_LEQUAL);
                GlStateManager.popMatrix();
                GlStateManager.disableBlend();
                GlStateManager.enableLighting();

            }
        }
        if (KaBladeEntityProperties.getPropCompound(entity).getInteger(KaBladeEntityProperties.FALLING_PETALS) > 0) {
            GlStateManager.disableLighting();
            GlStateManager.enableBlend();
            GlStateManager.pushMatrix();
            GlStateManager.depthFunc(GL11.GL_ALWAYS);
            GlStateManager.color(1f, 1f, 1f, 1f);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            GlStateManager.depthMask(false);

            Minecraft.getMinecraft().getTextureManager().bindTexture(SakuraBrand);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buffer = tess.getBuffer();


            GlStateManager.translate(event.getX(), event.getY() + ((event.getEntity().getEntityBoundingBox().maxY - event.getEntity().getEntityBoundingBox().minY) / 2), event.getZ());
            GlStateManager.rotate(180f - mc.getRenderManager().playerViewY, 0, 1, 0);
            GlStateManager.rotate(-mc.getRenderManager().playerViewX, 1, 0, 0);
            GlStateManager.rotate(angleManager.getAngle(), 0, 0, 2);


            buffer.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL);
            buffer.pos(-0.5d, -0.5d, 0).tex(0, 1).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(0.5d, -0.5d, 0).tex(1, 1).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(0.5d, 0.5d, 0).tex(1, 0).normal(0.0f, 1.0f, 0.0f).endVertex();
            buffer.pos(-0.5d, 0.5d, 0).tex(0, 0).normal(0.0f, 1.0f, 0.0f).endVertex();
            tess.draw();
            GlStateManager.depthMask(true);
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.popMatrix();
            GlStateManager.disableBlend();
            GlStateManager.enableLighting();
        }
    }

    @SubscribeEvent
    public void onPlantBreakEvent(BlockEvent.BreakEvent event) {
        World world = event.getWorld();
        if (!world.isRemote) {
            Block block = event.getState().getBlock();
            if (block == Blocks.GRASS || block == Blocks.LEAVES || block == Blocks.LEAVES2||block == Blocks.TALLGRASS) {
                if (Math.random() < 0.03) {
                    int x = event.getPos().getX();
                    int y = event.getPos().getY();
                    int z = event.getPos().getZ();
                    EntityItem drop = new EntityItem(world, x, y + 0.5, z, new ItemStack(ItemInit.PETAL));
                    world.spawnEntity(drop);
                }
            }
        }
    }
}
