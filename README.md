# Tessvine pour Android

Application Android native pour [Tessvine](https://github.com/Tessman404/cave-a-vin), la cave à vin de Home Assistant.
Elle parle directement à l'API de Tessvine (port 8099).

## Fonctions

- **Accueil** : bouteilles, valeur, vins à boire vite, places libres de la cave électrique, derniers mouvements
- **Mes vins** : recherche, filtres par couleur et par garde, fiche complète, favori, suppression
- **Déguster / sortir** : quantité, raison, note sur 5, commentaire, **annulation**
- **Sortie par code-barres** : lecteur de Google Play Services (aucune autorisation caméra)
- **Ajout d'un vin** : photo de l'étiquette (+ verso) lue par l'IA de Home Assistant, détourage sur fond, scan du code-barres, saisie manuelle
- **Plan de cave** : casier et étagères, rangs devant / derrière, quinconce, couleurs par vin ou par garde, recherche (« B4 »), localisation, ranger et déplacer
- **Mets & Vins** : tuiles de plats et recherche libre par l'IA (patch « recherche de plat »)
- **Envies, statistiques, historique** (avec annulation d'une sortie récente)
- **Réglages** : estimation de la garde par l'IA (toute la cave ou les vins manquants)

## Connexion

Dans l'appli : adresse **maison** (`http://192.168.1.37:8099`, ou l'adresse VPN) et adresse **extérieur** (https, facultative),
plus la clé d'API (`CAVE_API_KEY`). L'appli prend la première qui répond.

## Installer

Chaque envoi sur `main` compile l'APK (GitHub Actions) et le publie dans **Releases**.
Sur le téléphone : ouvrir la dernière version, télécharger `Tessvine-N.apk`, l'ouvrir et autoriser l'installation.
Les versions suivantes s'installent par-dessus (même clé de signature).
