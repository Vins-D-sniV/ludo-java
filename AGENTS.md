# Ludo Java

- Vérifier le backend avec `mvn verify` à la racine et le frontend avec `npm ci`, `npm test` et `npm run build` dans `frontend/`. Sous Windows PowerShell, `npm.cmd` convient également.
- Garder les règles du jeu dans `domain/Game.java`. Le backend décide du résultat des dés, des déplacements autorisés et du vainqueur. L'interface se sert des déplacements autorisés renvoyés par l'API.
- Utiliser la révision de la dernière réponse pour chaque action. Garder la lecture, la mutation et la copie de l'état sous le verrou de la partie.
- En cas de modification du parcours, mettre à jour ensemble les positions du moteur, `frontend/src/board.js` et leurs tests.
- Conserver le fichier `frontend/package-lock.json`. Exclure `target/`, `node_modules/` et `dist/` des commits.

- Garder les règles communes aux modes local et en ligne dans le même moteur. Vérifier côté serveur la place du joueur et le tour avant toute action en ligne. Les liens d’invitation contiennent uniquement l’identifiant de la partie.
- Pour tester le mode en ligne, utiliser deux contextes de navigateur indépendants. Construire le frontend avant Maven pour produire le JAR qui sert aussi l’interface.
