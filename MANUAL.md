# Ticket Chess — Manuel des commandes

Ce document décrit les commandes principales pour gérer le projet Ticket Chess. Pour l'installation, la configuration et le déploiement, voir aussi le [README](README.md).

## Prérequis

- **Java 25** (Temurin recommandé)
- **Maven 3.9+**

## Configuration locale

Copiez le modèle de configuration :

```bash
cp params.properties.example params.properties
```

Sous Windows (PowerShell) :

```powershell
Copy-Item params.properties.example params.properties
```

Pour le développement sans OAuth, définissez `testmode=true` dans `params.properties`. Ne versionnez jamais `params.properties`.

En production, placez la configuration hors du dépôt :

```bash
export CONFIG_DIR=/opt/ticketchess/config
```

Sous Windows (PowerShell) :

```powershell
$env:CONFIG_DIR = "C:\chemin\vers\config"
```

## 1. Tests

### Tests unitaires (sans intégration)

```bash
mvn test
```

### Suite complète (intégration Playwright)

```bash
mvn test -P integration
```

Équivalent à la chaîne d'intégration continue :

```bash
mvn clean verify -P integration
```

## 2. Exécution locale (développement)

Depuis la racine du dépôt, compilez les sources, puis lancez l'application
avec Jetty embarqué :

```bash
mvn compile exec:java
```

