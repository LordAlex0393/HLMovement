package org.lordalex.hlmovement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final JavaPlugin plugin;
    private Settings settings;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "settings.yml");
        if (!file.exists()) {
            plugin.saveResource("settings.yml", false);
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            this.settings = mapper.readValue(file, Settings.class);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось загрузить settings.yml! Используются настройки по умолчанию.");
            e.printStackTrace();
            this.settings = new Settings(); // Дефолт
        }
    }

    public Settings getSettings() {
        return settings;
    }
}