package org.lordalex.hlmovement;

import org.bukkit.configuration.file.FileConfiguration;

public class MovementConfig {

    public final double maxspeed;
    public final double accelerate;
    public final double airaccelerate;
    public final double friction;
    public final double stopspeed;
    public final double airWishspeedCap;
    public final boolean autoBhop;
    public final boolean scoreboardDefault;
    public final int scoreboardInterval;
    public final boolean enabled;

    public MovementConfig(FileConfiguration cfg) {
        this.maxspeed = cfg.getDouble("maxspeed", 0.56);
        this.accelerate = cfg.getDouble("accelerate", 5.0);
        this.airaccelerate = cfg.getDouble("airaccelerate", 10.0);
        this.friction = cfg.getDouble("friction", 4.0);
        this.stopspeed = cfg.getDouble("stopspeed", 0.28);
        this.airWishspeedCap = cfg.getDouble("air_wishspeed_cap", 0.28);
        this.autoBhop = cfg.getBoolean("auto_bhop", true);
        this.scoreboardDefault = cfg.getBoolean("scoreboard_default", true);
        this.scoreboardInterval = Math.max(1, cfg.getInt("scoreboard_interval", 2));
        this.enabled = cfg.getBoolean("enabled", true);
    }
}
