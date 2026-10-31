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

            if (lastLoc != null) {
                double dx = currentLoc.getX() - lastLoc.getX();
                double dz = currentLoc.getZ() - lastLoc.getZ();

                // 1. Считаем локальные координаты (вперёд / вбок)
                double yawRad = Math.toRadians(currentLoc.getYaw());
                double sin = Math.sin(yawRad);
                double cos = Math.cos(yawRad);

                // localForward - движение вперед/назад, localStrafe - движение вбок
                double localForward = sin * dx + cos * dz;
                double localStrafe = -cos * dx + sin * dz;

                // Обновляем сглаженную скорость для физики
                if (Math.abs(dx) < 2.0 && Math.abs(dz) < 2.0) {
                    s.vx = s.vx * 0.8 + dx * 0.2;
                    s.vz = s.vz * 0.8 + dz * 0.2;
                }

                // 2. ТЕСТ: Смотрим только на боковое смещение (A/D)
                // Если игрок сдвинулся вбок больше чем на 0.05 блока за тик
                boolean isStrafing = Math.abs(localStrafe) > 0.05;
                double strafeDir = Math.signum(localStrafe); // 1 = D (вправо), -1 = A (влево)

                // 3. Физика
                if (plugin.isMovementEnabled(p) && !s.onGround && isStrafing) {
                    Vector currentVel = p.getVelocity();
                    double velX = currentVel.getX();
                    double velZ = currentVel.getZ();
                    double velY = currentVel.getY();

                    double currentSpeed = Math.sqrt(velX * velX + velZ * velZ);

                    if (currentSpeed > 0.05) {
                        // Направление взгляда
                        Vector lookDir = p.getLocation().getDirection().setY(0).normalize();

                        // Направление стрейфа (вбок от взгляда)
                        Vector strafeDirVec = new Vector(-lookDir.getZ(), 0, lookDir.getX());

                        // Буст дается В НАПРАВЛЕНИИ ВЗГЛЯДА (вперед),
                        // НО только если игрок реально нажимает A/D (isStrafing = true).
                        // Чем сильнее нажата клавиша (больше localStrafe), тем больше буст.

                        double accel = settings.getAccelPerTick() * Math.min(1.0, Math.abs(localStrafe) * 20.0);

                        // Добавляем скорость вбок и немного вперед
                        double newX = velX + (strafeDirVec.getX() * strafeDir * accel * 0.5) + (lookDir.getX() * accel);
                        double newZ = velZ + (strafeDirVec.getZ() * strafeDir * accel * 0.5) + (lookDir.getZ() * accel);

                        // Убираем потолок скорости (ставим технический 1000)
                        double newSpeed = Math.sqrt(newX * newX + newZ * newZ);
                        if (newSpeed > 1000.0) {
                            newX = (newX / newSpeed) * 1000.0;
                            newZ = (newZ / newSpeed) * 1000.0;
                        }

                        p.setVelocity(new Vector(newX, velY, newZ));
                    }
                }
            }
            lastLocations.put(p.getUniqueId(), currentLoc.clone());
            s.onGround = p.isOnGround();
        }
    }
}