package org.lordalex.hlmovement;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStateManager {

    private final HLMovement plugin;
    private final MovementConfig config;
    private final Map<UUID, PlayerMoveState> states = new ConcurrentHashMap<>();
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private BukkitTask task;

    // Настройки разгона
    private final double HL_AIRACCEL = 100.0; // Сила разгона в воздухе
    private final double HL_WISHSPEED_CAP = 0.30;

    public PlayerStateManager(HLMovement plugin, MovementConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public PlayerMoveState get(Player p) {
        return states.computeIfAbsent(p.getUniqueId(), k -> new PlayerMoveState());
    }

    public void remove(Player p) {
        states.remove(p.getUniqueId());
        lastLocations.remove(p.getUniqueId());
    }

    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    public void stop() {
        if (task != null) task.cancel();
        states.clear();
        lastLocations.clear();
    }

    private void tick() {
        if (!config.enabled) return;

        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerMoveState s = get(p);
            Location currentLoc = p.getLocation();
            Location lastLoc = lastLocations.get(p.getUniqueId());

            // Вычисляем скорость для сайдбара
            if (lastLoc != null) {
                double dx = currentLoc.getX() - lastLoc.getX();
                double dz = currentLoc.getZ() - lastLoc.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);

                if (dist < 2.0) {
                    s.vx = s.vx * 0.8 + dx * 0.2;
                    s.vz = s.vz * 0.8 + dz * 0.2;
                }

                // Если мы в воздухе, и игрок нажимает клавиши (он двигается)
                // то мы даем ему ускорение.
                if (!s.onGround && dist > 0.01) {

                    // Направление взгляда (куда смотрит мышь)
                    Vector lookDir = currentLoc.getDirection().setY(0).normalize();

                    Vector velocity = p.getVelocity();
                    double velX = velocity.getX();
                    double velZ = velocity.getZ();

                    // Магия HL1: Если скорость совпадает с взглядом, мы не даем буст (чтобы не лететь прямо).
                    // Мы даем буст только если игрок двигается боком (стрейфит).

                    // Но для простоты мы просто даем скорость вперед, если он двигается.
                    // Чтобы не было бесконечного ускорения, мы ограничиваем максимальную скорость.

                    double currentSpeed = Math.sqrt(velX * velX + velZ * velZ);

                    if (currentSpeed < 1.5) { // Лимит скорости (1.5 блока/тик = 30 бл/с)
                        // Добавляем скорость в направлении взгляда
                        double addSpeed = 0.05; // Сила добавки
                        velX += lookDir.getX() * addSpeed;
                        velZ += lookDir.getZ() * addSpeed;

                        p.setVelocity(new Vector(velX, velocity.getY(), velZ));
                    }
                }
            }
            lastLocations.put(p.getUniqueId(), currentLoc.clone());
            s.onGround = p.isOnGround();
        }
    }
}