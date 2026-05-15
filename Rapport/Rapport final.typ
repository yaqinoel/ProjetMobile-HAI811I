// Rapport final — Projet Traveling
// HAI811I · Programmation Mobile Android · 2025-2026
//
// Plan de rédaction conseillé :
// 1. Compléter les informations de la page de garde.
// 2. Remplir le résumé avec une description courte du travail réellement réalisé.
// 3. Détailler la répartition du travail dans le groupe.
// 4. Décrire l'architecture, les modèles Firebase et les choix techniques.
// 5. Compléter la liste des fonctionnalités implémentées, uniquement avec les fonctions réellement livrées.
// 6. Ajouter le lien Git définitif.
// 7. Relire pour vérifier que le rapport répond strictement aux exigences :
//    résumé clair, fonctionnalités implémentées, conception/réalisation, lien Git.
//
// Remarque : les captures d'écran ne sont pas nécessaires ici, car la démonstration visuelle est prévue dans la vidéo.

#set page(
  paper: "a4",
  margin: (top: 2.5cm, bottom: 2.5cm, left: 2cm, right: 2cm),
)
#set text(font: "New Computer Modern", size: 11pt, lang: "fr")
#set heading(numbering: "1.1.")
#set par(justify: true, leading: 0.65em)

// ─────────────────────────────────────
// Page de garde
// ─────────────────────────────────────

#align(center)[
  #v(3.5cm)
  #text(24pt, weight: "bold")[Projet Mobile — Traveling]
  #v(8pt)
  #text(14pt)[Rapport final]
  #v(1cm)
  #text(11pt)[HAI811I — Programmation Mobile Android]
  #v(4pt)
  #text(11pt)[Année universitaire 2025–2026]
  #v(2cm)
  #text(12pt)[Groupe 34 — ZHANG Yaqi · HUANG Lei]
  #v(1cm)
  #text(11pt)[Université de Montpellier — Faculté des Sciences]
  #v(4pt)
  #text(11pt)[Mai 2026]
  #v(2cm)
  #text(10.5pt)[Dépôt Git : #link("https://github.com/yaqinoel/ProjetMobile-HAI811I")[https://github.com/yaqinoel/ProjetMobile-HAI811I]]
]

#pagebreak()

#outline(title: [Table des matières])

#pagebreak()

// ─────────────────────────────────────
= Résumé du projet et du travail réalisé
// ─────────────────────────────────────

// Objectif :
// Présenter en une page maximum le projet, son contexte, ses deux modules principaux et le résultat final.
// Ne pas détailler toutes les fonctionnalités ici : garder les détails pour la section 5.

Le projet *Traveling* est une application mobile Android dédiée au voyage. Elle regroupe deux services principaux : *TravelShare*, qui permet aux utilisateurs de publier, rechercher et consulter des photos de voyage, et *TravelPath*, qui permet de générer des parcours de visite personnalisés à partir des préférences de l'utilisateur.

L'application intègre également une passerelle entre les deux services. Les lieux partagés dans TravelShare peuvent être utilisés comme inspirations ou comme candidats de visite dans TravelPath. Cette liaison permet de construire une application cohérente, où les contenus publiés par les voyageurs enrichissent progressivement les possibilités de génération de parcours.

// À compléter :
// - Résumer le niveau de réalisation final.
// - Mentionner les grandes familles de fonctionnalités livrées.
// - Mentionner les technologies principales : Kotlin, Jetpack Compose, Firebase, MVVM.

// ─────────────────────────────────────
= Répartition du travail
// ─────────────────────────────────────

// Objectif :
// Expliquer clairement la responsabilité de chaque membre.
// Adapter le contenu ci-dessous à la répartition réelle du groupe.

#table(
  columns: (1.2fr, 2fr, 3fr),
  inset: 6pt,
  align: horizon,
  [*Membre*], [*Partie principale*], [*Contributions*],
  [ZHANG Yaqi], [TravelShare], [
    Développement du module de partage de photos : galerie, publication, détail des photos, interactions, groupes, notifications, profil utilisateur et liaison avec TravelPath.
  ],
  [HUANG Lei], [TravelPath], [
    Développement du module de génération de parcours : formulaire de préférences, génération d'itinéraires, présentation des parcours et gestion des données de visite.
  ],
  [Travail commun], [Application globale], [
    Intégration des deux modules, navigation générale, configuration Firebase et passerelle entre TravelShare et TravelPath.
  ],
)

