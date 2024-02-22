package com.github.neapovil.cooldowns;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.ItemStackArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.SafeSuggestions;

public final class Cooldowns extends JavaPlugin implements Listener
{
    private static Cooldowns instance;
    private Config config;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void onEnable()
    {
        instance = this;

        this.saveResource("config.json", false);

        try
        {
            this.load();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        this.getServer().getPluginManager().registerEvents(this, this);

        new CommandAPICommand("cooldowns")
                .withPermission("cooldowns.command")
                .withArguments(new LiteralArgument("set"))
                .withArguments(new ItemStackArgument("itemStack"))
                .withArguments(new IntegerArgument("seconds"))
                .executes((sender, args) -> {
                    final ItemStack itemstack = (ItemStack) args.get("itemStack");
                    final int seconds = (int) args.get("seconds");

                    final Config.Item item = new Config.Item();

                    item.material = itemstack.getType();
                    item.seconds = seconds;

                    try
                    {
                        this.config.items.removeIf(i -> i.material.equals(itemstack.getType()));
                        this.config.items.add(item);
                        this.save();
                        sender.sendMessage("Item cooldown set to: " + seconds + " seconds");
                    }
                    catch (IOException e)
                    {
                        this.getLogger().severe(e.getMessage());
                        throw CommandAPI.failWithString("Unable to set");
                    }
                })
                .register();

        new CommandAPICommand("cooldowns")
                .withPermission("cooldowns.command")
                .withArguments(new LiteralArgument("remove"))
                .withArguments(new ItemStackArgument("itemStack").replaceSafeSuggestions(SafeSuggestions.suggest(info -> {
                    return this.config.items.stream().map(i -> new ItemStack(i.material)).toArray(ItemStack[]::new);
                })))
                .executes((sender, args) -> {
                    final ItemStack itemstack = (ItemStack) args.get("itemStack");

                    try
                    {
                        this.config.items.removeIf(i -> i.material.equals(itemstack.getType()));
                        this.save();
                        sender.sendMessage("Item cooldown removed");
                    }
                    catch (IOException e)
                    {
                        this.getLogger().severe(e.getMessage());
                        throw CommandAPI.failWithString("Unable to remove");
                    }
                })
                .register();
    }

    @Override
    public void onDisable()
    {
    }

    public static Cooldowns instance()
    {
        return instance;
    }

    private void load() throws IOException
    {
        final String string = Files.readString(this.getDataFolder().toPath().resolve("config.json"));
        this.config = this.gson.fromJson(string, Config.class);
    }

    private void save() throws IOException
    {
        final String string = this.gson.toJson(this.config);
        Files.write(this.getDataFolder().toPath().resolve("config.json"), string.getBytes());
    }

    private Optional<Config.Item> find(ItemStack itemStack)
    {
        return this.config.items
                .stream()
                .filter(i -> i.material.equals(itemStack.getType()))
                .findFirst();
    }

    @EventHandler
    private void projectileLaunch(ProjectileLaunchEvent event)
    {
        if (!(event.getEntity().getShooter() instanceof Player player))
        {
            return;
        }

        if (!(event.getEntity() instanceof ThrowableProjectile projectile))
        {
            return;
        }

        this.find(projectile.getItem()).ifPresent(i -> {
            if (player.hasCooldown(i.material))
            {
                event.setCancelled(true);
            }
            else
            {
                this.getServer().getScheduler().runTask(this, () -> {
                    player.setCooldown(i.material, 20 * i.seconds);
                });
            }
        });
    }

    @EventHandler
    private void playerItemConsume(PlayerItemConsumeEvent event)
    {
        this.find(event.getItem()).ifPresent(i -> {
            if (event.getPlayer().hasCooldown(i.material))
            {
                event.setCancelled(true);
            }
            else
            {
                this.getServer().getScheduler().runTask(this, () -> {
                    event.getPlayer().setCooldown(i.material, 20 * i.seconds);
                });
            }
        });
    }

    public class Config
    {
        public boolean enabled;
        public List<Item> items = new ArrayList<>();

        public static class Item
        {
            public Material material;
            public int seconds;
        }
    }
}
