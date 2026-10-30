package org.lordalex.hlmovement;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Settings {

    @JsonProperty("enabled")
    private boolean enabled = true; // Включён ли плагин вообще

    @JsonProperty("auto_bhop")
    private boolean autoBhop = true; // Автопрыжок (HL1)

    @JsonProperty("scoreboard_default")
    private boolean scoreboardDefault = true; // Показывать сайдбар сразу

    @JsonProperty("accel_per_tick")
    private double accelPerTick = 0.15; // Сила разгона в воздухе

    @JsonProperty("forward_multiplier")
    private double forwardMultiplier = 0.7; // Влияние взгляда (вперёд)

    @JsonProperty("strafe_multiplier")
    private double strafeMultiplier = 0.3; // Влияние диагонали (вбок)

    @JsonProperty("max_cap")
    private double maxCap = 10.0; // Максимальный потолок (защита от краша)

    // --- Геттеры (чтобы код мог читать настройки) ---
    public boolean isEnabled() { return enabled; }
    public boolean isAutoBhop() { return autoBhop; }
    public boolean isScoreboardDefault() { return scoreboardDefault; }
    public double getAccelPerTick() { return accelPerTick; }
    public double getForwardMultiplier() { return forwardMultiplier; }
    public double getStrafeMultiplier() { return strafeMultiplier; }
    public double getMaxCap() { return maxCap; }
}