# 🐇 CUNI PRO - Application de Gestion Cunicole

**CUNI PRO** est une application Android moderne, intuitive et complète conçue pour simplifier et optimiser la gestion quotidienne des élevages cunicoles (élevages de lapins). 

L'application fonctionne en mode **Offline-First** (hors-ligne), permettant aux éleveurs de suivre leur cheptel, la reproduction, l'alimentation, la santé et les finances même dans les zones sans couverture réseau, avec une synchronisation automatique dans le Cloud Firebase.

---

## 🌟 Fonctionnalités Principales

### 📊 1. Tableau de Bord Interactif
- **Vue globale du cheptel** : Nombre total d'animaux vivants, répartition par sexes (Mâles/Femelles) et taux de mortalité.
- **Ventilation par catégories** : Reproducteurs, Sevrés, Engraissement, Primipares, Multipares, Vendus.
- **Aperçu financier rapide** : Revenus, dépenses et solde net de l'élevage.
- **Gestion des stocks d'aliments** : Alerte visuelle en cas de stock faible.
- **Tâches quotidiennes & Rappels prioritaires** : Liste des actions du jour à effectuer.

### 🐇 2. Gestion du Cheptel
- Enregistrement individuel des lapins (Matricule, Sexe, Race, Date de naissance, Statut, Cage/Local).
- Génération automatique de matricules uniques.
- Fiche individuelle détaillée pour chaque animal.

### 💖 3. Suivi de la Reproduction
- Enregistrement des saillies, palpations et mises bas.
- Calcul automatique des dates clés (Date probable de mise bas, date de sevrage).
- Bilan des portées : Nés vivants, mort-nés et sevrés.

### 🌾 4. Gestion Alimentaire & Stocks
- Suivi des stocks par types d'aliments : **Granulés, Foin, Légumes, Graines, Autres**.
- Définition de seuils d'alerte configurables.
- Enregistrement des entrées et sorties de stock.
- **Statistiques de consommation sur 7 jours** avec **jauges visuelles animées et colorées**.

### 🩺 5. Santé, Soins & Finances
- Historique des soins vétérinaires, traitements et vaccins.
- Déclaration et suivi des cas de mortalité.
- Suivi des ventes de lapins et des dépenses d'exploitation avec bilan de trésorerie.

### 📋 6. Tâches & Notifications Automatiques
- Planification automatique des événements d'élevage (Mises bas, palpations, contrôles de stock).
- Notifications quotidiennes de rappel via `WorkManager`.

### ☁️ 7. Authentification & Synchronisation Cloud
- Authentification sécurisée par e-mail via **Firebase Auth**.
- Mode **Offline-First** avec base de données locale **Room**.
- Synchronisation intelligente avec **Cloud Firestore** dès que la connexion Internet est rétablie.

### 🌐 8. Support Multilingue
- Prise en charge native du **Français** et de l'**Anglais**.

---

## 🛠️ Architecture Technique & Technologies

- **Langage** : Kotlin
- **Architecture** : MVVM (Model-View-ViewModel), Pattern Repository, Navigation Graph unique (`Single Activity`).
- **Base de données locale** : AndroidX Room (Mode Hors-ligne).
- **Backend Cloud** : Firebase Authentication & Cloud Firestore.
- **UI & Design System** : Material Design 3, ViewBinding, ConstraintLayout, Custom Gauges & Canvas.
- **Navigation** : AndroidX Navigation Component.
- **Tâches en arrière-plan** : AndroidX WorkManager.
- **Asynchronisme** : Kotlin Coroutines & Flow.

---

## 📱 Captures d'écran & Aperçu Visuel

| Tableau de Bord | Gestion Alimentaire (Jauges) | Suivi de Reproduction |
| :---: | :---: | :---: |
| 🏠 *Tableau de bord complet avec résumé financier et cheptel* | 🌾 *Statistiques de consommation avec jauges colorées et animées* | 💖 *Calcul automatique des saillies et mises bas* |

---

## 🚀 Installation & Configuration

### Prérequis
- **Android Studio** : Version 2024.1 ou supérieure (Ladybug / Jellyfish / Meerkat).
- **JDK** : Java 17.
- **Android SDK** : Compile SDK 35, Minimum SDK 24 (Android 7.0+).

### Étapes d'installation

1. **Cloner le dépôt GitHub** :
   ```bash
   git clone https://github.com/VOTRE_NOM_UTILISATEUR/CUNIPRO.git
   cd CUNIPRO
   ```

2. **Ouvrir le projet dans Android Studio** :
   - Fichier -> Ouvrir -> Sélectionner le dossier `CUNIPRO`.

3. **Configuration Firebase** *(optionnel si le fichier `google-services.json` est déjà fourni)* :
   - Créez un projet sur la [Console Firebase](https://console.firebase.google.com/).
   - Ajoutez une application Android avec le package `com.example.cunipro`.
   - Téléchargez le fichier `google-services.json` et placez-le dans le répertoire `app/`.

4. **Compiler et Exécuter** :
   - Synchronisez les fichiers Gradle (`Sync Project with Gradle Files`).
   - Lancez l'application sur un émulateur Android ou un appareil physique.

---

## 📁 Structure du Projet

```text
com.example.cunipro/
├── data/
│   ├── local/              # Base de données Room (Entities, DAOs, Database)
│   └── repository/         # Repositories (Auth, Animal, Feed, Finance, Health, Task, Sync)
├── ui/
│   ├── adapter/            # Adapteurs RecyclerView
│   ├── animals/            # Gestion des lapins et fiches individuelles
│   ├── auth/               # Connexion et Inscription (Login & Register)
│   ├── dashboard/          # Tableau de bord principal
│   ├── feed/               # Alimentation, stocks et jauges de consommation
│   ├── healthfinance/      # Soins, mortalité et finances
│   ├── reproduction/        # Saillies, palpations et mises bas
│   ├── salesfinance/       # Ventes et dépenses
│   ├── settings/           # Paramètres de l'application et du profil
│   └── tasks/              # Liste des tâches et alertes
├── util/                   # Utilitaires (Calculateurs de dates, Langues, Notifications)
├── worker/                 # WorkManager pour les rappels et la synchronisation
├── CuniProApplication.kt   # Classe Application principale
└── MainActivity.kt         # Activité principale hôte de la navigation
```

---

## 📄 Licence

Ce projet est sous licence **MIT**. Vous êtes libre de l'utiliser, le modifier et le distribuer.

---

## 👨‍💻 Auteur

Développé pour la gestion professionnelle des élevages cunicoles.
