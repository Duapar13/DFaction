# DFaction

**Le premier plugin de factions open source pour Minecraft 26.1.x**, développé
pour Spigot. Claims par chunk, power, protection anti-grief, raid à la TNT :
la base classique d'un plugin faction, en open source et pensée pour être
étendue par la communauté.

## Fonctionnalités

- **Factions** : création, invitation, ascension automatique d'un nouveau chef
  en cas de départ, dissolution.
- **Claims par chunk** : `/f claim` et `/f unclaim`, limités par le power total
  de la faction.
- **Protection anti-grief** : un joueur hors faction ne peut ni casser, ni
  poser de bloc, ni interagir avec un coffre / four / table de craft / enclume
  / ender chest / etc. sur un chunk claim par une autre faction.
- **Raid à la TNT** : seule une explosion de TNT peut détruire des blocs à
  l'intérieur d'un claim ennemi (configurable) ; toute autre explosion
  (creeper...) est bloquée dans les zones claim.
- **Système de power** : chaque joueur régénère du power dans le temps
  (jusqu'à un maximum configurable), en perd à la mort, et le nombre de
  claims d'une faction est limité par le power total de ses membres.
- **Affichage à l'écran** : un titre s'affiche quand un joueur entre sur un
  territoire (le sien, un territoire ennemi, ou une zone sauvage).
- **Stockage au choix** : fichiers YAML locaux par défaut, ou base MySQL
  (host/port/base/utilisateur/mot de passe dans `config.yml`) sans rien
  changer au code.

## Commandes

| Commande | Description |
|---|---|
| `/f create <nom>` | Crée une faction (10 caractères max). |
| `/f claim` | Claim le chunk sur lequel tu te trouves. |
| `/f unclaim` | Retire le claim du chunk sur lequel tu te trouves. |
| `/f power [joueur]` | Affiche ton power, ou celui d'un autre joueur. |
| `/f show [nom]` | Affiche les infos d'une faction (description, power, claims, membres). |
| `/f invite <joueur>` | Invite un joueur (chef uniquement). |
| `/f join <nom>` | Rejoint une faction après invitation. |
| `/f leave` | Quitte sa faction. |
| `/f disband` | Dissout sa faction (chef uniquement). |

## Configuration (`config.yml`)

```yaml
storage:
  type: local   # local ou mysql
  mysql:
    host: localhost
    port: 3306
    database: dfaction
    username: root
    password: ""

faction:
  max-name-length: 10

power:
  max: 10.0
  starting: 10.0
  regen-amount: 1.0
  regen-interval-minutes: 10
  loss-per-death: 2.0
  claim-cost: 1.0

claims:
  only-tnt-breaks-claims: true
```

## Compiler le projet

Le plugin est un projet Maven standard, compilé contre l'API Spigot 26.1.2.

```
mvn clean package
```

Le jar `spigot-api-26.1.2-R0.1-SNAPSHOT.jar` n'est **pas fourni** dans ce dépôt
(taille + licence Mojang) : voir [`libs/README.md`](libs/README.md) pour la
procédure (BuildTools + installation dans le dépôt Maven local).

## Roadmap / idées d'extension

- Rangs supplémentaires (officier...) et permissions par rang.
- Relations entre factions (alliés / ennemis).
- `/f home` et téléportation.
- Interface graphique pour `/f show`.

## Contribuer

Le projet est ouvert aux contributions (issues, pull requests). Toute idée
pour enrichir ce plugin faction est la bienvenue !

## Licence

Distribué sous licence MIT — voir [`LICENSE`](LICENSE).
