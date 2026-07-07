package com.dfaction.storage;

import com.dfaction.model.FPlayer;
import com.dfaction.model.Faction;

import java.util.Map;
import java.util.UUID;

public interface FactionStorage {

    /**
     * Ouvre la connexion / prépare les fichiers. Appelé une seule fois au démarrage,
     * de façon synchrone (avant que le reste du plugin ne s'active).
     */
    void init() throws Exception;

    /**
     * Charge toutes les factions connues. Clé de la map = nom de faction en minuscule.
     */
    Map<String, Faction> loadFactions() throws Exception;

    /**
     * Charge toutes les données joueurs connues.
     */
    Map<UUID, FPlayer> loadPlayers() throws Exception;

    /**
     * Sauvegarde (insert/update) une faction, y compris ses membres et ses claims.
     */
    void saveFaction(Faction faction);

    /**
     * Supprime définitivement une faction (et ses membres/claims) du stockage.
     */
    void deleteFaction(String nameLower);

    /**
     * Sauvegarde (insert/update) les données d'un joueur.
     */
    void savePlayer(FPlayer fPlayer);

    /**
     * Ferme proprement les ressources (connexions, etc.) à l'arrêt du plugin.
     */
    void close();
}
