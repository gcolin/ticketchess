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

Définissez `testmode=true` pour le développement local sans OAuth. Ne versionnez jamais `params.properties`.

## Processus de contribution

1. Créez un fork du dépôt, puis une branche à partir de `master`.
2. Apportez vos modifications.
3. Lancez les tests : `mvn clean verify -P integration`
4. Mettez à jour la documentation si le changement affecte les utilisateurs ou les organisateurs (`src/main/doc/`).
5. Ouvrez une demande d'intégration (*pull request*) avec une description claire du changement et de son contexte.

## Tests

| Commande | Description |
|----------|-------------|
| `mvn test` | Tests unitaires (sans intégration) |
| `mvn test -P integration` | Suite complète incluant Playwright |
| `mvn clean verify -P integration` | Équivalent à la chaîne d'intégration continue |

Les tests d'intégration nécessitent Playwright (installé automatiquement par Maven lors du premier lancement).

Documentation MkDocs (depuis `src/main/doc`) :

```bash
pip install mkdocs
mkdocs build --strict
```

## Conventions de code

- Respecter le style existant (packages `com.github.gcolin.*`).
- Préférer des changements ciblés — une demande d'intégration par sujet fonctionnel.
- Les messages de commit doivent être explicites (éviter « fix », « wip », etc.).

## Architecture (aperçu)

- **API REST** : Jersey (`@Path`, `@Inject` au moyen de HK2 et de `JerseyDiFeature`)
- **Templates** : JTE (`src/main/jte/`)
- **Persistance** : JPA / Hibernate, `RequestContext` par requête
- **Configuration** : `params.properties` chargé par `Config`, avec surcharge possible grâce à `CONFIG_DIR`

## Sécurité

Si vous découvrez une vulnérabilité, **n'ouvrez pas** de ticket public. Consultez [SECURITY.md](SECURITY.md).

## Code de conduite

Ce projet adhère au [Contributor Covenant](CODE_OF_CONDUCT.md). En participant, vous vous engagez à respecter ce code.

## Licence

En contribuant, vous acceptez que vos contributions soient publiées sous la licence [AGPL-3.0](LICENSE).
