# Ticket Chess

Application web de gestion d'événements et d'inscriptions pour clubs d'échecs.

## Prérequis

- **Java 25** (Temurin recommandé)
- **Maven 3.9+**

## Démarrage rapide (développement)

```bash
git clone https://github.com/gcolin/ticketchess.git
cd ticketchess
cp params.properties.example params.properties
```

Sous Windows (PowerShell) :

```powershell
Copy-Item params.properties.example params.properties
```

Pour le développement local sans OAuth, activez le mode test dans `params.properties` :

```properties
testmode=true
```

Compilez les sources, puis lancez l'application avec Jetty embarqué :

```bash
mvn compile exec:java
```

L'application est accessible sur [http://localhost:8080](http://localhost:8080).
La phase `compile` reconstruit les classes Java et les templates JTE avant que
`exec:java` démarre `com.github.gcolin.app.Main`. Relancez cette commande après
une modification du code.

Sans OAuth, la connexion passe par `/auth-sim` — **uniquement si `testmode=true`** (voir [Sécurité](#sécurité)).

## Compilation et tests

```bash
# Tests unitaires (sans intégration)
mvn test

# Tests complets (y compris intégration Playwright)
mvn test -P integration

# Chaîne d'intégration continue locale (équivalent à GitHub Actions)
mvn clean verify -P integration

# Couverture de code
mvn clean test -P jacoco
```

### Création des paquets

```bash
# JAR auto-exécutable (Jetty embarqué)
mvn package -P pack
java -jar target/ticket-chess-*-fat.jar

# WAR pour Tomcat 11
mvn package -P war

# Les deux formats de paquet
mvn package -P pack,war
```

La version est celle du `pom.xml` (p. ex. `1.0.0`). Voir [MANUAL.md](MANUAL.md) pour le détail des commandes Maven.

## Configuration

1. Copier le modèle :

   ```bash
   cp params.properties.example params.properties
   ```

   Sous Windows : `Copy-Item params.properties.example params.properties`

2. Éditer `params.properties` : organisation, adresses électroniques, Stripe, OAuth, base de données et **URL du code source** (`source.url`).

3. En production, placer la configuration hors du dépôt :

   ```bash
   export CONFIG_DIR=/opt/ticketchess/config
   ```

   Ce répertoire peut contenir `params.properties`, `logo.png`, `rib.pdf`, `db.sql`.

Les fichiers `params.properties`, `params_prod.properties` et `db.sql` ne doivent **jamais** être versionnés.

## Déploiement en production

1. **`testmode=false`** (valeur par défaut) — l'application refuse de démarrer sans OAuth et `jwt.key` personnalisé.
2. **Configurer OAuth** (Keycloak, Google, etc.).
3. **Définir une clé JWT** forte (`jwt.key`, au moins 32 caractères, différente des valeurs d'exemple).
4. **Configurer `source.url`** vers le dépôt public (conformité à l'AGPL).
5. **Configurer `CONFIG_DIR`** pour donner accès à `params.properties` et aux fichiers statiques (logo, RIB).
6. Choisir le mode de déploiement :
   - **JAR** : `java -jar target/ticket-chess-*-fat.jar` (port configurable avec `PORT`, valeur par défaut : `8080`)
   - **WAR** : Tomcat 11 + Java 25, déployer dans `webapps/`
   - **PostgreSQL** : décommenter `db.host`, `db.name`, `db.user`, `db.pass`, `db.type=postgres`

### PostgreSQL en local (développement)

Un fichier Docker Compose est fourni pour une base de test :

```bash
cd src/docker
docker compose up -d
```

Puis dans `params.properties` :

```properties
db.host=localhost:5432
db.name=ticketchess
db.user=ticket_user
db.pass=ticket_pass
db.type=postgres
```

Voir [src/docker/docker-compose.yml](src/docker/docker-compose.yml).

Alternative sans Docker : charger une sauvegarde dans H2 (voir [MANUAL.md](MANUAL.md#5-base-h2-avec-une-sauvegarde-postgresql)).

## Sécurité

- Ne jamais versionner de secrets — utiliser `params.properties.example` comme modèle.
- **`testmode=true`** : active `/auth-sim` (connexion administrateur sans OAuth) — développement local uniquement.
- **`testmode=false`** : OAuth + `jwt.key` obligatoires ; l'application refuse le démarrage sinon.
- Régénérer tous les secrets avant toute exposition publique du dépôt — voir [SECURITY.md](SECURITY.md#rotation-des-secrets-migration-depuis-un-dépôt-privé).
- Signaler une vulnérabilité : [SECURITY.md](SECURITY.md).

## Documentation

- **Site en ligne** : [gcolin.github.io/ticketchess](https://gcolin.github.io/ticketchess/) (MkDocs, publié sur GitHub Pages à chaque push sur `master`)
- Manuel des commandes : [MANUAL.md](MANUAL.md)
- Sources MkDocs : [`src/main/doc/`](src/main/doc/) (MkDocs)
- Synchronisation SharlyChess : [`src/main/doc/content/SYNCHRO_SHARLYCHESS.md`](src/main/doc/content/SYNCHRO_SHARLYCHESS.md)
- Contribution : [CONTRIBUTING.md](CONTRIBUTING.md)
- Code de conduite : [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md)

## Licence

Ce projet est distribué sous licence [GNU Affero General Public License v3.0](LICENSE) (AGPL-3.0).

En résumé :

- Vous pouvez utiliser, modifier et redistribuer le logiciel.
- Toute version modifiée mise à disposition en réseau (SaaS) doit être accompagnée du code source correspondant.
- Les œuvres dérivées doivent rester sous la même licence.

Voir le fichier [LICENSE](LICENSE) pour le texte complet.
