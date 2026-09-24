package ru.stamina.staminavitality.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.common.util.TriState;
import ru.stamina.staminavitality.StaminaAndVitality;
import ru.stamina.staminavitality.common.StaminaData;

@EventBusSubscriber(modid = StaminaAndVitality.MOD_ID, value = Dist.CLIENT)
public final class StaminaClientEvents {
    private StaminaClientEvents() {}

    @SubscribeEvent
    public static void onLockedRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) return;
        if (!StaminaData.isLocked(player)) return;

        
        
        
        if (event.getItemStack().getItem() instanceof BlockItem) {
            event.setUseItem(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void onLockedLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) return;
        if (!StaminaData.isLocked(player)) return;

        
        
        
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMovementInput(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) return;
        if (!StaminaData.isLocked(player)) return;

        Input input = event.getInput();
        input.jumping = false;

        
        
        
        Minecraft.getInstance().options.keySprint.setDown(false);
        player.sprintTriggerTime = 0;
        player.setSprinting(false);
        player.setJumping(false);
    }
}
