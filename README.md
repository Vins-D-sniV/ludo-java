# Ludo Java

Jeu de Ludo en français pour 2 à 4 personnes sur le même écran ou deux personnes en ligne. React affiche le plateau et Spring Boot gère les tours, les dés, les déplacements et la victoire.

## Jouer en ligne à deux

1. Sur le premier écran, sélectionne **En ligne à deux**, puis **Créer une partie**.
2. Envoie le **Lien d’invitation** à l’autre joueur. Il l’ouvre sur son écran et clique sur **Rejoindre la partie**.
3. Choisis un pseudo au moment de créer ou de rejoindre la partie. Il s’affiche sur les deux écrans. Le joueur qui crée la partie joue en rouge et commence. L’invité joue en vert. Les deux écrans se mettent à jour automatiquement.

Le bouton **Abandonner la partie** donne la victoire à l’autre joueur après confirmation, quel que soit le tour en cours.

Chaque écran conserve sa place tant que son onglet reste ouvert, y compris après une actualisation. Les parties sont conservées en mémoire et disparaissent au redémarrage du serveur. Le bouton **Même écran** donne accès au jeu local à 2–4 joueurs.

### Utiliser deux appareils

Les deux joueurs doivent ouvrir la même adresse, reliée au même serveur. Pour jouer depuis des réseaux différents, publie le JAR ci-dessous sur un serveur Java accessible en HTTPS. Pour jouer sur le même réseau Wi-Fi, ouvre le jeu via l’adresse IP locale du PC qui héberge le serveur. Par exemple, si le PC utilise `192.168.1.20`, les deux joueurs ouvrent `http://192.168.1.20:8080`. Le pare-feu du PC doit autoriser l’accès au port 8080 sur ce réseau privé.

Depuis la racine du dépôt, construis le frontend puis le JAR qui l’embarque.

```powershell
cd frontend
npm.cmd ci
npm.cmd run build
cd ..
mvn clean verify
java -jar target/ludo-java-0.1.0-SNAPSHOT.jar
```

Le serveur expose l’interface et les API sur le même port. Pour essayer sur un seul PC, ouvre [le jeu](http://localhost:8080) dans une fenêtre normale et ouvre l’invitation dans une fenêtre de navigation privée.



## Développer sur PC

Il faut Java 17 ou supérieur, Maven 3.6.3 ou supérieur et Node.js 20.19+ (branche 20) ou 22.12+.

Depuis la racine du dépôt, lance le serveur dans un premier terminal.

```powershell
mvn spring-boot:run
```

Dans un deuxième terminal, lance le frontend.

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

Ouvre [le jeu](http://localhost:5173), choisis le nombre de joueurs et clique sur **Nouvelle partie**. Les commandes `npm.cmd` fonctionnent sous Windows PowerShell. Sur macOS ou Linux, utilise `npm`.

Si le port 8080 est occupé, lance le serveur avec `mvn spring-boot:run '-Dspring-boot.run.arguments=--server.port=8081'` et définis `$env:LUDO_API_URL='http://localhost:8081'` dans le terminal du frontend avant de le lancer.

## Règles de cette version

- Un 6 permet de sortir un pion de sa base ou d'avancer un pion en jeu. Il donne un nouveau lancer, même si aucun déplacement n'est possible.
- Sur les cases étoilées, les couleurs peuvent cohabiter. Ailleurs, un pion qui arrive renvoie tous les pions adverses présents à leur base. Les pions d'une même couleur peuvent partager une case.
- Il faut le nombre exact pour atteindre l'arrivée. Le premier joueur à faire rentrer ses quatre pions gagne.

La partie se joue dans un seul onglet. Un rechargement de la page ramène à l'écran de démarrage. Le serveur conserve les parties en mémoire jusqu'à son redémarrage.

## Vérifier le projet

```powershell
mvn verify
cd frontend
npm.cmd ci
npm.cmd test
npm.cmd run build
```

Maven produit le JAR dans `target/` et Vite produit le frontend dans `frontend/dist/`. Pour prévisualiser ce dernier, démarre le serveur Spring Boot et lance `npm.cmd run preview` depuis `frontend/`.

## Organisation

- `src/main/java/.../domain` contient les règles de jeu en Java.
- `src/main/java/.../application` gère les sessions et applique les actions atomiquement.
- `src/main/java/.../web` expose l'API REST.
- `frontend/` contient le plateau React, sa présentation et les tests de coordonnées.

| Action | Requête | Corps JSON |
| --- | --- | --- |
| Créer | `POST /api/games` | `{"players":2}` |
| Lire | `GET /api/games/{id}` | |
| Lancer | `POST /api/games/{id}/roll` | `{"revision":0}` |
| Déplacer | `POST /api/games/{id}/move` | `{"token":0,"revision":1}` |

Le serveur renvoie l'identifiant et l'état de la partie. Utilise `state.revision` de la dernière réponse pour envoyer une action. Une action incompatible avec l'état courant renvoie un statut 409. Les pions sont numérotés de 0 à 3 dans l'API.
