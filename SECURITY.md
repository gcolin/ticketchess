# Politique de sécurité

## Signaler une vulnérabilité

**Ne pas** ouvrir d'issue publique pour un problème de sécurité.

### Option recommandée : GitHub Security Advisories

Sur le dépôt GitHub, utilisez l'onglet **Security → Advisories → Report a vulnerability** (responsible disclosure privé).

Si les Security Advisories ne sont pas activées sur le fork ou le miroir, contactez le mainteneur du dépôt par message privé GitHub.

Nous nous efforçons de répondre sous **72 heures** et de proposer un correctif ou un plan d'action sous **30 jours** selon la gravité.

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
- Élévation de privilèges (impersonation, accès admin)

Les problèmes liés à une mauvaise configuration en production (`testmode=true` exposé, OAuth absent, `jwt.key` par défaut) relèvent de la documentation — voir le [README](README.md). L'application refuse le démarrage si `testmode=false` sans OAuth et clé JWT sécurisée.

## Bonnes pratiques de déploiement

- Ne committez jamais `params.properties` — utilisez `params.properties.example`
- En production : `testmode=false`, `CONFIG_DIR=/opt/ticketchess/config` avec volume monté hors git
- Configurez **OAuth** et une **clé JWT** unique (≥ 32 caractères)
- Définissez **`source.url`** vers le dépôt public (conformité AGPL)
- Secrets CI/CD : variables GitHub Actions (masked + protected), pas dans le code

## Fichiers sensibles

Ne doivent **jamais** être versionnés :

- `params.properties` / `params_prod.properties` (OAuth, Stripe, JWT, SMTP, base de données)
- `src/docker/db.sql` (dump PostgreSQL)
- `rib.pdf`, logos, scripts de déploiement locaux

## Historique git

Si des secrets ont été commités par le passé, régénérez-les **avant** toute publication publique :

- Google OAuth : client secret + mot de passe application
- Stripe : clés test et live
- Keycloak : client secret
- JWT : `jwt.key`
- Mot de passe mail SMTP

Pour purger l'historique :

```bash
pip install git-filter-repo
git filter-repo --path params_prod.properties --path params.properties --path src/docker/db.sql --invert-paths
git push --force origin main
```

Puis demandez aux contributeurs de re-cloner le dépôt.

## Rotation des secrets (migration depuis un dépôt privé)

Si le projet a déjà tourné sur un serveur privé (GitLab LAN, instance de prod, etc.), **considérez tous les secrets comme compromis** avant la publication publique, même si l'historique git actuel est propre.

### Checklist de rotation

| Secret | Où le régénérer | Où le mettre à jour |
|--------|-----------------|---------------------|
| Google OAuth client secret | [Google Cloud Console](https://console.cloud.google.com/) | `params.properties`, provider OAuth |
| Stripe clés test/live | [Stripe Dashboard](https://dashboard.stripe.com/apikeys) | `params.properties` |
| Keycloak client secret | Console Keycloak du realm | `params.properties` |
| JWT `jwt.key` | Générer une chaîne aléatoire ≥ 32 caractères | `params.properties` — invalide les sessions existantes |
| SMTP `mail.PASSWORD` | Fournisseur mail | `params.properties` |
| Mot de passe PostgreSQL | Serveur DB | `params.properties` + `ALTER USER` |
| Webhooks Stripe | Stripe Dashboard → Webhooks | URL + signing secret si utilisé |

### Générer une nouvelle clé JWT

```bash
# Exemple (Linux/macOS)
openssl rand -hex 32
```

Collez le résultat dans `jwt.key` de `params.properties` (production).

### Après rotation

1. Redéployer avec le nouveau `params.properties` via `CONFIG_DIR`
2. Vérifier la connexion OAuth et un paiement test Stripe
3. Révoquer les anciennes clés chez chaque fournisseur
4. Si des secrets ont été dans git : purger l'historique (section ci-dessus)

## Versions supportées

Seule la branche `main` (dernière version publiée) reçoit des correctifs de sécurité.
