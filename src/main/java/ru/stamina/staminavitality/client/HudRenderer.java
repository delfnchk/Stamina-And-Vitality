package ru.stamina.staminavitality.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import com.mojang.blaze3d.systems.RenderSystem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import ru.stamina.staminavitality.StaminaAndVitality;
import ru.stamina.staminavitality.common.StaminaAndVitalityConfig;
import ru.stamina.staminavitality.common.StaminaData;

@EventBusSubscriber(modid = StaminaAndVitality.MOD_ID, value = Dist.CLIENT)
public final class HudRenderer {
    private static final ResourceLocation HEALTH_FRAME = texture("health_frame");
    private static final ResourceLocation STAMINA_FRAME = texture("stamina_frame");
    private static final ResourceLocation BAR_FILL = texture("bar_fill");
    private static final ResourceLocation BAR_GHOST = texture("bar_ghost");
    private static final ResourceLocation HEALTH_FILL = texture("health_fill");
    private static final ResourceLocation HEALTH_GHOST = texture("health_ghost");
    private static final ResourceLocation AIR_FILL = texture("air_fill");
    private static final ResourceLocation AIR_GHOST = texture("air_ghost");
    private static final ResourceLocation ENERGY_ICON = texture("energy");
    private static final ResourceLocation ENERGY_ICON_WHITE = texture("energy_white");
    private static final ResourceLocation ENERGY_ICON_OUTLINE = texture("energy_outline");
    private static final ResourceLocation HEART_NORMAL = texture("heart_normal");
    private static final ResourceLocation HEART_ABSORBING = texture("heart_absorbing");
    private static final ResourceLocation HEART_WITHERED = texture("heart_withered");
    private static final ResourceLocation HEART_POISONED = texture("heart_poisoned");
    private static final ResourceLocation HEART_FROZEN = texture("heart_frozen");
    private static final ResourceLocation HEART_HARDCORE = texture("heart_hardcore");

    private static final int ENERGY_ICON_SIZE = 10;
    private static final int FILL_TEXTURE_WIDTH = 8;
    private static final int FILL_TEXTURE_HEIGHT = 2;
    private static final int FRAME_TEXTURE_WIDTH = 80;
    private static final int FRAME_TEXTURE_HEIGHT = 6;
    private static final float STAMINA_FADE_SECONDS = 1.5f;
    private static final float STAMINA_WHITE_FADE_SECONDS = 0.35f;
    private static final int SELECTED_ITEM_NAME_Y_OFFSET = -36;

    private static float staminaVisual = 100;
    private static float staminaGhost = 100;
    private static float healthVisual = 20;
    private static float healthGhost = 20;
    private static float airVisual = 300;
    private static float airGhost = 300;

    private static float lastStaminaTarget = 100;
    private static float lastHealthTarget = 20;

    private static float staminaAlpha = 0.0f;
    private static float staminaWhiteAlpha = 0.0f;
    private static long staminaFadeOutStartNanos = -1L;
    private static long staminaWhiteTransitionStartNanos = -1L;
    private static long lastStaminaDecreaseNanos = -1L;
    private static boolean staminaWhiteTransitionToWhite;
    private static boolean staminaSpendingActive;
    private static final long STAMINA_SPENDING_GRACE_NANOS = 500_000_000L;
    private static int healthShakeTicks;
    private static int staminaShakeTicks;
    private static boolean airLatched;

