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

    // Прибавка скорости
    private final double ACCEL_PER_TICK = 0.15;
    private final double MAX_CAP = 10.0;

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

            // Скорость для сайдбара
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

            if (!s.onGround) {
                float yawDelta = s.yaw - s.lastYaw;
                if (yawDelta > 180) yawDelta -= 360;
                if (yawDelta < -180) yawDelta += 360;

                if (Math.abs(yawDelta) > 0.05f) {
                    Vector currentVel = p.getVelocity();
                    double velX = currentVel.getX();
                    double velZ = currentVel.getZ();
                    double velY = currentVel.getY();

                    double currentSpeed = Math.sqrt(velX * velX + velZ * velZ);

                    if (currentSpeed > 0.05) {
                        // Направление, куда мы летим прямо сейчас
                        Vector currentDir = new Vector(velX / currentSpeed, 0, velZ / currentSpeed);

                        // Направление взгляда
                        Vector lookDir = p.getLocation().getDirection().setY(0).normalize();

                        // Боковой вектор
                        Vector strafeDir = new Vector(-lookDir.getZ(), 0, lookDir.getX());

                        double turn = Math.signum(yawDelta);

                        // --- НОВЫЙ ВЕКТОР: 70% ВПЕРЁД + 30% ВБОК ---
                        // Мы берем направление взгляда (вперед) и добавляем немного вбок
                        double pushX = (lookDir.getX() * 0.7) + (strafeDir.getX() * turn * 0.3);
                        double pushZ = (lookDir.getZ() * 0.7) + (strafeDir.getZ() * turn * 0.3);

                        // Нормализуем, чтобы длина была 1
                        double pushLen = Math.sqrt(pushX * pushX + pushZ * pushZ);
                        pushX /= pushLen;
                        pushZ /= pushLen;

                        // Добавляем скорость
                        double newX = velX + (pushX * ACCEL_PER_TICK);
                        double newZ = velZ + (pushZ * ACCEL_PER_TICK);

                        // Потолок
                        double newSpeed = Math.sqrt(newX * newX + newZ * newZ);
                        if (newSpeed > MAX_CAP) {
                            newX = (newX / newSpeed) * MAX_CAP;
                            newZ = (newZ / newSpeed) * MAX_CAP;
                        }

                        p.setVelocity(new Vector(newX, velY, newZ));
                    }
                }
            }

            s.lastYaw = s.yaw;
        }
    }
}