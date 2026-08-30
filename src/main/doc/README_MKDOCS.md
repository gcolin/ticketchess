# MkDocs pour la documentation

Ce fichier décrit comment **générer** le site MkDocs. Les lecteurs (joueurs, organisateurs) n'ont pas besoin de suivre ces étapes — consultez [la page d'accueil](content/index.md).

## Prérequis

- Python 3
- pip

## Installation

```bash
pip install mkdocs
```

## Lancer en local

Depuis `src/main/doc` :

```bash
mkdocs serve
```

Ouvrez l'URL affichée dans votre navigateur.

## Générer le site statique

Depuis `src/main/doc` :

```bash
mkdocs build --strict
```

Le site est généré dans `target/mkdocs-site/` à la racine du dépôt (répertoire ignoré par Git).

## Diagrammes Mermaid

Les pages `MANUEL_UTILISATEUR.md` et `SYNCHRO_SHARLYCHESS.md` incluent des diagrammes Mermaid, chargés depuis unpkg lors du rendu MkDocs. Une connexion à Internet est nécessaire pour les visualiser en local.