// ─────────────────────────────────────
= Architecture générale de l'application
// ─────────────────────────────────────

// Objectif :
// Décrire l'organisation technique de l'application et montrer que le code suit une architecture structurée.

L'application suit une architecture *MVVM* afin de séparer l'interface utilisateur, la logique de présentation et l'accès aux données. Cette organisation facilite la maintenance du code et permet de faire évoluer séparément les écrans, les ViewModels, les repositories et les modèles de données.

== Organisation en couches

- *Screens* : écrans développés avec Jetpack Compose.
- *ViewModel* : gestion des états d'écran, des actions utilisateur et de la logique de présentation.
- *Repository* : accès aux services Firebase et encapsulation des requêtes Firestore, Storage et Authentication.
- *Model* : classes Kotlin représentant les documents stockés dans Firestore.
- *Navigation* : gestion des routes entre les écrans et des onglets principaux de l'application.

== Organisation fonctionnelle

// À compléter avec la structure finale du projet si nécessaire.

```
com.example.traveling/
├── core/                 navigation, constantes, utilitaires communs
├── data/                 modèles et repositories
├── features/
│   ├── auth/             connexion, inscription, mode anonyme
│   ├── main/             écran principal et passerelle
│   ├── profile/          profil utilisateur
│   ├── travelshare/      partage et découverte de photos
│   └── travelpath/       génération de parcours
└── ui/                   couleurs, thème et composants communs
```

// ─────────────────────────────────────
= Choix techniques
// ─────────────────────────────────────

// Objectif :
// Expliquer les technologies utilisées et leur rôle concret dans le projet.

== Kotlin et Jetpack Compose

L'application est développée en *Kotlin*. L'interface utilisateur est construite avec *Jetpack Compose*.

== Firebase

Firebase est utilisé comme backend principal de l'application :

- *Firebase Authentication* pour la connexion, l'inscription et le mode anonyme.
- *Cloud Firestore* pour stocker les utilisateurs, les publications, les commentaires, les groupes, les notifications et les données TravelPath.
- *Firebase Storage* pour stocker les images et les notes vocales associées aux publications.

== Google Maps et services Android

L'application utilise plusieurs services Google et Android liés au contexte de voyage :

- *Google Maps SDK / Maps Compose* pour afficher les cartes dans l'application, notamment dans la galerie et dans l'aperçu des parcours TravelPath ;
- *Google Places API* pour la recherche et la sélection de lieux lors de la création ou de la localisation d'une publication ;
- *Google Directions API* pour calculer les distances, durées de marche et polylines entre les étapes d'un parcours. Le service `GoogleDirectionsService` interroge l'API en mode `walking`, puis fournit à TravelPath des informations plus réalistes que les estimations locales ;
- *Google Maps Intents* pour ouvrir un itinéraire vers un lieu dans une application de cartographie externe ;
- *ML Kit Image Labeling* pour aider à l'annotation automatique des photos publiées dans TravelShare.

L'application s'appuie aussi sur des API Android natives : recherche vocale, capture ou sélection d'images, enregistrement et lecture de notes vocales, partage de contenu, génération de PDF et ouverture de fichiers.

== Open-Meteo API

TravelPath utilise également le service *Open-Meteo* afin d'obtenir la météo courante d'une destination. Le service `OpenMeteoService` récupère la température et le code météo à partir des coordonnées GPS de la destination, puis transforme ces données en description lisible, icône et conseil utilisateur.

Ces informations sont utilisées à deux niveaux :

- affichage d'un résumé météo dans le détail d'un parcours ;
- adaptation de l'algorithme de génération selon les tolérances météo indiquées par l'utilisateur : éviter la pluie, éviter les fortes chaleurs ou éviter le froid. Lorsque la météo est défavorable, l'algorithme favorise les lieux intérieurs ou mixtes.

== Chargement d'images et médias

