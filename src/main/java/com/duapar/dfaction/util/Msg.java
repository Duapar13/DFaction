package com.duapar.dfaction.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public final class Msg {

    public static final String PREFIX =
            ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "DFaction" + ChatColor.DARK_GRAY + "] " + ChatColor.GRAY;

    private Msg() {
    }

    public static void send(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + message);
    }

    public static void success(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + ChatColor.GREEN + message);
    }

    public static void error(CommandSender sender, String message) {
        sender.sendMessage(PREFIX + ChatColor.RED + message);
    }
}
