package ru.stamina.staminavitality;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.fml.config.ModConfig;
import ru.stamina.staminavitality.common.StaminaAndVitalityConfig;
import ru.stamina.staminavitality.common.StaminaData;

@Mod(StaminaAndVitality.MOD_ID)
public final class StaminaAndVitality {
    public static final String MOD_ID = "staminavitality";

    public StaminaAndVitality(IEventBus modBus, ModContainer container) {
        StaminaData.ATTACHMENTS.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, StaminaAndVitalityConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mc, parent) -> new ConfigurationScreen(container, parent));
    }
}
