package org.lordalex.hlmovement.ui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.lordalex.hlmovement.HLMovement;
import org.lordalex.hlmovement.PlayerMoveState;
import org.lordalex.hlmovement.PlayerStateManager;
import org.lordalex.hlmovement.Settings;

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
            p.sendMessage(ChatColor.RED + "Сайдбар скорости выключен.");
        } else {
            enable(p);
            p.sendMessage(ChatColor.GREEN + "Сайдбар скорости включён.");
        }
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::update, 2L, 2L);
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
                obj.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "HL Movement");
                p.setScoreboard(sb);
            }

            // Очищаем старые строки
            for (String entry : sb.getEntries()) {
                sb.resetScores(entry);
            }

            obj.getScore(ChatColor.YELLOW + "Скорость:").setScore(2);
            obj.getScore(ChatColor.AQUA + String.format("%.1f", bps) + " бл/с").setScore(1);
            obj.getScore(ChatColor.DARK_GRAY + " ").setScore(0);
        }
    }
}