Les images distantes sont affichées avec *Coil Compose*, ce qui simplifie le chargement asynchrone dans les écrans Compose. Les médias sont stockés dans *Firebase Storage* pour les publications TravelShare et pour les images TravelPath migrées. 

// ─────────────────────────────────────
= Modèle de données Firebase
// ─────────────────────────────────────

// Objectif :
// Présenter les collections principales, les champs importants et les liens entre les données.
// Ne pas tout détailler champ par champ si ce n'est pas utile, mais expliquer les choix de conception.

== Utilisateurs

Les utilisateurs sont stockés dans la collection `users`. Chaque document contient les informations principales du profil : identifiant, nom d'utilisateur, adresse e-mail, avatar, signature, type de compte et date de création.

Des sous-collections ou documents associés permettent de gérer les interactions personnelles :

- publications aimées ;
- publications enregistrées ;
- groupes rejoints ;
- paramètres de notifications.

== Publications TravelShare

Les publications de TravelShare sont stockées dans la collection `photoPosts`. Une publication contient les images, le titre, la description, les tags, la localisation, le type de lieu, les informations de visibilité et les compteurs d'interactions.

Les commentaires, likes et enregistrements sont séparés afin de faciliter la lecture et l'écriture des interactions :

```
photoPosts/{postId}/comments
photoPosts/{postId}/likes
photoPosts/{postId}/saves
```

== Groupes

Les groupes permettent à plusieurs utilisateurs de partager des publications dans un espace commun. Les données principales sont stockées dans la collection `groups`, avec les informations du groupe, son propriétaire et ses membres.

== Notifications

Les notifications sont utilisées pour signaler les événements importants à l'utilisateur : nouvelle publication d'un auteur suivi, activité dans un groupe, interaction sur une publication, tag suivi ou type de lieu suivi.

== Données TravelPath

TravelPath utilise principalement les collections liées aux destinations, attractions et itinéraires. Les destinations peuvent provenir de données officielles ou être créées à partir de TravelShare.

Les lieux issus de TravelShare ne sont pas directement mélangés aux attractions officielles. Ils sont stockés comme contributions utilisateur, par exemple dans une collection dédiée aux attractions TravelShare, avec une indication de source.

Les modèles principaux utilisés par TravelPath sont :

- `Destination` : ville ou zone de visite, avec nom, coordonnées, description et image principale ;
- `Attraction` : lieu visitable associé à une destination, avec type, coût, durée, note, coordonnées, horaires, niveau météo, tags et images ;
- `TravelRoute` : proposition de parcours affichée à l'utilisateur, avec budget total, durée, effort réel, nombre d'arrêts, image et points forts ;
- `RouteStop` : étape détaillée du parcours, avec horaire, durée, distance depuis l'étape précédente, temps de marche, image principale, galerie d'images et polyline éventuelle ;
- `SavedRoute` : version persistée d'un parcours aimé ou enregistré par l'utilisateur.

Le champ `imageUrls` a été ajouté aux attractions et aux étapes de parcours afin de gérer plusieurs images par lieu. Cela évite d'afficher des images génériques répétées et permet de conserver une galerie spécifique pour chaque attraction.

// ─────────────────────────────────────
= Fonctionnalités implémentées
// ─────────────────────────────────────

// Objectif :
// C'est la section la plus importante du rapport.
// Elle doit lister les fonctionnalités réellement implémentées, avec assez de détails pour montrer l'étendue du travail.

== Authentification et accès à l'application

// Objectif :
// Décrire les fonctions communes d'accès avant de détailler TravelShare et TravelPath.

=== Lancement de l'application

- écran d'accueil permettant de choisir entre le mode anonyme et le mode connecté ;
- redirection vers l'espace principal après authentification ;
- maintien de l'état de session selon l'utilisateur courant.

=== Inscription

- création d'un compte utilisateur avec Firebase Authentication ;
- saisie des informations de base du profil ;
- possibilité de définir un avatar et une signature ;
- création ou initialisation des données utilisateur nécessaires dans Firestore.

=== Connexion

- connexion d'un utilisateur existant ;
- accès aux fonctionnalités réservées aux utilisateurs connectés ;
- récupération des informations de profil et des paramètres associés.