La phase `compile` reconstruit les classes Java et les templates JTE.
`exec:java` démarre ensuite la classe principale
`com.github.gcolin.app.Main`. L'application est accessible sur
[http://localhost:8080](http://localhost:8080) ; le port peut être modifié
avec la variable d'environnement `PORT`.

Après une modification du code, arrêtez l'application avec `Ctrl+C`, puis
relancez la même commande.

Avec `testmode=true` et sans OAuth, la connexion de test passe par `/auth-sim`.

## 3. Création des paquets

Les fichiers produits portent la version du `pom.xml` (p. ex. `1.0.0`).

### JAR auto-exécutable (Jetty embarqué)

```bash
mvn package -P pack
```

Produit `target/ticket-chess-*-fat.jar`. Lancement :

```bash
java -jar target/ticket-chess-*-fat.jar
```

Port par défaut : `8080` (modifiable au moyen de la variable d'environnement `PORT`).

### WAR pour Tomcat 11

```bash
mvn package -P war
```

Produit `target/ticket-chess-*.war` (sans Jetty — fourni par Tomcat).

Prérequis : **Tomcat 11** + **Java 25**. Déployer dans `webapps/`.

Configurez `params.properties` hors du WAR au moyen de `CONFIG_DIR` ou du répertoire de travail du processus Tomcat.

### Les deux formats de paquet

```bash
mvn package -P pack,war
```

## 4. Qualité de code

```bash
# Couverture de code
mvn clean test -P jacoco
```

## 5. Base H2 avec une sauvegarde PostgreSQL

Vous pouvez démarrer en H2 et charger les données depuis une sauvegarde PostgreSQL locale (`db.sql`, non versionnée).

### Avec `params.properties`

```properties
db.type=h2
db.h2.loadPostgresDump=true
db.h2.postgresDumpFile=./db.sql
```

Puis :

```bash
mvn compile exec:java
```

### Avec `CONFIG_DIR`

Si `CONFIG_DIR` est défini, l'application cherchera `db.sql` dans ce dossier quand `db.h2.loadPostgresDump=true`.

## 6. PostgreSQL en local (Docker)

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

Voir [src/docker/docker-compose.yml](src/docker/docker-compose.yml) pour les valeurs par défaut.

## 7. Exemple de déploiement en production sur un Raspberry Pi

Cet exemple déploie le JAR sur `pi@<hôte-integration>` dans
`/mnt/nvme/ticketchess`. Les secrets ne figurent ni dans les commandes ni
dans le fichier Docker Compose.

### Préparer le serveur

Connectez-vous avec une clé SSH chargée dans l'agent SSH. Ne transmettez
jamais de mot de passe ou de clé privée dans la ligne de commande.

```bash
ssh pi@<hôte-integration>
sudo install -d -o pi -g pi -m 750 /mnt/nvme/ticketchess
install -d -m 750 /mnt/nvme/ticketchess/config
install -d -m 750 /mnt/nvme/ticketchess/postgres-data
exit
```

### Construire et transférer le JAR

Depuis la racine du dépôt :

```bash
mvn clean verify -P integration
mvn package -P pack -DskipTests
scp target/ticket-chess-*-fat.jar \
  pi@<hôte-integration>:/mnt/nvme/ticketchess/ticket-chess.jar
```

### Configurer la production

Créez directement le fichier de configuration sur le serveur :

```bash
ssh pi@<hôte-integration>
nano /mnt/nvme/ticketchess/config/params.properties
chmod 600 /mnt/nvme/ticketchess/config/params.properties
```

Exemple de structure, sans valeur secrète :

```properties
testmode=false
baseurl=https://echecs.example.org
source.url=https://github.com/gcolin/ticketchess

oauth.clientId=<identifiant à renseigner sur le serveur>
oauth.clientSecret=<secret à renseigner sur le serveur>
oauth.authorizationUrl=https://auth.example.org/authorize
oauth.tokenUrl=https://auth.example.org/token

jwt.key=<clé aléatoire d'au moins 32 caractères>

stripe.public=<clé publique à renseigner sur le serveur>
stripe.secret=<clé secrète à renseigner sur le serveur>
stripe.simuled=false

db.host=postgres:5432
db.name=<nom de la base>
db.user=<utilisateur de la base>
db.pass=<mot de passe à renseigner sur le serveur>
db.type=postgres
```

Le nom de la base, l'utilisateur et le mot de passe doivent correspondre aux
variables définies dans le fichier `.env`.

Remplacez les valeurs entre chevrons uniquement sur le serveur. Ne copiez
jamais le fichier de production dans le dépôt Git, un journal de terminal ou
une demande d'intégration.

### Créer le fichier d'environnement Docker Compose

Créez `/mnt/nvme/ticketchess/.env` directement sur le serveur :

```bash
ssh pi@<hôte-integration>
nano /mnt/nvme/ticketchess/.env
chmod 600 /mnt/nvme/ticketchess/.env
```

Structure attendue, sans valeur secrète :

```dotenv
POSTGRES_DB=<nom de la base>
POSTGRES_USER=<utilisateur de la base>
POSTGRES_PASSWORD=<mot de passe à renseigner sur le serveur>
```

### Créer le fichier Docker Compose

Créez `/mnt/nvme/ticketchess/docker-compose.yml` :

```yaml
services:
  app:
    image: eclipse-temurin:25-jre
    restart: unless-stopped
    working_dir: /app
    command: ["java", "-jar", "/app/ticket-chess.jar"]
    environment:
      CONFIG_DIR: /config
      PORT: "8080"
    volumes:
      - ./ticket-chess.jar:/app/ticket-chess.jar:ro
      - ./config:/config
    ports:
      - "127.0.0.1:8080:8080"
    depends_on:
      postgres:
        condition: service_healthy

  postgres:
    image: postgres:16-alpine
    restart: unless-stopped
    environment:
      POSTGRES_DB: ${POSTGRES_DB:?variable manquante}
      POSTGRES_USER: ${POSTGRES_USER:?variable manquante}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:?variable manquante}
    volumes:
      - ./postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $$POSTGRES_USER -d $$POSTGRES_DB"]
      interval: 10s
      timeout: 5s
      retries: 5
```

### Démarrer l'application

```bash
ssh pi@<hôte-integration> \
  "cd /mnt/nvme/ticketchess && \
   docker compose pull && \
   docker compose up -d && \
   docker compose ps"
```

Placez un proxy inverse HTTPS (Caddy, Nginx ou Apache) devant le port `8080`.
Le port applicatif ne doit pas être exposé directement sur Internet.

### Mettre à jour l'application

```bash
scp target/ticket-chess-*-fat.jar \
  pi@<hôte-integration>:/mnt/nvme/ticketchess/ticket-chess.jar.new
ssh pi@<hôte-integration> \
  "mv /mnt/nvme/ticketchess/ticket-chess.jar.new /mnt/nvme/ticketchess/ticket-chess.jar && \
   cd /mnt/nvme/ticketchess && \
   docker compose up -d --force-recreate app"
```

Vérifiez enfin les journaux :

```bash
ssh pi@<hôte-integration> \
  "cd /mnt/nvme/ticketchess && docker compose logs --tail=100 app"
```

## 8. Documentation MkDocs

Depuis `src/main/doc` :

```bash
pip install mkdocs
mkdocs serve
mkdocs build --strict
```

Voir [src/main/doc/README_MKDOCS.md](src/main/doc/README_MKDOCS.md).

---

## Notes

- Outil de compilation : **Maven**
- Version Java cible : **25**
- Injection de dépendances **manuelle** : `AppContext` (instance unique de l'application), `RequestContext` (par requête), `JerseyDiFeature` (HK2 pour `@Inject` dans les API Jersey)
- Profils Maven : `integration`, `pack`, `war`, `jacoco`
