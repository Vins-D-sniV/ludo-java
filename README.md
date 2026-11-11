# Ludo Java

Jeu de Ludo en français pour 2 à 4 personnes sur le même écran. React affiche le plateau et Spring Boot gère les tours, les dés, les déplacements et la victoire.

## Démarrer sur PC

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
