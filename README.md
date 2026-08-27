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

Pour le développement local sans OAuth, activez le mode test dans `params.properties` :

```properties
testmode=true
```

Puis lancez :

```bash
mvn exec:java
```

L'application est accessible sur [http://localhost:8080](http://localhost:8080).

Sans OAuth, la connexion passe par `/auth-sim` — **uniquement si `testmode=true`** (voir [Sécurité](#sécurité)).

## Build et tests

```bash
# Tests unitaires (sans intégration)
mvn test

# Tests complets (y compris intégration Playwright)
mvn test -P integration

# Pipeline CI locale (équivalent GitHub Actions)
mvn clean verify -P integration

# Formatage du code
mvn spotless:apply

# Couverture de code
mvn clean test -P jacoco
```

### Packaging

```bash
# JAR auto-exécutable (Jetty embarqué)
mvn package -P pack
java -jar target/ticket-chess-1.0.0-fat.jar

# WAR pour Tomcat 11
mvn package -P war

# Les deux artifacts
mvn package -P pack,war
```

Voir [MANUAL.md](MANUAL.md) pour le détail des commandes Maven.

## Configuration

1. Copier le modèle :

   ```bash
   cp params.properties.example params.properties
   ```

2. Éditer `params.properties` : organisation, emails, Stripe, OAuth, base de données, **URL du code source** (`source.url`).

3. En production, placer la configuration hors du dépôt :

   ```bash
   export CONFIG_DIR=/opt/ticketchess/config
   ```

   Ce répertoire peut contenir `params.properties`, `logo.png`, `rib.pdf`, `db.sql`.

Les fichiers `params.properties`, `params_prod.properties` et `src/docker/db.sql` ne doivent **jamais** être commités.

## Déploiement en production

1. **`testmode=false`** (valeur par défaut) — l'application refuse de démarrer sans OAuth et `jwt.key` personnalisé.
2. **Configurer OAuth** (Keycloak, Google, etc.).
3. **Définir une clé JWT** forte (`jwt.key`, au moins 32 caractères, hors placeholders).
4. **Configurer `source.url`** vers le dépôt public (conformité AGPL).
5. **Monter `CONFIG_DIR`** avec `params.properties` et les fichiers statiques (logo, RIB).
6. Choisir le mode de déploiement :
   - **JAR** : `java -jar ticket-chess-1.0.0-fat.jar` (port via `PORT`, défaut `8080`)
   - **WAR** : Tomcat 11 + Java 25, déployer dans `webapps/`
   - **PostgreSQL** : décommenter `db.host`, `db.name`, `db.user`, `db.pass`, `db.type=postgres`

Pour PostgreSQL en local (développement) :

```bash
cd src/docker
docker compose up -d
```

## Sécurité

- Ne jamais committer de secrets — utiliser `params.properties.example` comme modèle.
- **`testmode=true`** : active `/auth-sim` (connexion admin sans OAuth) — développement local uniquement.
- **`testmode=false`** : OAuth + `jwt.key` obligatoires ; l'application refuse le démarrage sinon.
- Régénérer tous les secrets avant toute exposition publique du dépôt — voir [SECURITY.md](SECURITY.md#rotation-des-secrets-migration-depuis-un-dépôt-privé).
- Signaler une vulnérabilité : [SECURITY.md](SECURITY.md).

## Documentation

- Manuel des commandes : [MANUAL.md](MANUAL.md)
- Documentation utilisateur : [`src/main/doc/`](src/main/doc/)
- Contribution : [CONTRIBUTING.md](CONTRIBUTING.md)

## Licence

Ce projet est distribué sous licence [GNU Affero General Public License v3.0](LICENSE) (AGPL-3.0).

En résumé :

- Vous pouvez utiliser, modifier et redistribuer le logiciel.
- Toute version modifiée mise à disposition en réseau (SaaS) doit être accompagnée du code source correspondant.
- Les œuvres dérivées doivent rester sous la même licence.

Voir le fichier [LICENSE](LICENSE) pour le texte complet.
