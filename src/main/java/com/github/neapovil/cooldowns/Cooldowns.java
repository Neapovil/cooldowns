package com.github.neapovil.cooldowns;

import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;

import com.electronwill.nightconfig.core.file.FileConfig;

public final class Cooldowns extends JavaPlugin
{
    private static Cooldowns instance;
    private FileConfig config;

    @Override
    public void onEnable()
    {
        instance = this;

        this.saveResource("cooldowns.json", false);

        this.config = FileConfig.builder(new File(this.getDataFolder(), "cooldowns.json"))
                .autoreload()
                .autosave()
                .build();
        this.config.load();
    }

    @Override
    public void onDisable()
    {
    }

    public static Cooldowns getInstance()
    {
        return instance;
    }
}
