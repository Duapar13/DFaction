# Dossier `libs/`

Ce dossier accueille localement les jars Spigot nécessaires à la compilation.
Ils ne sont **pas** versionnés (voir `.gitignore`) : ce sont des builds compilés
depuis le code de Minecraft via BuildTools, dont la redistribution n'est pas
autorisée par le EULA de Mojang. Chaque contributeur doit générer les siens.

## Mise en place

1. Génère le jar serveur Spigot 26.1.2 avec [BuildTools](https://www.spigotmc.org/wiki/buildtools/) :
   ```
   java -jar BuildTools.jar --rev 26.1.2
   ```
2. Extrais l'API du jar serveur généré (c'est un jar "bundler", l'API pure est
   embarquée dedans) :
   ```
   jar xf spigot-26.1.2.jar META-INF/libraries/spigot-api-26.1.2-R0.1-SNAPSHOT.jar
   mv META-INF/libraries/spigot-api-26.1.2-R0.1-SNAPSHOT.jar libs/
   ```
3. Installe-la dans ton dépôt Maven local :
   ```
   mvn install:install-file -Dfile=libs/spigot-api-26.1.2-R0.1-SNAPSHOT.jar ^
     -DgroupId=org.spigotmc -DartifactId=spigot-api ^
     -Dversion=26.1.2-R0.1-SNAPSHOT -Dpackaging=jar -DgeneratePom=true
   ```
4. `mvn clean package` fonctionne ensuite normalement (voir le `pom.xml` à la racine).
