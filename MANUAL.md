# Ticket Chess - Manuel des Commandes

Ce document décrit les commandes principales pour gérer le projet Ticket Chess.

## 1. Tests

Exécute tous les tests unitaires et d'intégration du projet.

```bash
mvn test -P integration
```

**Description:** Lance la suite complète de tests incluant les tests d'intégration (profil `integration`).

## 2. Packaging

### JAR auto-exécutable (Jetty embarqué)

```bash
mvn package -P pack
```

Produit `target/ticket-chess-1.0.0-fat.jar`. Lancement :

```bash
java -jar target/ticket-chess-1.0.0-fat.jar
```

Port par défaut : `8080` (surcharge via variable d'environnement `PORT`).

### WAR pour Tomcat 11

```bash
mvn package -P war
```

Produit `target/ticket-chess-1.0.0.war` (sans Jetty ni APIs servlet/websocket — fournies par Tomcat).

Prérequis : **Tomcat 11** + **Java 25**. Déployer dans `webapps/` (le `META-INF/context.xml` fixe le context path à la racine `/` ; pour un autre path, renommer le WAR ou ajuster le context).

Configurer `params.properties` hors WAR via `CONFIG_DIR` ou le répertoire de travail du process Tomcat.

### Les deux artifacts

```bash
mvn package -P pack,war
```

## 3. Exécution (dev)

Lance l'application directement.

```bash
mvn exec:java
```

**Description:** Exécute la classe principale `com.github.gcolin.app.Main` définie dans la configuration du plugin exec-maven-plugin.

---

## Notes Supplémentaires

- Le projet utilise **Maven** comme gestionnaire de build
- Version Java cible: **25**
- Injection de dépendances **manuelle** (plus de Weld/CDI) : `AppContext` (singleton application), `RequestContext` (par requête, EntityManager + DAOs), `JerseyDiFeature` (HK2 pour `@Inject` sur les APIs Jersey)
- Les profils Maven permettent de customiser les étapes du build
- Pour appliquer la formatage de code: `mvn spotless:apply`
- Pour générer un rapport de couverture de code: `mvn clean test -P jacoco`

## Démarrage H2 avec le backup PostgreSQL

Tu peux démarrer l'application en H2 et charger les données depuis un dump PostgreSQL local (`db.sql`, non versionné).

### Option 1: via `params.properties`

Copie d'abord le modèle :

```bash
cp params.properties.example params.properties
```

Puis ajoute ces propriétés dans `params.properties`:

```properties
db.type=h2
db.h2.loadPostgresDump=true
db.h2.postgresDumpFile=./db.sql
```

Puis lance:

```bash
mvn exec:java
```

### Option 2: via variable d'environnement

Si `CONFIG_DIR` est défini, l'application cherchera automatiquement `db.sql` dans ce dossier quand `db.h2.loadPostgresDump=true`.