=== Mode anonyme

- accès à l'application sans création immédiate de compte ;
- utilisation d'une identité anonyme Firebase pour conserver certaines interactions ;
- possibilité de consulter les contenus publics avant de s'inscrire ou se connecter.

== TravelShare

=== Mode anonyme

// À compléter avec l'état final exact.

- consultation des publications publiques ;
- recherche et filtrage des photos ;
- consultation du détail d'une photo ;
- interaction avec les publications autorisées en mode anonyme ;
- accès au profil anonyme limité aux contenus aimés ou enregistrés ;
- possibilité de passer au mode connecté.

=== Mode connecté

- inscription et connexion ;
- publication de photos de voyage ;
- modification et suppression des publications personnelles ;
- ajout de plusieurs images ;
- ajout d'un titre, d'une description, de tags et d'une localisation ;
- ajout d'une note vocale ;
- annotation automatique assistée ;
- publication publique ou dans un groupe ;
- liaison optionnelle avec TravelPath.

=== Recherche et découverte

- recherche textuelle ;
- recherche vocale ;
- filtrage par type de lieu ;
- filtrage par période ;
- filtrage autour d'un lieu avec rayon ;
- recherche de photos similaires ;
- affichage en liste, grille et carte.

=== Détail d'une publication

- affichage des images et informations de la publication ;
- affichage du lieu, de la date, de l'auteur, des tags et de la description ;
- lecture d'une note vocale ;
- likes et retraits de likes ;
- enregistrements et retraits d'enregistrements ;
- commentaires ;
- signalement d'un contenu ;
- ouverture d'un itinéraire dans une application de cartographie ;
- accès aux photos similaires ;
- utilisation du lieu dans TravelPath.

=== Groupes

- création de groupes ;
- découverte de groupes ;
- adhésion et sortie d'un groupe ;
- consultation des publications d'un groupe ;
- publication de photos dans un groupe.

=== Notifications

- notifications liées aux auteurs suivis ;
- notifications liées aux groupes ;
- notifications liées aux interactions sur les publications ;
- notifications liées aux tags suivis ;
- notifications liées aux types de lieux suivis ;
- écran de gestion des paramètres de notifications.

=== Profil utilisateur

- affichage du profil personnel ;
- avatar et signature ;
- gestion des publications personnelles ;
- consultation des publications aimées et enregistrées ;
- consultation des auteurs suivis ;
- accès aux profils des autres utilisateurs ;
- suivi et désabonnement d'un auteur.

== TravelPath

=== Saisie des préférences

- sélection d'une destination parmi les villes disponibles dans Firestore ;
- sélection des catégories d'activités souhaitées : culture, gastronomie, nature, loisirs, shopping, vie nocturne, sport et photo ;
- ajout de lieux favoris ou obligatoires, avec suggestions d'attractions existantes et suggestions issues de TravelShare ;
- prise en compte du budget maximal et de la durée souhaitée ;
- prise en compte de l'effort physique comme tolérance de marche maximale. Le niveau choisi par l'utilisateur limite la longueur du parcours : très facile, facile, modéré, élevé ou intense ;
- prise en compte des tolérances météo : éviter la pluie, éviter les fortes chaleurs et éviter le froid.

=== Génération des parcours

- génération de plusieurs propositions de parcours pour une même destination ;
- calcul de trois stratégies principales : parcours économique, parcours équilibré et parcours premium ;
- combinaison des attractions officielles avec des lieux issus de TravelShare lorsque ceux-ci sont liés à la destination ou sélectionnés par l'utilisateur ;
- filtrage par budget, activités, lieux favoris, météo et contraintes de durée ;
- contrôle de la longueur du parcours à partir de la distance de marche estimée entre les étapes ;
- calcul automatique du niveau d'effort réel d'une route à partir de la distance totale, du nombre d'étapes, de la durée et du type de lieux visités ;
- adaptation des propositions lorsque la météo est défavorable, en favorisant les lieux intérieurs ou partiellement couverts.

=== Présentation des résultats

