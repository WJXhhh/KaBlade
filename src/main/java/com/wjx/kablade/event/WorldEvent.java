package com.wjx.kablade.event;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.realmsclient.gui.ChatFormatting;
import com.wjx.kablade.Entity.AbsEntityShield;
import com.wjx.kablade.Entity.model.mdlRaikiriBlade;
import com.wjx.kablade.Main;
import com.wjx.kablade.SlashBlade.blades.bladeitem.MagicBlade;
import com.wjx.kablade.init.ItemInit;
import com.wjx.kablade.util.handlers.PlayerThrowableHandler;
import com.wjx.kablade.util.interfaces.IEntityShield;
import com.wjx.kablade.util.interfaces.IKabladeOre;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderBoat;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static com.wjx.kablade.SlashBlade.BladeLoader.ITEM_MAGIC;

@Mod.EventBusSubscriber
public class WorldEvent {

    public WorldEvent(){
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static int RaikiriRotation = 0;

    public static Set<Class<? extends Entity>> antiEntity = Sets.newHashSet();

    @SubscribeEvent
    public void onEntityItemJoinWorld(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        for (Class<? extends Entity> clazz : antiEntity) {
            if (clazz.isInstance(entity)) {
                Main.logger.info("checked "+ clazz);
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public void onPlayerJoinWorld(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        if (Main.YesUpdate){
            if (!player.world.isRemote){
                player.sendStatusMessage(new TextComponentString("§6§l[斩无不断]§b检测到模组有更新。最新版本为：§6"+Main.GetUrlVersion),false);
                TextComponentString t = new TextComponentString("§9https://www.mcmod.cn/class/8128.html");
                t.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,"https://www.mcmod.cn/class/8128.html")).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("点击前往链接")));
                player.sendStatusMessage(new TextComponentString("§6§l[斩无不断]§b可从MC百科下载或从其界面寻找蓝奏云下载地址：").appendSibling(t),false);
            }
        }
    }


    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof MagicBlade) {
            for(int x = 0; x < event.getToolTip().size(); ++x) {
                if (((String)event.getToolTip().get(x)).contains(I18n.translateToLocal("attribute.name.generic.attackDamage")) || ((String)event.getToolTip().get(x)).contains(I18n.translateToLocal("Attack Damage"+""))) {
                    event.getToolTip().set(x,  ChatFormatting.BLUE + " +" + UpdateColor.makeColourRainbow(I18n.translateToLocal("info.damageguer1111.name"))+" "+ChatFormatting.BLUE + I18n.translateToLocal("attribute.name.generic.attackDamage") );
                    return;
                }
            }
        }

    }

    @SubscribeEvent
    public void PlayerDeadProtect(LivingDeathEvent event){
        if(event.getEntityLiving() instanceof EntityPlayer){
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            if(player.inventory.hasItemStack(new ItemStack(ITEM_MAGIC))||player.getHeldItemMainhand().getItem() instanceof MagicBlade){
                player.setHealth(player.getMaxHealth());
                player.deathTime=0;
                player.isDead=false;
                player.preparePlayerToSpawn();
                if(event.isCancelable())
                {
                    event.setCanceled(true);
                }

            }
        }
    }
    @SubscribeEvent
    public void PlayerHurtProtect(LivingHurtEvent event){
        if(event.getEntityLiving() instanceof EntityPlayer){
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            if(player.inventory.hasItemStack(new ItemStack(ITEM_MAGIC))||player.getHeldItemMainhand().getItem() instanceof MagicBlade){
                player.setHealth(player.getMaxHealth());
                player.deathTime=0;
                player.isDead=false;
                if(event.isCancelable())
                {
                    event.setCanceled(true);
                }

            }
        }
    }


    @SubscribeEvent
    public void PlayerUpdattingProtect(LivingEvent.LivingUpdateEvent event){
        if(event.getEntityLiving() instanceof EntityPlayer){
            EntityPlayer player = (EntityPlayer) event.getEntityLiving();
            if(player.inventory.hasItemStack(new ItemStack(ITEM_MAGIC))||player.getHeldItemMainhand().getItem() instanceof MagicBlade){
                if(player.isDead || player.getHealth()<=0)
                {
                    player.setHealth(player.getMaxHealth());
                    player.deathTime = 0;
                    player.isDead = false;
                    player.preparePlayerToSpawn();
                }

            }
        }

    }



    @SubscribeEvent
    public void LivingUpdate(LivingEvent.LivingUpdateEvent event){
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.getEntityData().getInteger("frost_blade_1") > 0){
            if (!entity.world.isRemote){
                entity.getEntityData().setInteger("frost_blade_1",entity.getEntityData().getInteger("frost_blade_1")-1);
                entity.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS,60,2));
            }
        }
    }

    @SubscribeEvent
    public void PlayerUpdateEvent(TickEvent.PlayerTickEvent event){
        EntityPlayer player = event.player;
        World world = event.player.world;
        //Chop Willow
        if (player.getEntityData().getBoolean("to_chop_willow")){
            player.getEntityData().setInteger("chop_willow_retry_count",0);
            player.getEntityData().setBoolean("to_chop_willow",false);
            player.getEntityData().setBoolean("start_chop_willow",true);
            player.getEntityData().setInteger("chop_willow",10);
        }
        if (player.getEntityData().getBoolean("start_chop_willow")){
            if (player.getEntityData().getInteger("chop_willow") > -1){
                player.getEntityData().setInteger("chop_willow",player.getEntityData().getInteger("chop_willow")-1);
            }
            else {
                boolean retry = false;
                int RightEntityCount = 0;
                player.getEntityData().setBoolean("start_chop_willow",false);
                AxisAlignedBB bb = player.getEntityBoundingBox();
                bb = bb.grow(4.0D, 2D, 4.0D);
                bb = bb.offset(player.motionX, player.motionY, player.motionZ);
                List<Entity> list = world.getEntitiesInAABBexcluding(player, bb, input -> input != player && input.isEntityAlive());
                for (Entity entity:list){
                    if (entity instanceof EntityLivingBase){
                        entity.attackEntityFrom(DamageSource.causePlayerDamage(player),4);
                        RightEntityCount++;
                    }
                }
                if (RightEntityCount == 0){
                    retry = true;
                }
                if (player.getEntityData().getInteger("chop_willow_retry_count") > 40){
                    player.getEntityData().setInteger("chop_willow_retry_count",41);
                    retry =false;
                }
                if (world.isRemote){
                    double x1 = player.posX;
                    double y1 = player.posY;
                    double z1 = player.posZ;

                    for (int i = 0; i < 10; ++i)
                    {
                        int state1;
                        int state2;
                        if (world.rand.nextBoolean()){
                            state1 = 1;
                        }
                        else state1 = -1;
                        if (world.rand.nextBoolean()){
                            state2 = 1;
                        }
                        else state2 = -1;
                        world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, x1 + (world.rand.nextDouble() * state1), y1 + world.rand.nextDouble() * (double)player.height/2, z1 + (world.rand.nextDouble()* state2), 0.0D, 0.0D, 0.0D);
                    }
                }
                if (!retry){
                    return;
                }
                if (player.getEntityData().getInteger("chop_willow_retry_count") <= 40){
                    player.getEntityData().setBoolean("start_chop_willow",true);
                    player.getEntityData().setInteger("chop_willow_retry_count",player.getEntityData().getInteger("chop_willow_retry_count") + 1);
                }
            }
        }
    }

    @SubscribeEvent
    public void EntityShuaxinDizui(LivingEvent.LivingUpdateEvent event){
        EntityLivingBase entity = event.getEntityLiving();
        World world = entity.world;
        if(entity.getEntityData().getBoolean("dizui")){
            if(entity.getEntityData().getInteger("dizuitime")>0){
                entity.getEntityData().setInteger("dizuitime",entity.getEntityData().getInteger("dizuitime")-1);
                int state1 = 0;
                int state2 = 0;
                if (world.rand.nextBoolean()){
                    state1 = 1;
                }
                else state1 = -1;
                if (world.rand.nextBoolean()){
                    state2 = 1;
                }
                else state2 = -1;
                if (world.isRemote){
                    world.spawnParticle(EnumParticleTypes.END_ROD,entity.posX+world.rand.nextDouble()*state1,entity.posY+(entity.height/2),entity.posZ+world.rand.nextDouble()*state2,0.0D,0.0D,0.0D);
                }else {
                    world.spawnParticle(EnumParticleTypes.END_ROD,entity.posX+world.rand.nextDouble()*state1,entity.posY+(entity.height/2),entity.posZ+world.rand.nextDouble()*state2,0.0D,0.0D,0.0D);
                }
            }else if(entity.getEntityData().getInteger("dizuitime")<=0){
                entity.getEntityData().setBoolean("dizui",false);
            }
        }
        if(entity.getEntityData().getBoolean("dizuialive")){
            entity.getEntityData().setBoolean("dizui",false);
            entity.getEntityData().setBoolean("dizuialive",false);
            EntityLivingBase attacker = entity.getLastAttackedEntity();
            if (attacker!=null){
                //entity.setLastAttackedEntity(attacker);
                if (attacker instanceof EntityPlayer){
                    entity.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) attacker),15);
                }
                else {
                    entity.attackEntityFrom(DamageSource.causeMobDamage(attacker),15);
                }
            }
            else entity.attackEntityFrom(DamageSource.LIGHTNING_BOLT,20);
            for (int i = 0; i < 30; ++i)
            {
                Random r1 =new Random();
                Random r2 =new Random(r1.nextLong());
                int state1;
                int state2;
                int state3;
                if (r1.nextBoolean()){
                    state1 = 1;
                }
                else state1 = -1;
                if (r2.nextBoolean()){
                    state2 = 1;
                }
                else state2 = -1;
                if (world.rand.nextBoolean()){
                    state3 = 1;
                }
                else state3 = -1;
                world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, entity.posX + (world.rand.nextDouble() * 4 * state1), entity.posY + world.rand.nextDouble() *entity.height, entity.posZ + (world.rand.nextDouble() * 4 * state2), 0.0D, 0.0D, 0.0D);

            }
            AxisAlignedBB bb = entity.getEntityBoundingBox();
            bb = bb.grow(3.0D, 3.0D, 3.0D);
            bb = bb.offset(entity.motionX, entity.motionY, entity.motionZ);
            List<Entity> list = entity.world.getEntitiesInAABBexcluding(entity, bb, input -> !(input instanceof EntityPlayer) && input.isEntityAlive());
            if (list.size()!=0){
                for (Entity entitys: list){
                    if (entitys instanceof EntityLivingBase){
                        if (attacker!=null){
                            //((EntityLivingBase) entitys).setLastAttackedEntity(attacker);
                            if (attacker instanceof EntityPlayer){
                                entitys.attackEntityFrom(DamageSource.causePlayerDamage((EntityPlayer) attacker),10);
                            }
                            else {
                                entitys.attackEntityFrom(DamageSource.causeMobDamage(attacker),10);
                            }
                        }
                        else entitys.attackEntityFrom(DamageSource.LIGHTNING_BOLT,10);
                        entitys.getEntityData().setBoolean("dizui",true);
                        entitys.getEntityData().setInteger("dizuitime", 300);
                    }
                }

            }else{
                Main.logger.info("dizuisaofkuosan:isEmpty");
            }
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event){
        IBlockState state = event.getState();
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (!world.isRemote){
            if (state.getBlock() == Blocks.STONE){
                if (Math.random() < 0.003){
                    EntityItem item = new EntityItem(world,x,y,z,new ItemStack(ItemInit.GRAVITY_NUGGET));
                    world.spawnEntity(item);
                }
            }
            if (state.getBlock() instanceof IKabladeOre){
                if (Math.random() < 0.01){
                    EntityItem item = new EntityItem(world,x,y,z,new ItemStack(ItemInit.GRAVITY_NUGGET));
                    world.spawnEntity(item);
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event){
        EntityLivingBase e = event.getEntityLiving();
        World world = e.getEntityWorld();
        //Shield
        {
            if (e instanceof EntityPlayer){
                ArrayList<AbsEntityShield> list = Lists.newArrayList();
                EntityPlayer player = (EntityPlayer)e;
                List<Entity> entities = PlayerThrowableHandler.getAllThrowableForPlayer(world, player);
                if (!entities.isEmpty()){
                    for (Entity e1 : entities){
                        if (e1 instanceof AbsEntityShield){
                            list.add((AbsEntityShield) e1);
                        }
                    }
                    if (!list.isEmpty()){
                        float damage = event.getAmount();
                        for (AbsEntityShield shield : list){
                            if (damage <= 0){
                                break;
                            }
                            if (shield.getShieldBlood() > damage){
                                if (damage > 0){
                                    shield.setShieldBlood(shield.getShieldBlood() - damage);
                                    damage = 0;
                                }
                            }
                            if (shield.getShieldBlood() == damage){
                                if (damage > 0){
                                    shield.setShieldBlood(0);
                                }
                                damage = 0;
                            }
                            if (shield.getShieldBlood() < damage){
                                shield.setShieldBlood(0);
                                damage -= shield.getShieldBlood();
                            }
                        }
                        event.setAmount(damage);
                    }
                }
            }
        }

    }

}
