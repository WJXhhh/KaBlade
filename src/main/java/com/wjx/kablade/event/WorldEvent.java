package com.wjx.kablade.event;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.realmsclient.gui.ChatFormatting;
import com.wjx.kablade.Entity.AbsEntityShield;
import com.wjx.kablade.Entity.EntitySummonedSwordBasePlus;
import com.wjx.kablade.Entity.EntityWine;
import com.wjx.kablade.Lib;
import com.wjx.kablade.Main;
import com.wjx.kablade.SlashBlade.blades.bladeitem.MagicBlade;
import com.wjx.kablade.capability.CapabilityLoader;
import com.wjx.kablade.capability.CapabilitySlashPotion;
import com.wjx.kablade.capability.inters.IPotionInSlash;
import com.wjx.kablade.init.EnchantmentInit;
import com.wjx.kablade.init.ItemInit;
import com.wjx.kablade.init.PotionInit;
import com.wjx.kablade.network.MessageResetSend;
import com.wjx.kablade.network.MessageSpawnParticle;
import com.wjx.kablade.util.KaBladeEntityProperties;
import com.wjx.kablade.util.Reference;
import com.wjx.kablade.util.handlers.PlayerThrowableHandler;
import com.wjx.kablade.util.interfaces.IKabladeOre;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.ResourceLocationRaw;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
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
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
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

import java.util.*;

import static com.wjx.kablade.Lib.*;
import static com.wjx.kablade.Main.*;
import static com.wjx.kablade.SlashBlade.BladeLoader.ITEM_MAGIC;

@Mod.EventBusSubscriber
public class WorldEvent {