    private HudRenderer() {}

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(StaminaAndVitality.MOD_ID, "textures/gui/" + name + ".png");
    }

    @SubscribeEvent
    public static void hideVanillaHud(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(VanillaGuiLayers.FOOD_LEVEL)
                || event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)
                || event.getName().equals(VanillaGuiLayers.ARMOR_LEVEL)
                || event.getName().equals(VanillaGuiLayers.AIR_LEVEL)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void shiftSelectedItemName(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.SELECTED_ITEM_NAME)) return;
        event.getGuiGraphics().pose().pushPose();
        event.getGuiGraphics().pose().translate(0.0, SELECTED_ITEM_NAME_Y_OFFSET, 0.0);
    }

    @SubscribeEvent
    public static void restoreSelectedItemName(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.SELECTED_ITEM_NAME)) return;
        event.getGuiGraphics().pose().popPose();
    }

    @SubscribeEvent
    public static void renderCustomHud(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui || mc.gameMode == null) return;
        if (StaminaAndVitalityConfig.INSTANCE.hideInCreative.get()
                && (player.getAbilities().instabuild || player.isSpectator())) return;

        GuiGraphics g = event.getGuiGraphics();
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        render(g, player, pt);
    }

    private static void render(GuiGraphics g, Player player, float pt) {
        StaminaAndVitalityConfig cfg = StaminaAndVitalityConfig.INSTANCE;

        float maxStamina = StaminaData.max();
        float targetStamina = Mth.clamp(StaminaData.get(player), 0, maxStamina);
        float maxHealth = Math.max(1f, player.getMaxHealth());
        float targetHealth = Mth.clamp(player.getHealth(), 0, maxHealth);
        float maxAir = Math.max(1f, player.getMaxAirSupply());
        float targetAir = Mth.clamp(player.getAirSupply(), 0, maxAir);

        
        if (lastHealthTarget < maxHealth - 0.01f && targetHealth >= maxHealth - 0.01f) {
            healthShakeTicks = 20;
        } else if (targetHealth > lastHealthTarget + 0.01f) {
            
            healthShakeTicks = 20;
        }
        if (lastStaminaTarget < maxStamina - 0.01f && targetStamina >= maxStamina - 0.01f) {
            staminaShakeTicks = 20;
        }

        if (targetAir < maxAir - 0.01f || player.isUnderWater()) {
            airLatched = true;
        }

        staminaVisual = smooth(staminaVisual, targetStamina, .22f);
        staminaGhost = smooth(staminaGhost, Math.max(targetStamina, staminaVisual), .07f);
        healthVisual = smooth(healthVisual, targetHealth, .25f);
        healthGhost = smooth(healthGhost, Math.max(targetHealth, healthVisual), .09f);
        airVisual = smooth(airVisual, targetAir, .25f);
        airGhost = smooth(airGhost, Math.max(targetAir, airVisual), .09f);

        
        
        
        long nowNanos = System.nanoTime();
        boolean staminaFullyRecovered = targetStamina >= maxStamina - 0.01f
                && staminaVisual >= maxStamina - 0.05f;
        if (!staminaFullyRecovered) {
            staminaFadeOutStartNanos = -1L;
            staminaAlpha = 1.0f;
        } else {
            if (staminaFadeOutStartNanos < 0L) {
                staminaFadeOutStartNanos = nowNanos;
            }
            float progress = Mth.clamp((nowNanos - staminaFadeOutStartNanos)
                    / (STAMINA_FADE_SECONDS * 1_000_000_000.0f), 0.0f, 1.0f);
            staminaAlpha = 1.0f - easeOutCubic(progress);
        }

        
        
        
        
        
        
        if (targetStamina < lastStaminaTarget - 0.0001f) {
            lastStaminaDecreaseNanos = nowNanos;
        }
        boolean staminaIsBeingSpent = lastStaminaDecreaseNanos >= 0L
                && nowNanos - lastStaminaDecreaseNanos <= STAMINA_SPENDING_GRACE_NANOS;
        if (staminaIsBeingSpent != staminaSpendingActive) {
            staminaSpendingActive = staminaIsBeingSpent;
            staminaWhiteTransitionToWhite = staminaIsBeingSpent;
            staminaWhiteTransitionStartNanos = nowNanos;
        }
        if (staminaWhiteTransitionStartNanos < 0L) {
            staminaWhiteTransitionStartNanos = nowNanos;
        }
        float whiteProgress = Mth.clamp((nowNanos - staminaWhiteTransitionStartNanos)
                / (STAMINA_WHITE_FADE_SECONDS * 1_000_000_000.0f), 0.0f, 1.0f);
        float whiteEased = easeInOutCubic(whiteProgress);
        staminaWhiteAlpha = staminaWhiteTransitionToWhite ? whiteEased : 1.0f - whiteEased;
        if (healthShakeTicks > 0) healthShakeTicks--;
        if (staminaShakeTicks > 0) staminaShakeTicks--;

        if (airLatched && airVisual >= maxAir - 0.05f && targetAir >= maxAir - 0.01f && !player.isUnderWater()) {
            airLatched = false;
        }

        lastStaminaTarget = targetStamina;
        lastHealthTarget = targetHealth;

        int baseY = g.guiHeight() - 43;

        int healthCenterX = g.guiWidth() / 2 + cfg.healthX.get();
        int healthY = baseY + cfg.healthY.get();
        int staminaCenterX = g.guiWidth() / 2 + cfg.staminaX.get();
        int staminaY = baseY + cfg.staminaY.get();
        int airCenterX = g.guiWidth() / 2 + cfg.airX.get();
        int airY = baseY + cfg.airY.get();

        
        drawFrame(g, HEALTH_FRAME,
                healthCenterX + cfg.healthFrameX.get(), healthY + cfg.healthFrameY.get(), 1.0f);
        drawBar(g, healthCenterX, healthY, cfg.healthWidth.get(), cfg.healthHeight.get(),
                healthVisual / maxHealth, healthGhost / maxHealth,
                HEALTH_FILL, HEALTH_GHOST, 1.0f);
        drawHeartIcon(g, player,
                healthCenterX + cfg.healthIconX.get() + Math.round(shake(healthShakeTicks, 1.1f)),
                healthY + cfg.healthIconY.get() + Math.round(shakeY(healthShakeTicks, 0.8f)),
                1.0f);

        
        if (staminaAlpha > 0.001f) {
            float alpha = Mth.clamp(staminaAlpha, 0.0f, 1.0f);
            
            drawFrame(g, STAMINA_FRAME,
                    staminaCenterX + cfg.staminaFrameX.get(), staminaY + cfg.staminaFrameY.get(), alpha);
            drawBar(g, staminaCenterX, staminaY, cfg.staminaWidth.get(), cfg.staminaHeight.get(),
                    staminaVisual / maxStamina, staminaGhost / maxStamina,
                    BAR_FILL, BAR_GHOST, alpha);
            int energyX = staminaCenterX + cfg.staminaIconX.get() + Math.round(shake(staminaShakeTicks, 1.2f));
            int energyY = staminaY + cfg.staminaIconY.get() + Math.round(shakeY(staminaShakeTicks, 0.9f));
            
            
            drawIcon(g, ENERGY_ICON_OUTLINE, energyX, energyY, ENERGY_ICON_SIZE, ENERGY_ICON_SIZE, alpha);
            drawIcon(g, ENERGY_ICON, energyX, energyY, ENERGY_ICON_SIZE, ENERGY_ICON_SIZE,
                    alpha * (1.0f - staminaWhiteAlpha));
            drawIcon(g, ENERGY_ICON_WHITE, energyX, energyY, ENERGY_ICON_SIZE, ENERGY_ICON_SIZE,
                    alpha * staminaWhiteAlpha);
        }

        
        if (airLatched) {
            drawBar(g, airCenterX, airY, cfg.airWidth.get(), cfg.airHeight.get(),
                    airVisual / maxAir, airGhost / maxAir,
                    AIR_FILL, AIR_GHOST, 1.0f);
        }
    }

    private static float smooth(float a, float b, float s) {
        return a + (b - a) * s;
    }

    private static float easeOutCubic(float t) {
        float inv = 1.0f - Mth.clamp(t, 0.0f, 1.0f);
        return 1.0f - inv * inv * inv;
    }

    private static float easeInOutCubic(float t) {
        t = Mth.clamp(t, 0.0f, 1.0f);
        return t < 0.5f
                ? 4.0f * t * t * t
                : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3.0f) / 2.0f;
    }

    private static float shake(int ticks, float amplitude) {
        if (ticks <= 0) return 0;
        float progress = (20 - ticks) / 20.0f;
        return (float) Math.sin(progress * Math.PI * 6.0) * amplitude * (1.0f - progress);
    }

    private static float shakeY(int ticks, float amplitude) {
        if (ticks <= 0) return 0;
        float progress = (20 - ticks) / 20.0f;
        return (float) Math.cos(progress * Math.PI * 5.0) * amplitude * (1.0f - progress);
    }

    private static void drawBar(GuiGraphics g, int center, int y, int width, int height,
                                float value, float ghost, ResourceLocation fillTexture,
                                ResourceLocation ghostTexture, float alpha) {
        drawCenteredFill(g, ghostTexture, center, y, width, height, ghost, alpha * 0.85f);
        drawCenteredFill(g, fillTexture, center, y, width, height, value, alpha);
    }

    private static void drawCenteredFill(GuiGraphics g, ResourceLocation tex, int center, int y,
                                         int width, int height, float value, float alpha) {
        int fullLeft = center - width / 2;
        int fill = Mth.clamp(Math.round(width * Mth.clamp(value, 0, 1)), 0, width);
        if (fill <= 0 || alpha <= 0.001f) return;

        
        
        int fillLeft = center - fill / 2;
        int remaining = fill;
        int dx = fillLeft;
        int sourceU = Math.floorMod(fillLeft - fullLeft, FILL_TEXTURE_WIDTH);

        while (remaining > 0) {
            int part = Math.min(remaining, FILL_TEXTURE_WIDTH - sourceU);
            blitTinted(g, tex, dx, y, part, height, sourceU, 0, part, FILL_TEXTURE_HEIGHT,
                    FILL_TEXTURE_WIDTH, FILL_TEXTURE_HEIGHT, alpha);
            dx += part;
            remaining -= part;
            sourceU = 0;
        }
    }

    
    private static void drawFrame(GuiGraphics g, ResourceLocation frameTexture, int center, int y, float alpha) {
        if (alpha <= 0.001f) return;
        int x = center - FRAME_TEXTURE_WIDTH / 2;
        blitTinted(g, frameTexture, x, y, FRAME_TEXTURE_WIDTH, FRAME_TEXTURE_HEIGHT,
                0, 0, FRAME_TEXTURE_WIDTH, FRAME_TEXTURE_HEIGHT,
                FRAME_TEXTURE_WIDTH, FRAME_TEXTURE_HEIGHT, alpha);
    }

    private static void drawHeartIcon(GuiGraphics g, Player player, int centerX, int y, float alpha) {
        if (alpha <= 0.001f) return;

        ResourceLocation sprite;
        if (player.getAbsorptionAmount() > 0.0f) {
            sprite = HEART_ABSORBING;
        } else if (player.hasEffect(net.minecraft.world.effect.MobEffects.WITHER)) {
            sprite = HEART_WITHERED;
        } else if (player.hasEffect(net.minecraft.world.effect.MobEffects.POISON)) {
            sprite = HEART_POISONED;
        } else if (player.isFullyFrozen()) {
            sprite = HEART_FROZEN;
        } else if (player.level().getLevelData().isHardcore()) {
            sprite = HEART_HARDCORE;
        } else {
            sprite = HEART_NORMAL;
        }

        drawIcon(g, sprite, centerX, y, 10, 10, alpha);
    }

    private static void drawIcon(GuiGraphics g, ResourceLocation tex, int centerX, int y,
                                 int width, int height, float alpha) {
        if (alpha <= 0.001f) return;
        blitTinted(g, tex, centerX - width / 2, y, width, height,
                0, 0, width, height, width, height, alpha);
    }

    private static void blitTinted(GuiGraphics g, ResourceLocation tex, int x, int y,
                                   int width, int height, int u, int v, int uWidth, int vHeight,
                                   int texWidth, int texHeight, float alpha) {
        float a = Mth.clamp(alpha, 0.0f, 1.0f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1.0f, 1.0f, 1.0f, a);
        g.blit(tex, x, y, width, height, u, v, uWidth, vHeight, texWidth, texHeight);
        g.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }
}
