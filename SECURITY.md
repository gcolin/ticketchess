# Politique de sécurité

## Signaler une vulnérabilité

**N'ouvrez pas** de ticket public pour signaler un problème de sécurité.

### Option recommandée : GitHub Security Advisories

Sur le dépôt GitHub, utilisez l'onglet **Security → Advisories → Report a vulnerability** pour effectuer un signalement privé et responsable.

Si les Security Advisories ne sont pas activées sur le fork ou le miroir, contactez le mainteneur du dépôt par message privé GitHub.

Nous nous efforçons de répondre dans un délai de **72 heures** et de proposer un correctif ou un plan d'action dans un délai de **30 jours**, selon la gravité.

Merci d'inclure dans votre rapport :

- Description du problème et impact potentiel
- Étapes pour reproduire
- Version ou commit concerné
- Correctif suggéré, si vous en avez un

## Périmètre

Sont concernés notamment :

- Authentification et autorisation (OAuth, JWT, sessions)
- Injection SQL / XSS / CSRF
- Exposition de secrets ou de données personnelles
- Contournement des paiements Stripe
- Élévation de privilèges (usurpation d'identité, accès administrateur)

Les problèmes liés à une mauvaise configuration en production (`testmode=true` exposé, OAuth absent, `jwt.key` par défaut) relèvent de la documentation — voir le [README](README.md). L'application refuse le démarrage si `testmode=false` sans OAuth et clé JWT sécurisée.

## Bonnes pratiques de déploiement

- Ne versionnez jamais `params.properties` — utilisez `params.properties.example`
- En production : `testmode=false`, `CONFIG_DIR=/opt/ticketchess/config` avec un volume monté hors du dépôt Git
- Configurez **OAuth** et une **clé JWT** unique (≥ 32 caractères)
- Définissez **`source.url`** vers le dépôt public (conformité à l'AGPL)
- Secrets de CI/CD : variables GitHub Actions (masquées et protégées), jamais dans le code

## Fichiers sensibles

Ne doivent **jamais** être versionnés :

- `params.properties` / `params_prod.properties` (OAuth, Stripe, JWT, SMTP, base de données)
- `db.sql` (sauvegarde PostgreSQL)
- `rib.pdf`, logos, scripts de déploiement locaux

## Historique Git

Si des secrets ont été versionnés par le passé, régénérez-les **avant** toute publication publique :

- Google OAuth : secret client et mot de passe d'application
- Stripe : clés test et live
- Keycloak : secret client
- JWT : `jwt.key`
- Mot de passe de messagerie SMTP

Pour purger l'historique :

```bash
pip install git-filter-repo
git filter-repo --path params_prod.properties --path params.properties --path db.sql --invert-paths
git push --force origin master
```

Demandez ensuite aux contributeurs de cloner de nouveau le dépôt.

## Rotation des secrets (migration depuis un dépôt privé)

Si le projet a déjà tourné sur un serveur privé (GitLab local, instance de production, etc.), **considérez tous les secrets comme compromis** avant la publication publique, même si l'historique Git actuel est propre.

### Liste de contrôle pour la rotation

| Secret | Où le régénérer | Où le mettre à jour |
|--------|-----------------|---------------------|
| Secret client Google OAuth | [Google Cloud Console](https://console.cloud.google.com/) | `params.properties`, fournisseur OAuth |
| Clés Stripe de test et de production | [Tableau de bord Stripe](https://dashboard.stripe.com/apikeys) | `params.properties` |
| Secret client Keycloak | Console Keycloak du domaine (*realm*) | `params.properties` |
| JWT `jwt.key` | Générer une chaîne aléatoire ≥ 32 caractères | `params.properties` — invalide les sessions existantes |
| SMTP `mail.PASSWORD` | Fournisseur de messagerie | `params.properties` |
| Mot de passe PostgreSQL | Serveur de base de données | `params.properties` + `ALTER USER` |
| Webhooks Stripe | Tableau de bord Stripe → Webhooks | URL et secret de signature, le cas échéant |

### Générer une nouvelle clé JWT

```bash
# Exemple (Linux/macOS)
openssl rand -hex 32
```

Collez le résultat dans `jwt.key` de `params.properties` (production).

### Après rotation

1. Redéployer avec le nouveau `params.properties` au moyen de `CONFIG_DIR`
2. Vérifier la connexion OAuth et un paiement test Stripe
3. Révoquer les anciennes clés chez chaque fournisseur
4. Si des secrets ont été enregistrés dans le dépôt Git : purger l'historique (section ci-dessus)

## Versions supportées

Seule la branche `master` (dernière version publiée) reçoit des correctifs de sécurité.
