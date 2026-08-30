# Manuel organisateur — Ticket Chess

Ce guide s'adresse aux **organisateurs** et **administrateurs** d'un club utilisant Ticket Chess : créer des tournois, gérer les inscriptions, les paiements et la liaison avec la FFE ou SharlyChess.

Prérequis : disposer d'un compte doté de droits d'administration (le menu **Admin** est visible après la connexion).

## Accès à l'administration

1. Connectez-vous au site.
2. Cliquez sur **Admin** dans la barre de navigation.
3. Le tableau d'administration affiche les sections **Événements**, **Utilisateurs**, **Adhésions** et **Système** selon vos droits.

Raccourcis fréquents :

| Action | Chemin |
|--------|--------|
| Voir tous les tournois | **Admin** → **Tous les événements** |
| Gérer les paiements | **Admin** → **Paiements** |
| Tableau de bord des anomalies | **Admin** → **Tableau de bord** |
| Organisation (logo, RIB, adresses électroniques) | **Admin** → **Organisation** |
| Bases FFE et FIDE | **Admin** → **Base de données** |

---

## Structure des événements

Ticket Chess organise les tournois en trois niveaux :

1. **Groupe d'événements** — catégorie publique (p. ex. *Opens*, *Rapides*) disposant de sa propre page et de son propre filtre.
2. **Collection d'événements** — regroupe plusieurs tournois d'une même compétition (p. ex. *Open de Domloup 2026*) et peut fixer une jauge d'inscriptions commune.
3. **Événement (tournoi)** — tournoi individuel (Open A, Jeunes…).

### Créer une collection

1. **Admin** → **Tous les événements**.
2. Cliquez sur **Créer une collection d'événements**.
3. Renseignez le **nom**, éventuellement le **nombre maximal d'inscriptions** pour la série, et l'**identifiant ChessEvent (SharlyChess)** si vous synchronisez avec SharlyChess (voir [Synchronisation SharlyChess](SYNCHRO_SHARLYCHESS.md)).

### Créer un tournoi

1. **Admin** → **Tous les événements** → **Créer un événement**.
2. Renseignez le nom, les dates, les tarifs et le statut (**Brouillon** → **Inscriptions ouvertes** lorsque tout est prêt).
3. Associez le tournoi à un **groupe** ou à une **collection**, si nécessaire.

Le statut **Inscriptions ouvertes** permet aux joueurs de s'inscrire depuis la page publique du tournoi.

---

## Options d'un tournoi (FFE, SharlyChess, pointage)

Sur la page du tournoi → **Modifier** → onglet **Options** :

| Champ | Usage |
|-------|--------|
| Identifiant FFE | Numéro du tournoi fédéral (lien vers la fiche FFE) |
| Mot de passe FFE | Envoi automatique du fichier PAPI vers la FFE |
| Utilisateur ChessEvent | Identifiant arbitre pour SharlyChess (`user_id`) |
| Mot de passe ChessEvent | Mot de passe partagé avec SharlyChess (distinct du mot de passe FFE) |
| Pointage joueur | Si activé, seuls les joueurs pointés au guichet sont envoyés à SharlyChess |

Configurez l'**utilisateur et le mot de passe ChessEvent** sur au moins un tournoi de la collection pour permettre la synchronisation avec SharlyChess.

---

## Paiement & pointage (jour J)

L'écran **Paiement & pointage** (`menu ⋯` du tournoi → **Paiement & pointage**) sert aux bénévoles le jour de la compétition :

- marquer un joueur **présent** (pointage) ;
- enregistrer un paiement **sur place** (payé ou non payé) ;
- consulter l'**historique** des actions et annuler une erreur.

Si le pointage est activé dans les options du tournoi, seuls les joueurs pointés seront synchronisés vers SharlyChess.

Voir la [liste de contrôle pour les bénévoles](CHECKLIST_BENEVOLES_GUICHET.md) pour le déroulement des opérations sur place.

---

## Paiements (administration)

**Admin** → **Paiements** permet de :

- valider les virements reçus ;
- corriger un paiement ;
- exporter CSV ou PDF ;
- consulter le relevé des écarts entre les paiements et les inscriptions.

Les joueurs règlent en ligne (carte, virement) depuis **Mes tournois** ; les encaissements sur place passent par **Paiement & pointage**.

---

## Export PAPI et FFE

Depuis la page d'un tournoi :

- **⋯** → **Exporter PAPI** — fichier `.papi` pour SharlyChess ou autre logiciel ;
- **Exporter PAPI vers la FFE** — si l'identifiant et le mot de passe FFE sont renseignés dans les options.

Alternative avec SharlyChess : synchronisation automatique grâce à [ChessEvent](SYNCHRO_SHARLYCHESS.md).

---

## Adhésions et licences au club

### Adhésions

**Admin** → **Adhésions** : suivre et valider les demandes, puis envoyer des courriels.

Les joueurs s'inscrivent au moyen de **S'inscrire au club**. Cette option est visible si elle est activée dans **Admin** → **Organisation** et si les adresses électroniques de notification d'adhésion sont renseignées.

### Licences et tarifs

**Admin** → **Licences** : types de licence (A, B…) et grilles tarifaires par saison.

### Saisons

**Admin** → **Saisons** : définir la saison courante pour filtrer événements et adhésions.

---

## Base de données des joueurs (FFE et FIDE)

**Admin** → **Base de données** :

- télécharger ou recharger les index **FFE** et **FIDE** ;
- ajouter manuellement des joueurs (**Joueurs Lucene manuels**) si un licencié n'apparaît pas dans la base fédérale.

Rechargez la base FFE après une mise à jour fédérale importante.

---

## Organisation et conformité

**Admin** → **Organisation** :

- nom, adresse et adresses électroniques de contact ;
- **logo**, **RIB** (PDF), image de fond ;
- activation de **S'inscrire au club** ;
- URL du dépôt source (`source.url` en production — conformité AGPL).

En production, placez `params.properties`, le logo et le RIB dans `CONFIG_DIR` (hors du dépôt Git). Voir le `README.md` à la racine du dépôt.

---

## Statistiques et communication

- **Statistiques** — vue globale, par collection ou par tournoi (CSV, PDF).
- **Courriel** — depuis un tournoi ou une collection : envoyer un message aux inscrits (aperçu avant envoi).

---

## Voir aussi

- [Manuel utilisateur](MANUEL_UTILISATEUR.md) — parcours des joueurs et des parents
- [Synchronisation SharlyChess](SYNCHRO_SHARLYCHESS.md)
- [Liste de contrôle pour les bénévoles](CHECKLIST_BENEVOLES_GUICHET.md)
- `README.md` (racine du dépôt) — installation, déploiement, sécurité
