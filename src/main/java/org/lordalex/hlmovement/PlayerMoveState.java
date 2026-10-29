package org.lordalex.hlmovement;

import org.bukkit.util.Vector;

public class PlayerMoveState {

    /** Текущая горизонтальная скорость плагина (BPT). */
    public double vx, vz;

    /** Желаемое направление (WASD), уже с учётом yaw, нормализованное. */
    public double wishX, wishZ;

    /** Нажатые клавиши из пакета. */
    public float forward, strafe;
    public boolean jumpPressed;
    public boolean wasJumpPressed;

    public float yaw;

    /** Кэш onGround, обновляется раз в тик из Player.isOnGround(). */
    public boolean onGround;

    /** Флаг «последний тик был прыжок» — для auto_bhop. */
    public boolean justJumped;
    public float lastYaw;

    public double getSpeedBpt() {
        return Math.sqrt(vx * vx + vz * vz);
    }

    public double getSpeedBlocksPerSecond() {
        return getSpeedBpt() * 20.0;
    }
}
