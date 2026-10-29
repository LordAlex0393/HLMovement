package org.lordalex.hlmovement;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStateManager {

    private final HLMovement plugin;
    private final MovementConfig config;
    private final Map<UUID, PlayerMoveState> states = new ConcurrentHashMap<>();
    private BukkitTask task;

    public PlayerStateManager(HLMovement plugin, MovementConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public PlayerMoveState get(Player p) {
        return states.computeIfAbsent(p.getUniqueId(), k -> new PlayerMoveState());
    }

    public void remove(Player p) {
        states.remove(p.getUniqueId());
    }

    public void start() {
        // Запускаем пустой таск (ничего не делает)
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // ТУТ ПУСТО. Никаких velocity, никаких телепортов.
        }, 1L, 1L);
    }

    public void stop() {
        if (task != null) task.cancel();
        states.clear();
    }
}