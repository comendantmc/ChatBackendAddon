package ru.org.twobtwot.chatbackendaddon;

import org.bstats.bukkit.Metrics;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import ru.org.twobtwot.chatbackendaddon.Listeners.PistonChatListener;

import java.util.logging.Logger;

public final class ChatBackendAddon extends JavaPlugin {

    @Override
    public void onEnable() {
        Logger log = getLogger();

        log.info(ChatColor.YELLOW + "Loading config");
        saveDefaultConfig();

        log.info(ChatColor.YELLOW + "Registering listeners");
        getServer().getPluginManager().registerEvents(new PistonChatListener(this), this);

        log.info(ChatColor.YELLOW + "Loading metrics");
        if (this.getConfig().getBoolean("enable-metrics"))
            new Metrics(this, 16028);

        getLogger().info(ChatColor.YELLOW + "Done! :D");
    }
}
