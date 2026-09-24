package ru.stamina.staminavitality.common;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentSyncHandler;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

import static ru.stamina.staminavitality.StaminaAndVitality.MOD_ID;

public final class StaminaData {
    private StaminaData() {}

    public static final String LAST_USE_TICK = MOD_ID + ":last_stamina_use";
    public static final String EXHAUSTED_UNTIL = MOD_ID + ":exhausted_until";
    public static final String HEAL_ALLOWED_UNTIL = MOD_ID + ":heal_allowed_until";
    public static final String POTION_REGEN_ALLOWED = MOD_ID + ":potion_regen_allowed";

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MOD_ID);

    public static final Supplier<AttachmentType<Float>> STAMINA = ATTACHMENTS.register(
            "stamina", () -> AttachmentType.<Float>builder(StaminaData::maxDefault)
                    .serialize(Codec.FLOAT)
                    .sync(new AttachmentSyncHandler<Float>() {
                        @Override
                        public void write(RegistryFriendlyByteBuf buf, Float value, boolean initialSync) {
                            buf.writeFloat(value);
                        }

                        @Override
                        public @Nullable Float read(IAttachmentHolder holder, RegistryFriendlyByteBuf buf, Float previousValue) {
                            return buf.readFloat();
                        }
                    })
                    .copyOnDeath()
                    .build()
    );

    
    public static final Supplier<AttachmentType<Boolean>> EXHAUSTED = ATTACHMENTS.register(
            "exhausted", () -> AttachmentType.<Boolean>builder(() -> false)
                    .serialize(Codec.BOOL)
                    .sync(new AttachmentSyncHandler<Boolean>() {
                        @Override
                        public void write(RegistryFriendlyByteBuf buf, Boolean value, boolean initialSync) {
                            buf.writeBoolean(value);
                        }

                        @Override
                        public @Nullable Boolean read(IAttachmentHolder holder, RegistryFriendlyByteBuf buf, Boolean previousValue) {
                            return buf.readBoolean();
                        }
                    })
                    .build()
    );

    private static float maxDefault() {
        return 100.0f;
    }

    public static float max() {
        return StaminaAndVitalityConfig.INSTANCE.maxStamina.get().floatValue();
    }

    public static float get(Player player) {
        return Math.min(player.getData(STAMINA), max());
    }

    public static void set(Player player, float value) {
        float clamped = Math.max(0.0f, Math.min(max(), value));
        if (Math.abs(player.getData(STAMINA) - clamped) > 0.001f) {
            player.setData(STAMINA, clamped);
            player.syncData(STAMINA.get());
        }
    }

    
    public static boolean tryConsume(Player player, float amount) {
        if (amount <= 0) return true;
        if (isLocked(player)) return false;

        float current = get(player);
        if (current + 0.0001f < amount) {
            exhaust(player);
            return false;
        }

        float next = current - amount;
        set(player, next);
        player.getPersistentData().putLong(LAST_USE_TICK, player.level().getGameTime());
        if (next <= 0.001f) exhaust(player);
        return true;
    }

    public static void exhaust(Player player) {
        set(player, 0.0f);
        setExhausted(player, true);
        player.getPersistentData().putLong(EXHAUSTED_UNTIL,
                player.level().getGameTime() + StaminaAndVitalityConfig.INSTANCE.exhaustedCooldownTicks.get());
    }

    public static boolean isLocked(Player player) {
        return player.getData(EXHAUSTED);
    }

    public static void setExhausted(Player player, boolean exhausted) {
        if (player.getData(EXHAUSTED) != exhausted) {
            player.setData(EXHAUSTED, exhausted);
            player.syncData(EXHAUSTED.get());
        }
    }

    public static boolean recoveryDelayPassed(Player player) {
        long now = player.level().getGameTime();
        long lastUse = player.getPersistentData().getLong(LAST_USE_TICK);
        long exhaustionCooldown = player.getPersistentData().getLong(EXHAUSTED_UNTIL);
        return now - lastUse >= StaminaAndVitalityConfig.INSTANCE.recoveryDelayTicks.get()
                && now >= exhaustionCooldown;
    }

    public static void allowHealing(Player player, long ticks) {
        player.getPersistentData().putLong(HEAL_ALLOWED_UNTIL,
                Math.max(player.getPersistentData().getLong(HEAL_ALLOWED_UNTIL),
                        player.level().getGameTime() + ticks));
    }

    public static boolean healingAllowed(Player player) {
        return player.level().getGameTime() <= player.getPersistentData().getLong(HEAL_ALLOWED_UNTIL);
    }

    public static void setPotionRegenAllowed(Player player, boolean allowed) {
        player.getPersistentData().putBoolean(POTION_REGEN_ALLOWED, allowed);
    }

    public static boolean potionRegenAllowed(Player player) {
        return player.getPersistentData().getBoolean(POTION_REGEN_ALLOWED);
    }
}
