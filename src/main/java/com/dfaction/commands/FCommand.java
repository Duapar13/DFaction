package com.dfaction.commands;

import com.dfaction.manager.FactionException;
import com.dfaction.manager.FactionManager;
import com.dfaction.manager.PowerManager;
import com.dfaction.model.FPlayer;
import com.dfaction.model.Faction;
import com.dfaction.model.Role;
import com.dfaction.util.Msg;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class FCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "create", "claim", "unclaim", "power", "show", "invite", "join", "leave", "disband", "help"
    );

    private final FactionManager factionManager;
    private final PowerManager powerManager;

    public FCommand(FactionManager factionManager, PowerManager powerManager) {
        this.factionManager = factionManager;
        this.powerManager = powerManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        try {
            switch (sub) {
                case "create":
                    handleCreate(sender, args);
                    break;
                case "claim":
                    handleClaim(sender);
                    break;
                case "unclaim":
                    handleUnclaim(sender);
                    break;
                case "power":
                    handlePower(sender, args);
                    break;
                case "show":
                    handleShow(sender, args);
                    break;
                case "invite":
                    handleInvite(sender, args);
                    break;
                case "join":
                    handleJoin(sender, args);
                    break;
                case "leave":
                    handleLeave(sender);
                    break;
                case "disband":
                    handleDisband(sender);
                    break;
                default:
                    sendHelp(sender);
                    break;
            }
        } catch (FactionException e) {
            Msg.error(sender, e.getMessage());
        }
        return true;
    }

    private Player requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            throw new FactionException("Seul un joueur peut utiliser cette commande.");
        }
        return (Player) sender;
    }

    private void handleCreate(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        String name = args.length >= 2 ? args[1] : "";
        Faction faction = factionManager.createFaction(player, name);
        Msg.success(sender, "Faction " + ChatColor.GOLD + faction.getName() + ChatColor.GREEN + " créée avec succès !");
    }

    private void handleClaim(CommandSender sender) {
        Player player = requirePlayer(sender);
        Faction faction = factionManager.claim(player);
        Msg.success(sender, "Chunk claim pour la faction " + ChatColor.GOLD + faction.getName() + ChatColor.GREEN
                + " (" + faction.getClaims().size() + "/" + factionManager.getMaxClaims(faction) + ").");
    }

    private void handleUnclaim(CommandSender sender) {
        Player player = requirePlayer(sender);
        Faction faction = factionManager.unclaim(player);
        Msg.success(sender, "Chunk unclaim pour la faction " + ChatColor.GOLD + faction.getName() + ChatColor.GREEN
                + " (" + faction.getClaims().size() + "/" + factionManager.getMaxClaims(faction) + ").");
    }

    private void handlePower(CommandSender sender, String[] args) {
        FPlayer target;
        String displayName;
        if (args.length >= 2) {
            target = powerManager.getByName(args[1]);
            if (target == null) {
                throw new FactionException("Ce joueur n'a jamais été vu sur ce serveur.");
            }
            displayName = target.getName();
        } else {
            Player player = requirePlayer(sender);
            target = powerManager.getOrCreate(player);
            displayName = "Vous";
        }
        Msg.send(sender, displayName + " : " + ChatColor.AQUA + target.getPower() + ChatColor.GRAY
                + " / " + ChatColor.AQUA + powerManager.getMaxPower() + ChatColor.GRAY + " power.");
    }

    private void handleShow(CommandSender sender, String[] args) {
        Faction faction;
        if (args.length >= 2) {
            faction = factionManager.getByName(args[1]);
            if (faction == null) {
                throw new FactionException("Cette faction n'existe pas.");
            }
        } else {
            Player player = requirePlayer(sender);
            FPlayer fp = powerManager.getOrCreate(player);
            faction = factionManager.getFactionOf(fp);
            if (faction == null) {
                throw new FactionException("Utilisation: /f show <nom> (vous n'êtes dans aucune faction).");
            }
        }

        UUID ownerUuid = faction.getOwner();
        FPlayer ownerFp = ownerUuid == null ? null : powerManager.get(ownerUuid);

        sender.sendMessage(ChatColor.DARK_GRAY + "==== " + ChatColor.GOLD + faction.getName() + ChatColor.DARK_GRAY + " ====");
        sender.sendMessage(ChatColor.GRAY + "Description: " + ChatColor.WHITE + faction.getDescription());
        sender.sendMessage(ChatColor.GRAY + "Chef: " + ChatColor.WHITE + (ownerFp != null ? ownerFp.getName() : "Inconnu"));
        sender.sendMessage(ChatColor.GRAY + "Power: " + ChatColor.AQUA + factionManager.getTotalPower(faction)
                + ChatColor.GRAY + " | Claims: " + ChatColor.AQUA + faction.getClaims().size()
                + " / " + factionManager.getMaxClaims(faction));

        String members = faction.getMembers().entrySet().stream()
                .map(entry -> {
                    FPlayer mfp = powerManager.get(entry.getKey());
                    String name = mfp != null ? mfp.getName() : entry.getKey().toString();
                    return entry.getValue() == Role.OWNER ? ChatColor.GOLD + name : ChatColor.WHITE + name;
                })
                .collect(Collectors.joining(ChatColor.GRAY + ", "));
        sender.sendMessage(ChatColor.GRAY + "Membres (" + faction.getMembers().size() + "): " + members);
    }

    private void handleInvite(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (args.length < 2) {
            throw new FactionException("Utilisation: /f invite <joueur>");
        }
        factionManager.invite(player, args[1]);
        Msg.success(sender, "Invitation envoyée à " + args[1] + ".");
        Player target = org.bukkit.Bukkit.getPlayerExact(args[1]);
        if (target != null) {
            Msg.send(target, "Vous avez été invité à rejoindre une faction. Tapez " + ChatColor.GOLD
                    + "/f join <nom>" + ChatColor.GRAY + " pour l'accepter.");
        }
    }

    private void handleJoin(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (args.length < 2) {
            throw new FactionException("Utilisation: /f join <nom>");
        }
        Faction faction = factionManager.join(player, args[1]);
        Msg.success(sender, "Vous avez rejoint la faction " + ChatColor.GOLD + faction.getName() + ChatColor.GREEN + " !");
    }

    private void handleLeave(CommandSender sender) {
        Player player = requirePlayer(sender);
        factionManager.leave(player);
        Msg.success(sender, "Vous avez quitté votre faction.");
    }

    private void handleDisband(CommandSender sender) {
        Player player = requirePlayer(sender);
        factionManager.disband(player);
        Msg.success(sender, "Faction dissoute.");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY + "==== " + ChatColor.GOLD + "DFaction" + ChatColor.DARK_GRAY + " ====");
        sender.sendMessage(ChatColor.GOLD + "/f create <nom>" + ChatColor.GRAY + " - Créer une faction (max 10 caractères).");
        sender.sendMessage(ChatColor.GOLD + "/f claim" + ChatColor.GRAY + " - Claim le chunk où vous êtes.");
        sender.sendMessage(ChatColor.GOLD + "/f unclaim" + ChatColor.GRAY + " - Retirer le claim du chunk où vous êtes.");
        sender.sendMessage(ChatColor.GOLD + "/f power [joueur]" + ChatColor.GRAY + " - Voir le power d'un joueur.");
        sender.sendMessage(ChatColor.GOLD + "/f show [nom]" + ChatColor.GRAY + " - Voir les infos d'une faction.");
        sender.sendMessage(ChatColor.GOLD + "/f invite <joueur>" + ChatColor.GRAY + " - Inviter un joueur (chef uniquement).");
        sender.sendMessage(ChatColor.GOLD + "/f join <nom>" + ChatColor.GRAY + " - Rejoindre une faction après invitation.");
        sender.sendMessage(ChatColor.GOLD + "/f leave" + ChatColor.GRAY + " - Quitter votre faction.");
        sender.sendMessage(ChatColor.GOLD + "/f disband" + ChatColor.GRAY + " - Dissoudre votre faction (chef uniquement).");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(partial)).collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("power") || args[0].equalsIgnoreCase("invite"))) {
            String partial = args[1].toLowerCase(Locale.ROOT);
            return org.bukkit.Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(partial))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
