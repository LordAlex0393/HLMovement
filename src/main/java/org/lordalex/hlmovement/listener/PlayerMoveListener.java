package org.lordalex.hlmovement.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.lordalex.hlmovement.PlayerMoveState;
import org.lordalex.hlmovement.PlayerStateManager;

public class PlayerMoveListener implements Listener {

    private final PlayerStateManager stateManager;

    public PlayerMoveListener(PlayerStateManager sm) {
        this.stateManager = sm;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        PlayerMoveState s = stateManager.get(event.getPlayer());
        s.yaw = event.getTo().getYaw();
    }
}