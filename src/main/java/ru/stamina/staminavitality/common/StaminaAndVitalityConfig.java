package ru.stamina.staminavitality.common;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class StaminaAndVitalityConfig {
    public static final ModConfigSpec SPEC;
    public static final StaminaAndVitalityConfig INSTANCE;

    public final ModConfigSpec.DoubleValue maxStamina;
    public final ModConfigSpec.DoubleValue sprintCostPerTick;
    public final ModConfigSpec.DoubleValue attackCost;
    public final ModConfigSpec.DoubleValue jumpCost;
    public final ModConfigSpec.DoubleValue swimCostPerTick;
    public final ModConfigSpec.DoubleValue recoveryPerTick;
    public final ModConfigSpec.IntValue recoveryDelayTicks;
    public final ModConfigSpec.IntValue exhaustedCooldownTicks;
    public final ModConfigSpec.DoubleValue foodHealPerNutrition;
    public final ModConfigSpec.DoubleValue blockBreakCost;
    public final ModConfigSpec.DoubleValue blockPlaceCost;

    public final ModConfigSpec.BooleanValue hideInCreative;

    public final ModConfigSpec.IntValue healthWidth;
    public final ModConfigSpec.IntValue healthHeight;
    public final ModConfigSpec.IntValue healthX;
    public final ModConfigSpec.IntValue healthY;
    public final ModConfigSpec.IntValue healthFrameX;
    public final ModConfigSpec.IntValue healthFrameY;
    public final ModConfigSpec.IntValue healthIconX;
    public final ModConfigSpec.IntValue healthIconY;

    public final ModConfigSpec.IntValue staminaWidth;
    public final ModConfigSpec.IntValue staminaHeight;
    public final ModConfigSpec.IntValue staminaX;
    public final ModConfigSpec.IntValue staminaY;
    public final ModConfigSpec.IntValue staminaFrameX;
    public final ModConfigSpec.IntValue staminaFrameY;
    public final ModConfigSpec.IntValue staminaIconX;
    public final ModConfigSpec.IntValue staminaIconY;

    public final ModConfigSpec.IntValue airWidth;
    public final ModConfigSpec.IntValue airHeight;
    public final ModConfigSpec.IntValue airX;
    public final ModConfigSpec.IntValue airY;

    static {
        var pair = new ModConfigSpec.Builder().configure(StaminaAndVitalityConfig::new);
        INSTANCE = pair.getLeft();
        SPEC = pair.getRight();
    }

    private StaminaAndVitalityConfig(ModConfigSpec.Builder b) {
        b.push("balance");
        maxStamina = b.comment("Maximum stamina.").defineInRange("max_stamina", 150.0, 1.0, 1000.0);
        sprintCostPerTick = b.comment("Stamina spent per tick while sprinting.").defineInRange("sprint_cost_per_tick", 0.55, 0.0, 100.0);
        attackCost = b.comment("Stamina spent for a melee attack attempt.").defineInRange("attack_cost", 10.0, 0.0, 100.0);
        jumpCost = b.comment("Extra stamina spent when jumping.").defineInRange("jump_cost", 14.0, 0.0, 100.0);
        swimCostPerTick = b.comment("Stamina spent per tick while fast swimming.").defineInRange("swim_cost_per_tick", 0.35, 0.0, 100.0);
        recoveryPerTick = b.comment("Stamina restored per tick after the recovery delay.").defineInRange("recovery_per_tick", 2.0, 0.0, 100.0);
        recoveryDelayTicks = b.comment("Ticks after the last stamina use before recovery starts.").defineInRange("recovery_delay_ticks", 50, 0, 600);
        exhaustedCooldownTicks = b.comment("Additional ticks after reaching zero before recovery can start.").defineInRange("exhausted_cooldown_ticks", 60, 0, 1200);
        foodHealPerNutrition = b.comment("Health restored per point of food nutrition. A food with more nutrition heals more.").defineInRange("food_heal_per_nutrition", 0.5, 0.0, 10.0);
        blockBreakCost = b.comment("Stamina spent when a player successfully attempts to break a block. Creative and spectator are exempt.").defineInRange("block_break_cost", 2.0, 0.0, 100.0);
        blockPlaceCost = b.comment("Stamina spent when a player places a block. Creative and spectator are exempt.").defineInRange("block_place_cost", 2.0, 0.0, 100.0);
        b.pop();

        b.push("client");
        hideInCreative = b.comment("Hide all custom status bars in Creative and Spectator modes.").define("hide_in_creative", true);

        b.push("health_bar");
        healthWidth = b.comment("Health fill width in pixels.").defineInRange("width", 50, 1, 2000);
        healthHeight = b.comment("Health fill height in pixels.").defineInRange("height", 2, 1, 512);
        healthX = b.comment("Health bar X offset from screen center.").defineInRange("x", 0, -100000, 100000);
        healthY = b.comment("Health bar Y offset from the HUD anchor.").defineInRange("y", 0, -100000, 100000);
        healthFrameX = b.comment("Health frame X offset relative to the health bar center.").defineInRange("frame_x", 0, -100000, 100000);
        healthFrameY = b.comment("Health frame Y offset relative to the health bar.").defineInRange("frame_y", -2, -100000, 100000);
        healthIconX = b.comment("Heart icon X offset relative to the health bar center.").defineInRange("icon_x", 0, -100000, 100000);
        healthIconY = b.comment("Heart icon Y offset relative to the health bar.").defineInRange("icon_y", -4, -100000, 100000);
        b.pop();

        b.push("stamina_bar");
        staminaWidth = b.comment("Stamina fill width in pixels.").defineInRange("width", 74, 1, 2000);
        staminaHeight = b.comment("Stamina fill height in pixels.").defineInRange("height", 2, 1, 512);
        staminaX = b.comment("Stamina bar X offset from screen center.").defineInRange("x", 0, -100000, 100000);
        staminaY = b.comment("Stamina bar Y offset from the HUD anchor.").defineInRange("y", -13, -100000, 100000);
        staminaFrameX = b.comment("Stamina frame X offset relative to the stamina bar center.").defineInRange("frame_x", 0, -100000, 100000);
        staminaFrameY = b.comment("Stamina frame Y offset relative to the stamina bar.").defineInRange("frame_y", -2, -100000, 100000);
        staminaIconX = b.comment("Energy icon X offset relative to the stamina bar center.").defineInRange("icon_x", 0, -100000, 100000);
        staminaIconY = b.comment("Energy icon Y offset relative to the stamina bar.").defineInRange("icon_y", -4, -100000, 100000);
        b.pop();

        b.push("air_bar");
        airWidth = b.comment("Air fill width in pixels.").defineInRange("width", 96, 1, 2000);
        airHeight = b.comment("Air fill height in pixels.").defineInRange("height", 3, 1, 512);
        airX = b.comment("Air bar X offset from screen center.").defineInRange("x", 0, -100000, 100000);
        airY = b.comment("Air bar Y offset from the HUD anchor.").defineInRange("y", -27, -100000, 100000);
        b.pop();
        b.pop();
    }
}
