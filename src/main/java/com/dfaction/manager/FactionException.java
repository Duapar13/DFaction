package com.dfaction.manager;

/**
 * Erreur "attendue" (mauvais usage, permission, état invalide...) destinée à être
 * affichée telle quelle au joueur par la commande qui l'a déclenchée.
 */
public class FactionException extends RuntimeException {

    public FactionException(String message) {
        super(message);
    }
}
