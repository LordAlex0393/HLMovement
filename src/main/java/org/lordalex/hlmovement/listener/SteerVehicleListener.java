package org.lordalex.hlmovement.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.lordalex.hlmovement.HLMovement;
import org.lordalex.hlmovement.PlayerStateManager;

public class SteerVehicleListener extends PacketAdapter {
    public SteerVehicleListener(HLMovement plugin, PlayerStateManager stateManager) {
        super(plugin, ListenerPriority.LOWEST, PacketType.Play.Client.POSITION);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        // Пусто. Физика теперь в PlayerStateManager.
    }
}