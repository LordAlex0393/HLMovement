package org.lordalex.hlmovement;

public class PlayerMoveState {
    public double vx, vz;
    public float forward, strafe;
    public boolean jumpPressed;
    public boolean wasJumpPressed;
    public float yaw;
    public float lastYaw;
    public boolean onGround;
    public boolean wasOnGround; // <--- ДОБАВЬ ЭТУ СТРОКУ
    public boolean justJumped;
    public double wishX, wishZ;
    public double speedBps;

    public double getSpeedBpt() {
        return Math.sqrt(vx * vx + vz * vz);
    }

    public double getSpeedBlocksPerSecond() {
        return getSpeedBpt() * 20.0;
    }
}