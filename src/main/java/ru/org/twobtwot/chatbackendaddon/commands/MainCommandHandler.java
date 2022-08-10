package ru.org.twobtwot.chatbackendaddon.commands;


import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.org.twobtwot.chatbackendaddon.ChatBackendAddon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainCommandHandler implements CommandExecutor, TabExecutor {
    private final ChatBackendAddon plugin;

    public MainCommandHandler(ChatBackendAddon plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender.hasPermission("chatbackendaddon.admin") && args.length > 0) {
            if (args[0].equalsIgnoreCase("reload")) {
                plugin.reloadConfig();
                sender.sendMessage(ChatColor.GOLD + "Reloaded the config!");
            }
        }

        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> suggestion = new ArrayList<>();
            suggestion.add("reload");
            List<String> completions = new ArrayList<>();
            StringUtil.copyPartialMatches(args[0], suggestion, completions);
            Collections.sort(completions);
            return completions;
        } else {
            return new ArrayList<>();
        }
    }
}
