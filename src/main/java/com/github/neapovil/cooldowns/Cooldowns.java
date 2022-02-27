package com.github.neapovil.cooldowns;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.electronwill.nightconfig.core.file.FileConfig;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;

public final class Cooldowns extends JavaPlugin implements Listener
{
    private static Cooldowns instance;
    private FileConfig config;
    private final List<UUID> players = new ArrayList<>();

    @Override
    public void onEnable()
    {
        instance = this;

        this.saveResource("cooldowns.toml", false);

        this.config = FileConfig.builder(new File(this.getDataFolder(), "cooldowns.toml"))
                .autoreload()
                .autosave()
                .build();
        this.config.load();

        this.getServer().getPluginManager().registerEvents(this, this);

        new CommandAPICommand("cooldowns")
                .withPermission("cooldowns.command")
                .withArguments(new LiteralArgument("set"))
                .withArguments(new MultiLiteralArgument("ender_pearl", "golden_apple"))
                .withArguments(new IntegerArgument("seconds"))
                .executes((sender, args) -> {
                    final String setting = (String) args[0];
                    final int cd = (int) args[1];

                    this.config.set("general." + setting, cd);

                    sender.sendMessage(setting + " cooldown changed to: " + cd + "s");
                })
                .register();
    }

    @Override
    public void onDisable()
    {
    }

    public static Cooldowns getInstance()
    {
        return instance;
    }

    @EventHandler
    private void projectileLaunch(ProjectileLaunchEvent event)
    {
        if (!(event.getEntity().getShooter() instanceof Player))
        {
            return;
        }

        if (!event.getEntityType().equals(EntityType.ENDER_PEARL))
        {
            return;
        }

        final Player player = (Player) event.getEntity().getShooter();

        this.getServer().getScheduler().runTaskLater(this, () -> {
            player.setCooldown(Material.ENDER_PEARL, 20 * this.config.getInt("general.ender_pearl"));
        }, 0);
    }

    @EventHandler
    private void playerItemConsume(PlayerItemConsumeEvent event)
    {
        if (!event.getItem().getType().equals(Material.GOLDEN_APPLE))
        {
            return;
        }

        final Player player = event.getPlayer();

        if (!player.hasCooldown(Material.GOLDEN_APPLE))
        {
            this.players.removeIf(u -> u.equals(player.getUniqueId()));
        }

        if (this.players.contains(player.getUniqueId()))
        {
            event.setCancelled(true);
            return;
        }

        this.players.add(player.getUniqueId());

        this.getServer().getScheduler().runTaskLater(this, () -> {
            player.setCooldown(Material.GOLDEN_APPLE, 20 * this.config.getInt("general.golden_apple"));
        }, 0);
    }
}
