// Rapport de mi-parcours — Projet Traveling
// HAI811I · Programmation Mobile Android · 2025-2026

#set page(paper: "a4", margin: (top: 2.5cm, bottom: 2.5cm, left: 2cm, right: 2cm))
#set text(font: "New Computer Modern", size: 11pt, lang: "fr")
#set heading(numbering: "1.1.")
#set par(justify: true, leading: 0.65em)

// Page de garde
#align(center)[
  #v(4cm)
  #text(24pt, weight: "bold")[Projet Mobile - Traveling]
  #v(8pt)
  #text(13pt)[Rapport de mi-parcours]
  #v(1cm)
  #text(11pt)[HAI811I — Programmation Mobile Android]
  #v(4pt)
  #text(11pt)[Année universitaire 2025–2026]
  #v(2cm)
  #text(12pt)[Groupe34 - iOS - ZHANG Yaqi · HUANG Lei]
  #v(1cm)
  #text(11pt)[Université de Montpellier — Faculté des Sciences]
  #v(4pt)
  #text(11pt)[21 Avril 2026]
]
#pagebreak()

// ─────────────────────────────────────
= Choix techniques

L'application est développée en *Kotlin* avec *Jetpack Compose* et *Material 3* pour l'interface. Nous avons adopté une architecture *MVVM* avec un découpage par feature. La structure du projet est la suivante :

```
com.example.traveling/
├── MainActivity.kt                  (point d'entrée, NavHost)
├── core/
│   ├── navigation/AppNavigation.kt  (BottomBar, navigation principale)
│   └── utils/util.kt
├── data/
│   ├── model/
│   │   ├── Firestore.kt             (Destination, Attraction)
│   │   ├── Route.kt                 (TravelRoute, RouteStop)
│   │   └── User.kt
│   └── repository/
│       ├── AuthRepository.kt
│       ├── TravelRepository.kt      (accès Firestore temps réel)
│       └── FirestoreSeeder.kt       (données initiales)
├── features/
│   ├── passerelle/                   (LaunchScreen, Login, Register, ForgotPassword)
│   ├── travelshare/                  (Gallery, Explore, Publish, Notifications, Groups)
│   ├── travelpath/                   (Preferences, Results, RouteDetail, ViewModel)
│   └── profile/                      (ProfileScreen)
└── ui/theme/                         (Color.kt, Theme.kt, Type.kt)
```

- *Navigation* : Navigation Compose (`NavHost`) avec une barre inférieure à 4 onglets
- *Gestion d'état* : `ViewModel` + `StateFlow` / `MutableStateFlow`
- *Backend* : Firebase Authentication, Storage et Firestore
- *Cartes* : Google Maps Compose
- *Chargement d'images* : Coil (async)

// ─────────────────────────────────────
= Travail réalisé

== Passerelle — Partie commune

*Authentification* : Les écrans de connexion, d'inscription et de réinitialisation de mot de passe sont implémentés et connectés à Firebase Auth. L'inscription met à jour le `displayName` de l'utilisateur.

*Navigation* : Le `NavHost` dans `MainActivity` détecte automatiquement l'état d'authentification et redirige vers l'écran d'accueil ou le dashboard. Chaque écran reçoit un paramètre `isAnonymous` qui masque les actions réservées aux utilisateurs connectés (publication, like, groupes...).

*Modèles de données* : Les modèles `Destination` et `Attraction` sont définis et partagés entre les deux modules. Le `TravelRepository` expose des `Flow` temps réel depuis Firestore. Un `FirestoreSeeder` permet d'initialiser la base avec 10 villes (Chine + France) et environ 70 attractions.

== TravelShare — Partage de photos

- *GalleryScreen* : page statique proposant 3 modes de vue (liste, grille, carte Google Maps). Les données affichées sont pour l'instant des données fictives codées en dur. Les interactions (like, sauvegarde) sont uniquement visuelles, sans persistance backend.

- *MapGalleryView* : intégration de Google Maps avec des marqueurs personnalisés et un bottom sheet de détail. Fonctionne avec des données en dur.

- *ExploreScreen* : interface de recherche et filtrage (type, période, rayon). La section « destinations populaires » est connectée à Firestore via `ExploreViewModel`. Les filtres et la recherche ne sont pas encore fonctionnels côté logique.

- *PublishPhotosScreen* : formulaire de publication (sélection photo, titre, description, liaison avec un itinéraire TravelPath). La page est entièrement statique : aucune donnée n'est envoyée à Firestore.

- *NotificationsScreen* : interface avec onglets de filtrage et actions contextuelles. Données fictives uniquement.

- *GroupsScreen* : liste des groupes et découverte avec bottom sheet de création. Données fictives, pas de persistance.

== TravelPath — Génération d'itinéraires

- *PreferencesForm* : formulaire de préférences complet (destination avec validation Firestore, 8 catégories d'intérêts, lieux favoris avec suggestions provenant de Firestore, sliders budget/durée/effort). L'état du formulaire est persisté dans le `TravelViewModel`.

- *ResultsScreen* : affiche 3 itinéraires générés (Économique, Équilibrée, Premium) sous forme de cartes avec statistiques et highlights. Les itinéraires sont réellement générés à partir des données Firestore.

- *RouteDetailScreen* : détail complet d'un itinéraire avec image héros, statistiques, mini-carte Canvas (tracé GPS calculé via la formule de Haversine), et timeline d'arrêts regroupés par créneau horaire avec détails expansibles.

- *TravelViewModel* : contient la logique métier — filtrage multi-niveaux des attractions selon les préférences utilisateur, génération de 3 routes avec stratégies de sélection distinctes (coût, ratio qualité/prix, note), et scheduling temporel avec calcul de distances.

- *Limites actuelles* : les itinéraires générés ne sont pas sauvegardés. L'export PDF et le mode hors ligne ne sont pas implémentés.

// ─────────────────────────────────────
= Prochaines étapes

== TravelShare

- Remplacer les données fictives de la galerie par des données Firestore et implémenter le chargement temps réel des photos publiées par les utilisateurs
- Connecter le formulaire de publication à Firebase Storage (upload photo) et Firestore (métadonnées)
- Implémenter la logique de like, sauvegarde et commentaire avec persistance
- Rendre les filtres de l'écran Explore fonctionnels (recherche textuelle, filtrage par type/rayon)
- Implémenter la gestion des groupes : création, rejoindre, quitter, avec persistance Firestore
- Ajouter un vrai sélecteur de position sur carte (map picker) pour la publication

== TravelPath

- Sauvegarder les itinéraires générés dans le profil utilisateur (Firestore)
- Implémenter l'export PDF d'un itinéraire
- Intégrer une vraie carte Google Maps dans le détail d'itinéraire (remplacer la mini-carte Canvas)
- Ajouter le mode hors ligne (cache local avec Room)

== Passerelle

- Implémenter le lien fonctionnel entre TravelShare et TravelPath : permettre la publication de photos associées à une étape d'itinéraire
- Affiner la gestion du profil utilisateur
