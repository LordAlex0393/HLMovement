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
    private Settings settings;
    private final Map<UUID, PlayerMoveState> states = new ConcurrentHashMap<>();
    private final Map<UUID, Location> lastLocations = new ConcurrentHashMap<>();
    private BukkitTask task;

    public PlayerStateManager(HLMovement plugin, Settings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public void updateSettings(Settings newSettings) {
        this.settings = newSettings;
    }

    public PlayerMoveState get(Player p) {
        return states.computeIfAbsent(p.getUniqueId(), k -> new PlayerMoveState());
    }

    public void remove(Player p) {
        states.remove(p.getUniqueId());
        lastLocations.remove(p.getUniqueId());
        p.setLevel(0);
        p.setExp(0);
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
        if (!settings.isEnabled()) return;

        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerMoveState s = get(p);
            Location currentLoc = p.getLocation();
            Location lastLoc = lastLocations.get(p.getUniqueId());

            // Обновляем сглаженную скорость
            if (lastLoc != null) {
                double dx = currentLoc.getX() - lastLoc.getX();
                double dz = currentLoc.getZ() - lastLoc.getZ();
                if (Math.abs(dx) < 2.0 && Math.abs(dz) < 2.0) {
                    s.vx = s.vx * 0.8 + dx * 0.2;
                    s.vz = s.vz * 0.8 + dz * 0.2;
                }
            }
            lastLocations.put(p.getUniqueId(), currentLoc.clone());
            s.onGround = p.isOnGround();

            // ФИЗИКА
            if (plugin.isMovementEnabled(p) && !s.onGround) {
                Vector currentVel = p.getVelocity();
                double velX = currentVel.getX();
                double velZ = currentVel.getZ();
                double velY = currentVel.getY();
                double currentSpeed = Math.sqrt(velX * velX + velZ * velZ);

                if (currentSpeed > 0.1) {
                    // 1. Куда мы летим (вектор скорости)
                    Vector velDir = new Vector(velX / currentSpeed, 0, velZ / currentSpeed);

                    // 2. Куда смотрит игрок
                    Vector lookDir = p.getLocation().getDirection().setY(0).normalize();

                    // 3. Угол между полетом и взглядом
                    double dot = velDir.dot(lookDir);
                    // dot = 1.0 (летим прямо), 0.0 (летим вбок), -1.0 (летим назад)
                    double angleDeg = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));

                    // Если угол меньше 45 градусов (смотрим примерно туда, куда летим)
                    if (angleDeg < 30.0) {
                        // Смотрим, нажимает ли он A или D (по смещению мыши)
                        float yawDelta = s.yaw - s.lastYaw;
                        if (yawDelta > 180) yawDelta -= 360;
                        if (yawDelta < -180) yawDelta += 360;

                        // Если мышь крутится плавно (не резко)
                        if (Math.abs(yawDelta) > 0.05f && Math.abs(yawDelta) < 10.0f) {
                            // Считаем силу стрейфа: чем ближе угол к 45°, тем сильнее буст
                            // (но не больше 1.0)
                            double strafePower = Math.sin(Math.toRadians(angleDeg));

                            // Множитель: даем буст только если смотрим вперёд-вбок
                            double accel = settings.getAccelPerTick() * strafePower;

                            // Добавляем скорость В НАПРАВЛЕНИИ ВЗГЛЯДА (а не вбок!)
                            // Но мы хотим, чтобы он летел вперёд, поэтому добавляем чуть-чуть
                            // к текущему вектору полета.
                            Vector pushDir = velDir.clone().add(lookDir).normalize();

                            double newX = velX + (pushDir.getX() * accel);
                            double newZ = velZ + (pushDir.getZ() * accel);

                            // Ограничение скорости
                            double newSpeed = Math.sqrt(newX * newX + newZ * newZ);
                            if (newSpeed > settings.getMaxCap()) {
                                newX = (newX / newSpeed) * settings.getMaxCap();
                                newZ = (newZ / newSpeed) * settings.getMaxCap();
                            }

                            p.setVelocity(new Vector(newX, velY, newZ));
                        }
                    }
                }
            }

            s.lastYaw = s.yaw;
        }
    }
}