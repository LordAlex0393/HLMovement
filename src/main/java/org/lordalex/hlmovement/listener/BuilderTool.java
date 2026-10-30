package org.lordalex.hlmovement.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.lordalex.hlmovement.utils.ColorUtil;

public class BuilderTool implements Listener {

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // Проверяем, что кликнули правой кнопкой по воздуху или блоку
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player p = event.getPlayer();
        ItemStack item = p.getItemInHand();

        // Проверяем, что в руке железный слиток
        if (item == null || item.getType() != Material.IRON_INGOT) {
            return;
        }

        // Чтобы не ломать блоки, отменяем стандартное действие
        event.setCancelled(true);

        Location center = p.getLocation();
        World world = center.getWorld();

        // Центр платформы - под ногами игрока (на 1 блок ниже)
        int cx = center.getBlockX();
        int cy = center.getBlockY() - 1;
        int cz = center.getBlockZ();

        // --- СТРОИТЕЛЬСТВО ПЛАТФОРМЫ 5x5 (КРУГ) ---
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                // Вычисляем расстояние от центра
                double distance = Math.sqrt(x * x + z * z);

                // Если это углы (слишком далеко), пропускаем, чтобы получить круг
                if (distance > 2.2) continue;

                Location blockLoc = new Location(world, cx + x, cy, cz + z);

                // Узор: края - золото, центр - железо
                if (distance > 1.3) {
                    blockLoc.getBlock().setType(Material.GOLD_BLOCK);
                } else {
                    blockLoc.getBlock().setType(Material.IRON_BLOCK);
                }
            }
        }

        // --- ДОБАВЛЯЕМ ОБЪЁМ СНИЗУ (ПОЛУСФЕРА) ---
        // Для красоты добавим слой ниже (на 1 блок вниз)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Location blockLoc = new Location(world, cx + x, cy - 1, cz + z);
                // Снизу только центр (крест), чтобы не было квадрата
                if (Math.abs(x) + Math.abs(z) <= 1) {
                    blockLoc.getBlock().setType(Material.IRON_BLOCK);
                }
            }
        }

        p.sendMessage(ColorUtil.get("&aПлатформа создана!"));
    }
}