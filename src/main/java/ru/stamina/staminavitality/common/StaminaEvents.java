package ru.stamina.staminavitality.common;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import ru.stamina.staminavitality.StaminaAndVitality;

@EventBusSubscriber(modid = StaminaAndVitality.MOD_ID)
public final class StaminaEvents {
    private StaminaEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        var cfg = StaminaAndVitalityConfig.INSTANCE;
        boolean creative = player.getAbilities().instabuild || player.isSpectator();

        if (creative) {
            StaminaData.set(player, StaminaData.max());
            StaminaData.setExhausted(player, false);
        } else {
            boolean locked = StaminaData.isLocked(player);
            boolean sprinting = player.isSprinting();
            boolean fastSwimming = player.isSwimming() && player.isSprinting();

            
            if (locked) {
                player.setSprinting(false);
                player.setJumping(false);
            } else if (fastSwimming) {
                if (!StaminaData.tryConsume(player, cfg.swimCostPerTick.get().floatValue())) {
                    player.setSprinting(false);
                }
            } else if (sprinting) {
                if (!StaminaData.tryConsume(player, cfg.sprintCostPerTick.get().floatValue())) {
                    player.setSprinting(false);
                }
            }

            if (StaminaData.recoveryDelayPassed(player) && StaminaData.get(player) < StaminaData.max()) {
                StaminaData.set(player, StaminaData.get(player) + cfg.recoveryPerTick.get().floatValue());
            }

            
            if (StaminaData.isLocked(player) && StaminaData.get(player) >= StaminaData.max() - 0.001f) {
                StaminaData.set(player, StaminaData.max());
                StaminaData.setExhausted(player, false);
            }
        }

        if (StaminaData.potionRegenAllowed(player) && !player.hasEffect(MobEffects.REGENERATION)) {
            StaminaData.setPotionRegenAllowed(player, false);
        }

        
        
        player.getFoodData().setFoodLevel(17);
        player.getFoodData().setSaturation(0.0f);
        player.getFoodData().setExhaustion(0.0f);
    }

    @SubscribeEvent
    public static void onLockedRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (!StaminaData.isLocked(player)) return;
        if (event.getItemStack().getItem() instanceof BlockItem) {
            
            
            
            event.setUseItem(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onLockedLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (!StaminaData.isLocked(player)) return;

        
        
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide()) return;
        if (player.getAbilities().instabuild || player.isSpectator()) return;

        float cost = StaminaAndVitalityConfig.INSTANCE.blockBreakCost.get().floatValue();
        if (cost <= 0.0f) return;

        
        
        
        if (StaminaData.isLocked(player) || !StaminaData.tryConsume(player, cost)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (player.getAbilities().instabuild || player.isSpectator()) return;

        float cost = StaminaAndVitalityConfig.INSTANCE.blockPlaceCost.get().floatValue();
        if (cost <= 0.0f) return;

        
        
        if (StaminaData.isLocked(player) || !StaminaData.tryConsume(player, cost)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.isSpectator() || player.getAbilities().instabuild) return;
        if (StaminaData.isLocked(player)
                || !StaminaData.tryConsume(player, StaminaAndVitalityConfig.INSTANCE.attackCost.get().floatValue())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onJump(LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
        if (player.getAbilities().instabuild || player.isSpectator()) return;
        if (StaminaData.isLocked(player)
                || !StaminaData.tryConsume(player, StaminaAndVitalityConfig.INSTANCE.jumpCost.get().floatValue())) {
            var v = player.getDeltaMovement();
            player.setDeltaMovement(v.x, Math.min(v.y, 0.0), v.z);
            player.setJumping(false);
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;

        boolean regenerationEffect = player.hasEffect(MobEffects.REGENERATION) && StaminaData.potionRegenAllowed(player);
        if (!StaminaData.healingAllowed(player) && !regenerationEffect) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
        ItemStack stack = event.getItem();
        FoodProperties food = stack.getFoodProperties(player);
        if (food != null || isPotion(stack)) {
            
            
            StaminaData.allowHealing(player, Math.max(10, event.getDuration() + 10L));
        }
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
        ItemStack stack = event.getItem();
        FoodProperties food = stack.getFoodProperties(player);
        if (food != null) {
            StaminaData.allowHealing(player, 100);
            if (player.getHealth() < player.getMaxHealth()) {
                float healAmount = food.nutrition()
                        * StaminaAndVitalityConfig.INSTANCE.foodHealPerNutrition.get().floatValue();
                player.heal(healAmount);
            }
        }
        if (isPotion(stack)) {
            StaminaData.allowHealing(player, 100);
            StaminaData.setPotionRegenAllowed(player, true);
        }
    }

    private static boolean isPotion(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.POTION)
                || stack.is(net.minecraft.world.item.Items.SPLASH_POTION)
                || stack.is(net.minecraft.world.item.Items.LINGERING_POTION);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            player.getFoodData().setFoodLevel(17);
            player.getFoodData().setSaturation(0.0f);
            player.getFoodData().setExhaustion(0.0f);
            StaminaData.set(player, StaminaData.max());
            StaminaData.setExhausted(player, false);
            player.getPersistentData().putLong(StaminaData.LAST_USE_TICK, player.level().getGameTime());
            player.getPersistentData().putLong(StaminaData.EXHAUSTED_UNTIL, 0);
            player.getPersistentData().putLong(StaminaData.HEAL_ALLOWED_UNTIL, 0);
            StaminaData.setPotionRegenAllowed(player, false);
        }
    }
}
