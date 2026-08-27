# Contribuer à Ticket Chess

Merci de votre intérêt pour Ticket Chess ! Ce guide décrit comment proposer des améliorations.

## Prérequis

- Java 25
- Maven 3.9+
- Git

## Configuration locale

```bash
cp params.properties.example params.properties
```

Définir `testmode=true` pour le développement local sans OAuth. Ne committez jamais `params.properties`.

## Workflow

1. Forkez le dépôt et créez une branche depuis `main` ou `develop`.
2. Apportez vos modifications.
3. Formatez le code : `mvn spotless:apply`
4. Lancez les tests : `mvn clean verify -P integration`
5. Ouvrez une pull request avec une description claire du changement et du contexte.

## Tests

| Commande | Description |
|----------|-------------|
| `mvn test` | Tests unitaires (sans intégration) |
| `mvn test -P integration` | Suite complète incluant Playwright |
| `mvn clean verify -P integration` | Équivalent pipeline CI |

Les tests d'intégration nécessitent Playwright (installé automatiquement par Maven lors du premier lancement).

## Conventions de code

- Respecter le style existant (packages `com.github.gcolin.*`).
- Appliquer Spotless avant chaque commit : `mvn spotless:apply`
- Préférer des changements ciblés — une PR par sujet fonctionnel.
- Les messages de commit doivent être explicites (éviter « fix », « wip », etc.).

## Architecture (aperçu)

- **API REST** : Jersey (`@Path`, `@Inject` via HK2 / `JerseyDiFeature`)
- **Templates** : JTE (`src/main/jte/`)
- **Persistance** : JPA / Hibernate, `RequestContext` par requête
- **Config** : `params.properties` via `Config`, surchargeable avec `CONFIG_DIR`

## Sécurité

Si vous découvrez une vulnérabilité, **ne pas** ouvrir d'issue publique. Consultez [SECURITY.md](SECURITY.md).

## Licence

En contribuant, vous acceptez que vos contributions soient publiées sous la licence [AGPL-3.0](LICENSE).