    public WorldEvent(){
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static ArrayList<Integer> auroraBladeColor = Lists.newArrayList();
    public static int auroraBladeColorIndex = 0;

     public static void loadEvent(){
        //auroraColor
         auroraBladeColor.clear();
         auroraBladeColorIndex = 0;
        int g = 196;
        int b = 255;
        while (g < 255){
            int c = Lib.rgbToMetrication(0,g,b);
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
     public void onWorldUpdate(TickEvent.WorldTickEvent event){

         if (!auroraBladeColor.isEmpty()){

             if (auroraBladeColorIndex>= (auroraBladeColor.size() -2)){
                 auroraBladeColorIndex = 0;
             }
             else auroraBladeColorIndex++;
         }
     }

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
        if(!player.world.isRemote){
            if(YesUpdate){
                PACKET_HANDLER.sendTo(new MessageResetSend(), (EntityPlayerMP) player);
            }
        }
        if (!Main.isBladePostLoad){
            Main.isBladePostLoad = true;
            ItemStack stack = SlashBlade.findItemStack(bladestr, "wjx.blade.honkai.plasma_kagehide", 1);
            stack.addEnchantment(EnchantmentInit.ENCHANTMENT_SLOW,1);
            SlashBlade.BladeRegistry.put(new ResourceLocationRaw(bladestr,"wjx.blade.honkai.plasma_kagehide"),stack);
            addEnchantmentForBlade(EnchantmentInit.ENCHANTMENT_SLOW,1,"wjx.blade.honkai.xuanyuan_katana");
        }
    }

    void addEnchantmentForBlade(Enchantment e,int level,String name){
        ItemStack stack = SlashBlade.findItemStack(bladestr, name, 1);
        stack.addEnchantment(e,level);
        SlashBlade.BladeRegistry.put(new ResourceLocationRaw(bladestr,name),stack);
    }


    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof MagicBlade) {
            for(int x = 0; x < event.getToolTip().size(); ++x) {
                if (event.getToolTip().get(x).contains(I18n.translateToLocal("attribute.name.generic.attackDamage")) || event.getToolTip().get(x).contains(I18n.translateToLocal("Attack Damage"+""))) {
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
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event){
        EntityLivingBase entity = event.getEntityLiving();
        World world = entity.world;
        NBTTagCompound KaBladeCompound = KaBladeEntityProperties.getPropCompound(entity);
        if (!entity.world.isRemote){
            if (entity.getEntityData().getInteger("frost_blade_1") > 0){
                if (!entity.world.isRemote){
                    entity.getEntityData().setInteger("frost_blade_1",entity.getEntityData().getInteger("frost_blade_1")-1);
                    entity.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS,60,2));
                }
            }
            if (entity.hasCapability(CapabilityLoader.SlashPotion,null)){
                IPotionInSlash slash = entity.getCapability(CapabilityLoader.SlashPotion,null);
                Capability.IStorage<IPotionInSlash> storage = CapabilityLoader.SlashPotion.getStorage();
                NBTTagCompound compound = (NBTTagCompound) storage.writeNBT(CapabilityLoader.SlashPotion,slash,null);
                if (compound.getInteger(NBT_SLOW_LEVEL) > 0){
                    if (compound.getInteger(NBT_SLOW_TIME) > 0){
                        compound.setInteger(NBT_SLOW_TIME,compound.getInteger(NBT_SLOW_TIME) - 1);
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
                        PACKET_HANDLER.sendToAll(new MessageSpawnParticle(EnumParticleTypes.SLIME,entity.posX+world.rand.nextDouble()*state1,entity.posY+(entity.height/2),entity.posZ+world.rand.nextDouble()*state2,0.0D,0.0D,0.0D));
                        if (compound.getInteger(NBT_SLOW_TIME) > 1){
                            entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(compound.getDouble(NBT_ORIGIN_MOVEMENT)/(1 + compound.getInteger(NBT_SLOW_LEVEL) * 0.2));
                        }
                        if (compound.getInteger(NBT_SLOW_TIME) == 1){
                            entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(compound.getDouble(NBT_ORIGIN_MOVEMENT));
                        }
                    }
                }
            }
            //WineBind
            {
                if (KaBladeCompound.getInteger(KaBladeEntityProperties.PROP_WINE_BIND) > 0){
                    KaBladeCompound.setInteger(KaBladeEntityProperties.PROP_WINE_BIND, KaBladeEntityProperties.getPropCompound(entity).getInteger(KaBladeEntityProperties.PROP_WINE_BIND) - 1);
                    KaBladeEntityProperties.updateNBTForClient(entity);
                    Entity attacker = world.getEntityByID(KaBladeCompound.getInteger(KaBladeEntityProperties.PROP_WINE_BIND_ATTACKER));
                    if (world.getTotalWorldTime() % 20 == 0 &&  attacker!=null && !attacker.isDead){
                        entity.attackEntityFrom(DamageSource.causeMobDamage((EntityLivingBase) attacker),3);
                        EntitySummonedSwordBasePlus sword = new EntitySummonedSwordBasePlus(world, (EntityLivingBase) attacker,4,entity.posX + 1,entity.posY + entity.getEyeHeight() + 1,entity.posZ,0f,0f) ;
                        EntitySummonedSwordBasePlus sword2 = new EntitySummonedSwordBasePlus(world, (EntityLivingBase) attacker,4,entity.posX - 1,entity.posY + entity.getEyeHeight() + 1,entity.posZ,0f,0f) ;
                        sword.setColor(3388211);
                        sword2.setColor(3388211);
                        world.spawnEntity(sword);
                        world.spawnEntity(sword2);
                    }
                    boolean hasLocked = false;
                    for (Entity e:world.getLoadedEntityList()){
                        if (e instanceof EntityWine){
                            if (e.getDataManager().get(EntityWine.targetID) == entity.getEntityId()){
                                hasLocked = true;
                            }
                        }
                    }
                    if (hasLocked){
                        entity.addPotionEffect(new PotionEffect(MobEffects.SLOWNESS,-1,2));
                    }
                    else {
                        int originX = (int) entity.posX;
                        int originY = (int) entity.posY;
                        int originZ = (int) entity.posZ;
                        ArrayList<BlockPos> poses = Lists.newArrayList();
                        ArrayList<BlockPos> correctPoses = Lists.newArrayList();
                        int dx = originX -1;
                        int dy = originY -1;
                        int dz = originZ -1;
                        int mx = originX +1;
                        int my = originY +1;
                        int mz = originZ +1;
                        boolean shouldBreak = false;
                        for (int i = 0;i < 6;i++){
                            if (shouldBreak){
                                break;
                            }
                            dx -= 1;
                            dy -= 1;
                            dz -= 1;
                            mx += 1;
                            my += 1;
                            mz += 1;
                            for (int j = dx;j < mx;j ++){
                                for (int k = dy;k<my;k++){
                                    if (k <= 0){
                                        continue;
                                    }
                                    for (int l = dz;l < mz;l++){
                                        BlockPos pos1 = new BlockPos(j,k,l);
                                        if (poses.contains(pos1)){
                                            continue;
                                        }
                                        else {
                                            if (world.getBlockState(pos1).getBlock() != Blocks.AIR){
                                                correctPoses.add(pos1);
                                                shouldBreak = true;
                                                poses.add(pos1);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (!correctPoses.isEmpty()){
                            for (int i = 0;i<3;i++){
                                BlockPos pos = correctPoses.get(world.rand.nextInt(correctPoses.size()));
                                EntityWine wine = new EntityWine(world,pos.getX(),pos.getY(),pos.getZ());
                                wine.getDataManager().set(EntityWine.targetID,entity.getEntityId());
                                world.spawnEntity(wine);
                            }
                        }
                    }
                }
            }
        }


    }

    @SubscribeEvent
    public void PlayerUpdateEvent(TickEvent.PlayerTickEvent event){
        EntityPlayer player = event.player;
        World world = event.player.world;

        if(player.world.isRemote){
            if(YesUpdate && !hasSendMessage){
                hasSendMessage = true;
                player.sendStatusMessage(new TextComponentString("§6§l[斩无不断]§b检测到模组有更新。最新版本为：§6"+Main.GetUrlVersion),false);
                TextComponentString t = new TextComponentString("§9https://www.mcmod.cn/class/8128.html");
                t.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,"https://www.mcmod.cn/class/8128.html")).setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("点击前往链接")));
                player.sendStatusMessage(new TextComponentString("§6§l[斩无不断]§b可从MC百科下载或从其界面寻找蓝奏云下载地址：").appendSibling(t),false);
            }
        }
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
        //SlashBladeColorUpdate
        {
            //Aurora
            {
                ItemStack stack = player.getHeldItemMainhand();
                if (stack.getItem() instanceof ItemSlashBlade){
                    if(stack.hasTagCompound()){
                        if (stack.getTagCompound().getBoolean("isAurora")){
                            ItemSlashBlade.SummonedSwordColor.set(stack.getTagCompound(),auroraBladeColor.get(auroraBladeColorIndex));
                        }
                    }
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
                if (world.isRemote){
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
                        entitys.getEntityData().setInteger("dizuitime", 300);
                        entitys.getEntityData().setBoolean("dizui",true);

                    }
                }

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
        //EnchantmentFreezyBlades
        {
            if (!world.isRemote){
                if (event.getSource().getImmediateSource() != null){
                    if (event.getSource().getImmediateSource() instanceof EntityLivingBase){
                        EntityLivingBase attacker = (EntityLivingBase) event.getSource().getImmediateSource();
                        ItemStack stack = attacker.getHeldItemMainhand();
                        int level = EnchantmentHelper.getEnchantmentLevel(EnchantmentInit.ENCHANTMENT_SLOW,stack);
                        if (level > 0){
                            if(event.getEntityLiving()!=null)
                            {
                                if (stack.getItem() instanceof ItemSlashBlade){
                                    if (event.getEntityLiving().hasCapability(CapabilityLoader.SlashPotion,null)){
                                        Capability.IStorage<IPotionInSlash> storage = CapabilityLoader.SlashPotion.getStorage();
                                        IPotionInSlash potions = event.getEntityLiving().getCapability(CapabilityLoader.SlashPotion,null);
                                        if (potions != null){
                                            NBTTagCompound compound = CapabilitySlashPotion.initNBT(potions);
                                            compound.setInteger(NBT_SLOW_LEVEL,level);
                                            compound.setInteger(NBT_SLOW_TIME,50 * level);
                                            storage.readNBT(CapabilityLoader.SlashPotion,potions,null,compound);
                                        }
                                    }
                                }
                                else
                                event.getEntityLiving().addPotionEffect(new PotionEffect(MobEffects.SLOWNESS, 50 * level, level));
                            }
                        }
                    }
                }
            }
        }
        //PotionParaly
        {
            PotionEffect effect = e.getActivePotionEffect(PotionInit.PARALY);
            if (effect != null){
                if (effect.getDuration() >0 && effect.getAmplifier() > 0){
                    event.setAmount(event.getAmount() * (1.2f * (1f + (effect.getAmplifier() * 0.1f))));
                }
            }
        }
    }

    @SubscribeEvent
    public void onAttachCapabilitiesEntity(AttachCapabilitiesEvent<Entity> event)
    {
        Entity entity = event.getObject();
        if (entity instanceof EntityLivingBase){
            ICapabilitySerializable<NBTTagCompound> provider = new CapabilitySlashPotion.ProviderEntity();
            event.addCapability(new ResourceLocation(Reference.MODID + ":" + "slash_potion"), provider);
        }
    }

    @SubscribeEvent
    public void onEntityJoin(EntityJoinWorldEvent event){
         if (event.getEntity() instanceof EntityLivingBase){
             EntityLivingBase livingBase = (EntityLivingBase) event.getEntity();
             if (livingBase.hasCapability(CapabilityLoader.SlashPotion,null)){
                 IPotionInSlash potion = livingBase.getCapability(CapabilityLoader.SlashPotion,null);
                 Capability.IStorage<IPotionInSlash> storage = CapabilityLoader.SlashPotion.getStorage();
                 NBTTagCompound compound = (NBTTagCompound) storage.writeNBT(CapabilityLoader.SlashPotion,livingBase.getCapability(CapabilityLoader.SlashPotion,null),null).copy();
                 compound.setDouble(NBT_ORIGIN_MOVEMENT, livingBase.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
                 storage.readNBT(CapabilityLoader.SlashPotion,livingBase.getCapability(CapabilityLoader.SlashPotion,null),null,compound);
             }
         }
    }



}
