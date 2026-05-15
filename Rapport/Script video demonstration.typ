// Script de vidéo de démonstration — Projet Traveling
// Ce fichier sert de guide pour enregistrer la vidéo finale avec explication orale.
// Les actions indiquent ce qu'il faut montrer à l'écran.
// Les phrases en "Voix" peuvent être utilisées directement comme texte oral.

#set page(
  paper: "a4",
  margin: (top: 2.3cm, bottom: 2.3cm, left: 2cm, right: 2cm),
)
#set text(font: "New Computer Modern", size: 10.5pt, lang: "fr")
#set heading(numbering: "1.1.")
#set par(justify: true, leading: 0.62em)

#align(center)[
  #v(2cm)
  #text(22pt, weight: "bold")[Script de démonstration vidéo]
  #v(8pt)
  #text(14pt)[Projet Mobile — Traveling]
  #v(1cm)
  #text(11pt)[TravelShare · TravelPath · Passerelle]
  #v(8pt)
  #text(10.5pt)[HAI811I — Programmation Mobile Android]
  #v(4pt)
  #text(10.5pt)[Année universitaire 2025–2026]
]

#pagebreak()

= Préparation avant l'enregistrement

Avant de lancer l'enregistrement, préparer l'application dans un état stable :

- avoir au moins un compte utilisateur connecté disponible ;
- avoir quelques publications publiques déjà créées ;
- avoir au moins une publication avec plusieurs photos, des tags, une localisation, une note vocale et une liaison TravelPath ;
- avoir au moins un groupe créé avec des publications ;
- avoir quelques notifications ou interactions visibles ;
- vérifier que les données TravelPath sont disponibles : destinations, attractions et éventuellement contributions TravelShare ;
- préparer une publication de test si l'on souhaite montrer la modification ou la suppression sans toucher aux données importantes.

Durée recommandée de la vidéo : environ 10 à 14 minutes. Il vaut mieux montrer rapidement chaque fonctionnalité importante plutôt que rester trop longtemps sur un seul écran.

= Plan général de la vidéo

