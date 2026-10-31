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

            // 1. Обновляем сглаженную скорость
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

            // 2. ФИЗИКА (только в воздухе и если включено)
            if (plugin.isMovementEnabled(p) && !s.onGround) {
                Vector currentVel = p.getVelocity();
                double velX = currentVel.getX();
                double velZ = currentVel.getZ();
                double velY = currentVel.getY();
                double currentSpeed = Math.sqrt(velX * velX + velZ * velZ);

                // Если мы летим с какой-то скоростью
                if (currentSpeed > 0.05) {
                    // Направление текущего полета
                    Vector velDir = new Vector(velX / currentSpeed, 0, velZ / currentSpeed);

                    // Направление взгляда (куда смотрим)
                    Vector lookDir = p.getLocation().getDirection().setY(0).normalize();

                    // Боковой вектор (вправо от взгляда)
                    Vector rightDir = new Vector(-lookDir.getZ(), 0, lookDir.getX());

                    // Смотрим, насколько мышь ушла от направления полета
                    float yawDelta = s.yaw - s.lastYaw;
                    if (yawDelta > 180) yawDelta -= 360;
                    if (yawDelta < -180) yawDelta += 360;

                    // Если игрок крутит мышку
                    if (Math.abs(yawDelta) > 0.05f) {
                        // Определяем, в какую сторону крутится мышь (1 = вправо, -1 = влево)
                        double turnDir = Math.signum(yawDelta);

                        // Определяем, нажимает ли он A или D
                        // Смотрим на угол между полетом и взглядом.
                        // Если мы летим вправо, а смотрим влево - это ошибка.
                        double dot = velDir.dot(lookDir);

                        // Вычисляем угол в градусах
                        double angleDeg = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));

                        // Если угол слишком большой (смотрим больше чем на 90 градусов вбок) - это не стрейф
                        if (angleDeg < 90.0) {
                            // --- ЛОГИКА БАЛАНСА ---
                            // Если мы смотрим вправо (angle > 0) и нажимаем D (turnDir > 0), то это ПРАВИЛЬНО.
                            // Если мы смотрим вправо, но нажимаем A (turnDir < 0), то это НЕПРАВИЛЬНО (штраф).

                            // Определяем, куда мы смотрим относительно полета (вправо или влево)
                            // Используем скалярное произведение с rightDir
                            double lookSide = lookDir.dot(rightDir);

                            // Если lookSide > 0, мы смотрим вправо. Если < 0, влево.
                            double inputSide = turnDir; // 1 = вправо (D), -1 = влево (A)

                            // Смотрим, совпадают ли направления
                            double directionMatch = lookSide * inputSide;
                            // 1.0 = идеально (смотрим вправо и нажимаем D)
                            // -1.0 = ошибка (смотрим вправо, но нажимаем A)

                            // --- ПРИМЕНЯЕМ БУСТ ИЛИ ШТРАФ ---
                            double accel = 0;

                            if (directionMatch > 0.3) {
                                // ПРАВИЛЬНЫЙ СТРЕЙФ: даем буст
                                // Чем больше угол (до 45 градусов), тем сильнее буст
                                double angleFactor = Math.min(1.0, angleDeg / 45.0);
                                accel = settings.getAccelPerTick() * angleFactor;

                                // Применяем буст в направлении ВЗГЛЯДА (вперёд-вбок)
                                // Но мы хотим, чтобы он летел вперёд, поэтому смешиваем с текущим полетом
                                Vector pushDir = new Vector(
                                        (lookDir.getX() * settings.getForwardMultiplier()) + (rightDir.getX() * directionMatch * settings.getStrafeMultiplier()),
                                        0,
                                        (lookDir.getZ() * settings.getForwardMultiplier()) + (rightDir.getZ() * directionMatch * settings.getStrafeMultiplier())
                                ).normalize();

                                velX += pushDir.getX() * accel;
                                velZ += pushDir.getZ() * accel;

                            } else if (directionMatch < -0.3) {
                                // НЕПРАВИЛЬНЫЙ СТРЕЙФ: штраф (гасим скорость)
                                accel = settings.getAccelPerTick() * 0.5; // Сила штрафа
                                velX *= (1.0 - accel);
                                velZ *= (1.0 - accel);
                            } else {
                                // Нейтрально (просто крутим мышкой) - минимальный буст
                                accel = settings.getAccelPerTick() * 0.1;
                                velX += lookDir.getX() * accel;
                                velZ += lookDir.getZ() * accel;
                            }

                            // Ограничение скорости
                            double newSpeed = Math.sqrt(velX * velX + velZ * velZ);
                            if (newSpeed > settings.getMaxCap()) {
                                velX = (velX / newSpeed) * settings.getMaxCap();
                                velZ = (velZ / newSpeed) * settings.getMaxCap();
                            }

                            p.setVelocity(new Vector(velX, velY, velZ));
                        }
                    } else {
                        // Если мышка не крутится - никакого буста
                        // Но скорость не сбрасываем (она есть по инерции)
                    }
                }
            }

            s.lastYaw = s.yaw;
        }
    }
}