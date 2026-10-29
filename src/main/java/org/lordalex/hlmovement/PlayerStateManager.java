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

    // ОГРОМНЫЙ РАЗГОН ДЛЯ ТЕСТА
    private final double HL_MAXSPEED = 0.28;
    private final double HL_AIRACCEL = 2000.0; // ОГРОМНАЯ СИЛА
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

            if (lastLoc != null) {
                // 1. Берем ФАКТИЧЕСКИЙ вектор движения (куда игрок сместился в ванили)
                double dx = currentLoc.getX() - lastLoc.getX();
                double dz = currentLoc.getZ() - lastLoc.getZ();
                double dist = Math.sqrt(dx*dx + dz*dz);

                // 2. Если сдвиг был (нажимал WASD)
                if (dist > 0.01) {
                    double yawRad = Math.toRadians(currentLoc.getYaw());
                    double sin = Math.sin(yawRad);
                    double cos = Math.cos(yawRad);

                    // Преобразуем мировые координаты в локальные (относительно взгляда)
                    // Если dx,dz совпадают с направлением взгляда - это forward
                    // Если перпендикулярно - это strafe
                    double localForward = -sin * dx - cos * dz;
                    double localStrafe = cos * dx - sin * dz;

                    // Нормализуем (чтобы не зависеть от скорости)
                    s.forward = (float) (localForward / dist);
                    s.strafe = (float) (localStrafe / dist);

                    // ДЕБАГ! Без русских букв, только цифры.
                    if (dist > 0.05) {
                        System.out.println("[HL-DEBUG] forward=" + String.format("%.2f", s.forward)
                                + " strafe=" + String.format("%.2f", s.strafe));
                    }
                } else {
                    s.forward = 0;
                    s.strafe = 0;
                }
            }
            lastLocations.put(p.getUniqueId(), currentLoc.clone());
            s.onGround = p.isOnGround();

            // Считаем wishdir
            double len = Math.sqrt(s.forward * s.forward + s.strafe * s.strafe);
            if (len > 0.001) {
                double yawRad = Math.toRadians(currentLoc.getYaw());
                double sin = Math.sin(yawRad);
                double cos = Math.cos(yawRad);
                s.wishX = (-sin * (s.forward/len) + cos * (s.strafe/len));
                s.wishZ = (-cos * (s.forward/len) - sin * (s.strafe/len));
            } else {
                s.wishX = 0;
                s.wishZ = 0;
            }

            Vector velocity = p.getVelocity();
            double velX = velocity.getX();
            double velZ = velocity.getZ();
            double velY = velocity.getY();

            // --- ФИЗИКА (ТОЛЬКО В ВОЗДУХЕ) ---
            if (!s.onGround) {
                double currentSpeed = velX * s.wishX + velZ * s.wishZ;
                double addSpeed = HL_WISHSPEED_CAP - currentSpeed;

                // Если есть желание двигаться (жмёт WASD) и не превысили лимит
                if (addSpeed > 0 && (Math.abs(s.wishX) > 0.01 || Math.abs(s.wishZ) > 0.01)) {
                    double accelSpeed = HL_AIRACCEL * HL_WISHSPEED_CAP * 0.05;
                    if (accelSpeed > addSpeed) accelSpeed = addSpeed;

                    velX += s.wishX * accelSpeed;
                    velZ += s.wishZ * accelSpeed;
                } else {
                    // Если не жмёт WASD, гасим скорость, чтобы не было "льда"
                    velX *= 0.99;
                    velZ *= 0.99;
                }
            }

            p.setVelocity(new Vector(velX, velY, velZ));
        }
    }
}