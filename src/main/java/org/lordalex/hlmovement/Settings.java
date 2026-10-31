package org.lordalex.hlmovement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Settings {

    @JsonProperty("enabled")
    private boolean enabled = true;

    @JsonProperty("scoreboard_default")
    private boolean scoreboardDefault = true;

    @JsonProperty("accel_per_tick")
    private double accelPerTick = 0.15;

    @JsonProperty("forward_multiplier")
    private double forwardMultiplier = 0.6;

    @JsonProperty("strafe_multiplier")
    private double strafeMultiplier = 0.4;

    // Настройки сайдбара
    @JsonProperty("scoreboard_update_ticks")
    private int scoreboardUpdateTicks = 4;

    @JsonProperty("speed_max_for_bar")
    private double speedMaxForBar = 100.0;

    @JsonProperty("speed_bars_count")
    private int speedBarsCount = 15;

    public boolean isEnabled() { return enabled; }
    public boolean isScoreboardDefault() { return scoreboardDefault; }
    public double getAccelPerTick() { return accelPerTick; }
    public double getForwardMultiplier() { return forwardMultiplier; }
    public double getStrafeMultiplier() { return strafeMultiplier; }
    public int getScoreboardUpdateTicks() { return scoreboardUpdateTicks; }
    public double getSpeedMaxForBar() { return speedMaxForBar; }
    public int getSpeedBarsCount() { return speedBarsCount; }
}