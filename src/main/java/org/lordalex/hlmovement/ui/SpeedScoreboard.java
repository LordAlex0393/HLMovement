package org.lordalex.hlmovement.ui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.lordalex.hlmovement.HLMovement;
import org.lordalex.hlmovement.PlayerMoveState;
import org.lordalex.hlmovement.PlayerStateManager;
import org.lordalex.hlmovement.Settings;
import org.lordalex.hlmovement.utils.ColorUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SpeedScoreboard {

    private final HLMovement plugin;
    private final PlayerStateManager stateManager;
    private Settings settings;
    private final Set<UUID> enabled = new HashSet<>();
    private BukkitTask task;

    public SpeedScoreboard(HLMovement plugin, PlayerStateManager sm, Settings settings) {
        this.plugin = plugin;
        this.stateManager = sm;
        this.settings = settings;
    }

    public void updateSettings(Settings newSettings) {
        this.settings = newSettings;
    }

    public boolean isEnabled(Player p) {
        return enabled.contains(p.getUniqueId());
    }

    public void enable(Player p) {
        enabled.add(p.getUniqueId());
    }

    public void disable(Player p) {
        enabled.remove(p.getUniqueId());
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        p.setScoreboard(sb);
    }

    public void toggle(Player p) {
        if (enabled.contains(p.getUniqueId())) {
            disable(p);
            p.sendMessage(ColorUtil.get("&cСайдбар скорости выключен."));
        } else {
            enable(p);
            p.sendMessage(ColorUtil.get("&aСайдбар скорости включён."));
        }
    }

    public void start() {
        int ticks = Math.max(1, settings.getScoreboardUpdateTicks());
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::update, ticks, ticks);
    }

    public void stop() {
        if (task != null) task.cancel();
        for (UUID id : enabled) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) disable(p);
        }
        enabled.clear();
    }

    private void update() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!enabled.contains(p.getUniqueId())) continue;

            PlayerMoveState s = stateManager.get(p);
            double bps = s.getSpeedBlocksPerSecond();

            Scoreboard sb = p.getScoreboard();
            Objective obj = sb.getObjective("hl_speed");
            if (obj == null) {
                sb = Bukkit.getScoreboardManager().getNewScoreboard();
                obj = sb.registerNewObjective("hl_speed", "dummy");
                obj.setDisplaySlot(DisplaySlot.SIDEBAR);
                obj.setDisplayName(ColorUtil.get("&6&lHL Movement"));
                p.setScoreboard(sb);
            }

            // Очистка
            for (String entry : sb.getEntries()) {
                sb.resetScores(entry);
            }

            // Формируем шкалу и строку (в одной строке)
            String bar = buildBar(bps, settings.getSpeedMaxForBar());
            String speedStr = String.format("%.1f", bps);

            // Защита от 40 символов
            String line = ColorUtil.get("&b" + speedStr + " &eбл/с " + bar);
            if (line.length() > 40) {
                line = line.substring(0, 40);
            }

            obj.getScore(ColorUtil.get("&eСкорость:")).setScore(2);
            obj.getScore(line).setScore(1);
            obj.getScore(ColorUtil.get("&8 ")).setScore(0);
        }
    }

    private String buildBar(double cur, double max) {
        int bars = settings.getSpeedBarsCount();
        int filled = (int) Math.min(bars, Math.round((cur / max) * bars));

        StringBuilder sb = new StringBuilder("&7[");
        for (int i = 0; i < bars; i++) {
            if (i < filled) {
                sb.append("&a|");
            } else {
                sb.append("&7.");
            }
        }
        return sb.append("&7]").toString();
    }
}