- affichage de plusieurs options de parcours ;
- présentation des métriques principales : budget, durée, effort et nombre d'étapes ;
- comparaison rapide des propositions générées ;
- affichage d'une carte visuelle, d'une image représentative et d'un résumé des arrêts principaux.

=== Détail d'un parcours

- consultation du détail d'un parcours ;
- affichage des étapes et des métriques du parcours ;
- présentation de l'ordre des visites par créneaux : matin, après-midi et soir ;
- affichage des informations utiles pour chaque étape : horaire d'arrivée, type d'activité, durée, coût, note, horaires d'ouverture, distance depuis l'étape précédente et temps de marche ;
- affichage d'une mini-carte du parcours ;
- récupération des distances, durées de marche et polylines avec Google Directions API lorsque les coordonnées sont disponibles ;
- affichage de plusieurs images par attraction grâce au champ `imageUrls` ;
- affichage de photos TravelShare liées aux lieux du parcours lorsque des publications correspondantes existent ;
- affichage d'un résumé météo et de conseils de visite.

=== Interactions avec les parcours

- mise en favori et enregistrement des parcours ;
- consultation des itinéraires aimés ou enregistrés depuis le profil utilisateur ;
- partage d'un parcours sous forme de texte ;
- export d'un parcours en PDF ;
- régénération avec ajustements : privilégier les lieux intérieurs, réduire le coût, réduire la marche ou générer une variante plus surprenante ;
- mode hors-ligne léger : cache des données essentielles du parcours et compression locale des médias utilisés par les étapes ;
- navigation vers une étape via une application cartographique externe.

=== Données et extensibilité

- utilisation de destinations et attractions stockées dans Firestore ;
- distinction entre données officielles et contributions issues de TravelShare grâce à un champ de source ;
- stockage des images des destinations et attractions dans Firebase Storage après migration ;
- gestion de plusieurs images par attraction afin d'éviter les images génériques ou répétées ;
- possibilité d'étendre progressivement les lieux disponibles grâce aux publications TravelShare ;
- conservation locale de parcours pour permettre une consultation partielle sans connexion.

== Passerelle entre TravelShare et TravelPath

La passerelle permet de relier les contenus publiés dans TravelShare avec la génération de parcours dans TravelPath.

=== Utiliser un lieu TravelShare dans TravelPath

- depuis le détail d'une photo TravelShare, l'utilisateur peut utiliser le lieu dans TravelPath ;
- le formulaire TravelPath peut être prérempli à partir d'une publication TravelShare.

=== Ajouter une publication TravelShare à TravelPath

- lors de la publication d'une photo, l'utilisateur peut choisir de l'ajouter à TravelPath ;
- les informations de lieu, de type d'activité, de coût, de durée et d'ouverture peuvent être associées à cette contribution ;
- la publication reste une publication TravelShare, mais devient également exploitable par TravelPath.

=== Contributions TravelShare aux destinations

- si la ville n'existe pas encore dans TravelPath, une destination issue de TravelShare peut être créée ;
- les destinations créées à partir de TravelShare sont distinguées des destinations officielles ;
- les lieux TravelShare peuvent être utilisés comme candidats de visite.

=== Photos TravelShare dans les parcours

- les parcours TravelPath peuvent afficher des photos partagées par les voyageurs ;
- les photos recommandées sont liées aux destinations, aux lieux ou aux étapes du parcours.

=== Retour de TravelPath vers TravelShare

- l'utilisateur peut revenir d'une photo liée à un parcours vers son détail TravelShare.

// ─────────────────────────────────────
= Utilisation d'IA
// ─────────────────────────────────────

L'intelligence artificielle a été utilisée comme un outil d'assistance. Pendant la phase de prototypage, nous avons utilisé le mode *Make* de Figma pour explorer des idées d'interface. Au début du projet, l'IA nous a aidés à rechercher et comparer des choix techniques, notamment le langage, l'architecture, la base de données et les services backend. Pendant le développement, elle a aussi servi d'aide ponctuelle pour résoudre des problèmes de mise en page, comprendre certaines API ou services, réfléchir à des ajustements d'architecture et diagnostiquer des erreurs ou bugs. Les choix finaux, l'intégration dans le projet et la validation des fonctionnalités sont restés réalisés par les membres du groupe.