#table(
  columns: (1.2fr, 2.4fr, 4fr),
  inset: 6pt,
  align: horizon,
  [*Temps*], [*Partie*], [*Objectif*],
  [0:00 - 0:40], [Introduction], [Présenter l'application et les deux modules principaux.],
  [0:40 - 1:40], [Authentification], [Montrer le lancement, le mode anonyme, la connexion et l'inscription.],
  [1:40 - 4:00], [TravelShare — exploration], [Montrer la galerie, les filtres, les vues et le détail d'une photo.],
  [4:00 - 6:30], [TravelShare — publication], [Montrer la création d'une publication complète.],
  [6:30 - 8:30], [Profil, groupes et notifications], [Montrer la gestion utilisateur, les groupes et les notifications.],
  [8:30 - 11:00], [TravelPath], [Montrer les préférences, la génération et le détail d'un parcours.],
  [11:00 - 13:00], [Passerelle], [Montrer le lien entre TravelShare et TravelPath dans les deux sens.],
  [13:00 - 13:30], [Conclusion], [Résumer les fonctionnalités réalisées.],
)

= Script détaillé

== 1. Introduction

*Action à l'écran* : lancer l'application et rester quelques secondes sur l'écran d'accueil.

*Voix* :

Dans cette vidéo, nous allons présenter notre application Android *Traveling*, développée dans le cadre du projet de programmation mobile. L'application est organisée autour de deux parties principales. La première partie, *TravelShare*, permet de partager et consulter des photos de voyage. La deuxième partie, *TravelPath*, permet de générer des parcours de visite à partir des préférences de l'utilisateur. Nous avons aussi ajouté une passerelle entre les deux modules afin que les lieux partagés dans TravelShare puissent être utilisés dans TravelPath.

== 2. Lancement, inscription, connexion et mode anonyme

=== Mode anonyme

*Action à l'écran* :

- depuis l'écran d'accueil, choisir le mode anonyme ;
- arriver sur l'écran principal ;
- montrer rapidement que l'utilisateur peut consulter les photos publiques.

*Voix* :

Au lancement, l'utilisateur peut choisir d'utiliser l'application en mode anonyme ou de se connecter. Le mode anonyme permet de découvrir les photos publiques sans créer immédiatement un compte. Nous utilisons une identité anonyme Firebase, ce qui permet de conserver certaines interactions comme les likes ou les publications enregistrées.

=== Connexion

*Action à l'écran* :

- revenir à l'écran de connexion ou utiliser le bouton de connexion ;
- entrer un compte de démonstration ;
- se connecter ;
- montrer l'arrivée dans l'espace principal connecté.

*Voix* :

L'utilisateur peut aussi se connecter avec un compte existant. Une fois connecté, il a accès aux fonctionnalités complètes, comme la publication de photos, les commentaires, les groupes, les notifications et la gestion du profil.

=== Inscription

*Action à l'écran* :

- ouvrir rapidement l'écran d'inscription ;
- montrer les champs disponibles sans forcément créer un nouveau compte pendant la vidéo.

*Voix* :

L'application propose également un écran d'inscription. Lors de la création du compte, l'utilisateur peut renseigner ses informations de profil, comme son nom, son avatar et sa signature. Ces données sont ensuite enregistrées pour être utilisées dans le profil et dans les publications.

== 3. TravelShare — galerie, recherche et consultation

=== Galerie principale

*Action à l'écran* :

- ouvrir l'onglet TravelShare ou Galerie ;
- faire défiler la liste des publications ;
- montrer les cartes de publications avec titre, auteur, lieu, tags et interactions.

*Voix* :

Voici la galerie principale de TravelShare. Les publications sont affichées par ordre chronologique, avec les informations importantes : le titre, l'auteur, le lieu, les tags et les interactions. L'utilisateur peut aimer ou enregistrer une publication directement depuis la liste.

=== Vues liste, grille et carte

*Action à l'écran* :

- passer de la vue liste à la vue grille ;
- passer ensuite à la vue carte ;
- cliquer sur un marqueur de la carte ;
- ouvrir le détail depuis la carte si possible.

*Voix* :

La galerie propose plusieurs modes d'affichage. La vue liste permet de consulter les publications avec plus de détails, la vue grille donne une vue plus visuelle, et la vue carte permet de voir les publications selon leur localisation. En cliquant sur un marqueur, on peut accéder à la publication correspondante.

=== Recherche et filtres

*Action à l'écran* :

- utiliser la barre de recherche avec un mot-clé ;
- ouvrir les filtres ;
- montrer le filtre par type de lieu ;
- montrer le filtre par période ;
- montrer le filtre autour d'un lieu avec choix du centre sur carte et rayon ;
- si possible, lancer rapidement la recherche vocale.

*Voix* :

TravelShare permet de rechercher des photos selon différents critères. L'utilisateur peut effectuer une recherche textuelle, utiliser la recherche vocale si elle est disponible sur l'appareil, filtrer par type de lieu, par période ou autour d'un point sélectionné sur la carte avec un rayon. Ces filtres permettent de retrouver plus facilement des photos selon le lieu, le thème ou le contexte du voyage.

=== Détail d'une publication

*Action à l'écran* :

- ouvrir une publication ;
- faire défiler le détail ;
- montrer les photos, le titre, la description, le lieu, les tags et l'auteur ;
- lancer la note vocale si la publication en possède une ;
- aimer et enregistrer la publication ;
- montrer les commentaires ;
- montrer le bouton Google Maps ;
- montrer le bouton Photos similaires.

*Voix* :

Dans le détail d'une publication, on retrouve toutes les informations liées à la photo : les images, le titre, la description, la localisation, les tags, l'auteur et la date. Si l'auteur a ajouté une note vocale, elle peut être écoutée depuis cet écran. L'utilisateur peut aussi aimer, enregistrer, commenter ou signaler une publication. Le bouton Google Maps permet d'ouvrir un itinéraire vers le lieu, et le bouton Photos similaires permet de retrouver d'autres publications proches par thème ou par tags.

== 4. TravelShare — publication d'une photo

*Action à l'écran* :

- cliquer sur le bouton de publication ;
- sélectionner plusieurs images ;
- supprimer une image sélectionnée pour montrer la gestion de la sélection ;
- remplir un titre, une description et des tags ;
- montrer l'annotation assistée si elle est disponible ;
- choisir une localisation sur la carte ;
- montrer le mode précis ou approximatif ;
- enregistrer ou ajouter une note vocale ;
- choisir la visibilité : public ou groupe ;
- activer l'option Ajouter au TravelPath ;
- montrer les paramètres TravelPath : type d'activité, coût, durée, horaires ;
- cliquer sur publier et montrer l'indicateur de chargement.

*Voix* :

Un utilisateur connecté peut publier une nouvelle photo de voyage. Le formulaire permet d'ajouter plusieurs images, de retirer une image avant publication, puis de compléter les informations de la publication : titre, description, tags et localisation. La localisation peut être choisie sur la carte, avec une précision exacte ou approximative. L'utilisateur peut également ajouter une note vocale et utiliser l'aide à l'annotation pour proposer des tags. La publication peut être publique ou partagée dans un groupe. Si l'option Ajouter au TravelPath est activée, le lieu peut aussi devenir une contribution utilisable dans la génération de parcours.

== 5. Profil utilisateur et gestion des publications

*Action à l'écran* :

- ouvrir le profil ;
- montrer l'avatar, la signature et les statistiques ;
- ouvrir les publications de l'utilisateur ;
- montrer l'écran de gestion des publications ;
- ouvrir la modification d'une publication ;
- montrer que l'écran de modification reprend le formulaire de publication ;
- revenir au profil ;
- ouvrir les publications aimées et enregistrées ;
- ouvrir la liste des auteurs suivis.

*Voix* :

Le profil utilisateur affiche les informations personnelles, comme l'avatar et la signature. Depuis cet écran, l'utilisateur peut consulter ses publications, ses photos aimées, ses photos enregistrées et les auteurs suivis. Les publications déjà créées peuvent être modifiées ou supprimées. Pour la modification, nous réutilisons le formulaire de publication afin de permettre à l'utilisateur de mettre à jour les informations principales de manière complète.

== 6. Groupes

*Action à l'écran* :

- ouvrir l'écran des groupes ;
- montrer les groupes rejoints et les groupes disponibles ;
- ouvrir un groupe ;
- montrer la description du groupe et les publications du groupe ;
- si possible, montrer la création ou l'adhésion à un groupe.

*Voix* :

TravelShare propose aussi une gestion de groupes. Un utilisateur peut créer un groupe, rejoindre un groupe existant, consulter les publications partagées dans un groupe et publier une photo uniquement pour les membres du groupe. Cette fonctionnalité permet de partager des souvenirs de voyage avec un cercle plus restreint qu'une publication publique.

== 7. Notifications

*Action à l'écran* :

- ouvrir l'écran des notifications ;
- montrer la liste des notifications ;
- ouvrir les paramètres de notification ;
- montrer les cinq catégories : auteurs, groupes, posts, tags et lieux ;
- activer ou désactiver rapidement une option.

*Voix* :

L'application dispose d'un système de notifications internes. L'utilisateur peut être informé lorsqu'un auteur suivi publie une photo, lorsqu'un groupe a une nouvelle activité, lorsqu'une publication reçoit une interaction, ou lorsqu'un tag ou un type de lieu suivi est concerné. Les paramètres de notifications permettent de contrôler ces différentes catégories.

== 8. TravelPath — génération de parcours

=== Formulaire de préférences

*Action à l'écran* :

- ouvrir l'onglet TravelPath ;
- choisir une destination ;
- sélectionner plusieurs activités ;
- ajouter des lieux favoris ;
- régler le budget, la durée et l'effort ;
- montrer les tolérances météo ;
- lancer la génération.

*Voix* :

Dans TravelPath, l'utilisateur commence par remplir ses préférences. Il choisit une destination, des types d'activités, des lieux favoris, un budget maximal, une durée de visite et un niveau d'effort. Le formulaire prend aussi en compte certaines contraintes météo, comme éviter la pluie, les fortes chaleurs ou le froid.

=== Résultats de parcours

*Action à l'écran* :

- montrer les différentes propositions générées ;
- présenter les cartes de résultats : économique, équilibré, premium ;
- ouvrir un parcours.

*Voix* :

À partir de ces préférences, l'application génère plusieurs propositions de parcours. Les parcours sont calculés avec différentes stratégies, par exemple un parcours plus économique, un parcours équilibré ou un parcours premium. Chaque proposition affiche des métriques comme le budget, la durée, le niveau d'effort et le nombre d'étapes.

=== Détail d'un parcours

*Action à l'écran* :

- faire défiler le détail du parcours ;
- montrer les étapes matin, après-midi et soir ;
- montrer les informations d'une étape ;
- montrer la mini-carte ou les informations de distance ;
- montrer les photos liées au parcours ;
- montrer les actions : favori, sauvegarde, partage, export PDF ou navigation externe selon ce qui est visible.

*Voix* :

Dans le détail d'un parcours, les étapes sont organisées par créneaux de la journée. Pour chaque étape, l'utilisateur peut consulter le type d'activité, le coût, la durée, les horaires, la distance depuis l'étape précédente et les informations utiles. Le parcours peut aussi afficher des photos partagées par les voyageurs lorsque des publications TravelShare correspondent aux lieux visités.

== 9. Passerelle entre TravelShare et TravelPath

=== Depuis TravelShare vers TravelPath

*Action à l'écran* :

- revenir dans TravelShare ;
- ouvrir une publication avec un lieu ;
- cliquer sur Utiliser ce lieu ;
- montrer que l'application passe à TravelPath ;
- montrer que le formulaire est prérempli avec le lieu ou la destination.

*Voix* :

La passerelle permet d'utiliser un lieu découvert dans TravelShare comme point de départ pour TravelPath. Depuis le détail d'une publication, le bouton Utiliser ce lieu permet de passer directement au formulaire TravelPath. Le lieu de la photo est alors ajouté aux informations du parcours.

=== Depuis la publication vers les données TravelPath

*Action à l'écran* :

- ouvrir une publication ou le formulaire de publication où l'option Ajouter au TravelPath est visible ;
- montrer que le lieu peut être ajouté comme contribution TravelPath ;
- expliquer rapidement sans forcément publier à nouveau.

*Voix* :

Lorsqu'un utilisateur publie une photo, il peut aussi choisir d'ajouter le lieu à TravelPath. Si la destination existe déjà, la publication est liée à cette destination. Si elle n'existe pas encore, une destination issue de TravelShare peut être créée. Les contributions des utilisateurs sont distinguées des données officielles afin de garder une séparation claire.

=== Depuis TravelPath vers TravelShare

*Action à l'écran* :

- dans le détail d'un parcours, cliquer sur une photo TravelShare associée à une étape ;
- montrer l'ouverture du détail de la publication TravelShare.

*Voix* :

La passerelle fonctionne aussi dans l'autre sens. Depuis un parcours TravelPath, l'utilisateur peut consulter des photos partagées par d'autres voyageurs pour certains lieux du parcours. En cliquant sur une photo, on revient au détail de la publication TravelShare.

== 10. Conclusion de la vidéo

*Action à l'écran* :

- revenir à l'écran principal ou au profil ;
- terminer sur une vue stable de l'application.

*Voix* :

Pour conclure, notre application Traveling propose une expérience complète autour du voyage. TravelShare permet de partager, rechercher et consulter des photos avec des interactions sociales, des groupes, des notifications et une gestion de profil. TravelPath permet de générer des parcours personnalisés à partir des préférences de l'utilisateur. Enfin, la passerelle relie les deux modules en permettant aux lieux partagés par les voyageurs d'enrichir les parcours proposés. L'application utilise Kotlin, Jetpack Compose, Firebase et une architecture MVVM.

= Checklist finale avant de filmer

- vérifier que l'application est connectée au bon projet Firebase ;
- vérifier que les images et notes vocales de démonstration se chargent correctement ;
- éviter de montrer des données personnelles réelles ;
- préparer un compte de démonstration ;
- préparer une publication de test pour la modification ;
- préparer un groupe de test ;
- préparer un parcours TravelPath générable rapidement ;
- fermer les notifications système inutiles pendant l'enregistrement ;
- parler lentement et éviter de rester trop longtemps sur les chargements.

