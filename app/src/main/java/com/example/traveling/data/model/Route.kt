package com.example.traveling.data.model

data class TravelRoute(
    val id: String,
    val name: String,
    val subtitle: String,
    val budget: Int,
    val duration: String,
    val effort: String,
    val effortLevel: Int,
    val stops: Int,
    val rating: Float,
    val reviews: Int,
    val imageUrl: String,
    val highlights: List<String>,
    val gradientColors: Pair<Long, Long> // Start and end gradient color
)

data class RouteStop(
    val id: String,
    val name: String,
    val type: String,
    val timeSlot: TimeSlot,
    val arrivalTime: String,
    val duration: String,
    val distance: String,
    val walkTime: String,
    val cost: Int,
    val description: String,
    val imageUrl: String,
    val rating: Float,
    val openHours: String
)

enum class TimeSlot(val label: String) {
    MATIN("Matin"),
    APRES_MIDI("Après-midi"),
    SOIR("Soir")
}

data class Activity(
    val id: String,
    val label: String,
    val icon: String
)

// Seed data matching the Figma design
object TravelPathData {

    val activities = listOf(
        Activity("culture", "Culture", "🏛️"),
        Activity("food", "Gastronomie", "🍽️"),
        Activity("nature", "Nature", "🌳"),
        Activity("leisure", "Loisirs", "🎮"),
        Activity("shopping", "Shopping", "🛍️"),
        Activity("nightlife", "Vie nocturne", "🌃"),
        Activity("sport", "Sport", "⚽"),
        Activity("photo", "Photo", "📸"),
    )

    val quickCities = listOf("Pékin", "Xi'an", "Hangzhou", "Chengdu", "Guilin")

    val routes = listOf(
        TravelRoute(
            id = "1", name = "Route Économique",
            subtitle = "Pour voyageurs avec budget limité",
            budget = 300, duration = "3-4 heures", effort = "Modéré", effortLevel = 3,
            stops = 5, rating = 4.6f, reviews = 128,
            imageUrl = "https://images.unsplash.com/photo-1603120527222-33f28c2ce89e?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800",
            highlights = listOf("Cité Interdite", "Place Tian'anmen", "Parc Jingshan", "Nanluoguxiang", "Shichahai"),
            gradientColors = Pair(0xFF10B981, 0xFF0D9488)
        ),
        TravelRoute(
            id = "2", name = "Route Équilibrée",
            subtitle = "Meilleur rapport qualité-prix",
            budget = 600, duration = "5-6 heures", effort = "Facile", effortLevel = 2,
            stops = 7, rating = 4.8f, reviews = 256,
            imageUrl = "https://images.unsplash.com/photo-1558507564-c573429b9ceb?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800",
            highlights = listOf("Grande Muraille", "Palais d'Été", "Temple du Ciel", "Déjeuner canard laqué", "Tour Hutong", "Nid d'Oiseau", "Wangfujing"),
            gradientColors = Pair(0xFFB91C1C, 0xFF991B1B)
        ),
        TravelRoute(
            id = "3", name = "Route Premium",
            subtitle = "Expérience haut de gamme",
            budget = 1500, duration = "7-8 heures", effort = "Très facile", effortLevel = 1,
            stops = 9, rating = 4.9f, reviews = 89,
            imageUrl = "https://images.unsplash.com/photo-1647067151201-0b37c7555870?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=800",
            highlights = listOf("Visite privée Cité Interdite", "Déjeuner Michelin", "Palais du Prince Gong", "Expérience SPA", "Théâtre Chang'an"),
            gradientColors = Pair(0xFFF59E0B, 0xFFB45309)
        ),
    )

    val routeStops = listOf(
        RouteStop("s1", "Cité Interdite", "Culture", TimeSlot.MATIN, "09:00", "2h30", "Départ", "",
            60, "Le plus grand complexe de palais antiques au monde, 600 ans d'histoire, un million de trésors nationaux.",
            "https://images.unsplash.com/photo-1603120527222-33f28c2ce89e?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", 4.9f, "8:30-17:00 (fermé lundi)"),
        RouteStop("s2", "Parc Jingshan", "Nature", TimeSlot.MATIN, "11:30", "45 min", "350m", "5 min à pied",
            2, "Vue panoramique sur la Cité Interdite depuis le sommet, meilleur point de vue sur l'axe central de Pékin.",
            "https://images.unsplash.com/photo-1687524669097-7fc9b204f92f?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", 4.5f, "6:00-21:00"),
        RouteStop("s3", "Déjeuner - Canard Quanjude", "Gastronomie", TimeSlot.APRES_MIDI, "12:30", "1h15", "2km", "5 min en taxi",
            180, "Restaurant centenaire, authentique canard laqué de Pékin, peau croustillante et viande tendre.",
            "https://images.unsplash.com/photo-1672891197847-d3a65c11b33d?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", 4.3f, "11:00-22:00"),
        RouteStop("s4", "Temple du Ciel", "Monument", TimeSlot.APRES_MIDI, "14:00", "2h", "6km", "15 min en taxi",
            34, "Lieu de culte impérial des Ming et Qing, le Hall de la Prière est un chef-d'œuvre architectural chinois.",
            "https://images.unsplash.com/photo-1709133332724-2f56232c77eb?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", 4.7f, "6:00-22:00"),
        RouteStop("s5", "Shichahai", "Loisirs", TimeSlot.APRES_MIDI, "16:30", "1h", "8km", "20 min en taxi",
            0, "Culture traditionnelle des Hutong, promenade au bord du lac, ambiance populaire de Pékin.",
            "https://images.unsplash.com/photo-1772490184794-4ae50d917785?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", 4.4f, "24/7"),
        RouteStop("s6", "Nanluoguxiang", "Culture", TimeSlot.SOIR, "18:00", "1h30", "1km", "10 min à pied",
            0, "Hutong le mieux préservé de l'époque Yuan, rue de boutiques et de gastronomie.",
            "https://images.unsplash.com/photo-1659466248885-8b7a03205661?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", 4.6f, "24/7"),
        RouteStop("s7", "Dîner - Fondue Mouton", "Gastronomie", TimeSlot.SOIR, "20:00", "1h30", "2km", "15 min à pied",
            120, "Fondue traditionnelle de Pékin au pot de cuivre, avec sauce sésame et ail sucré, goût authentique.",
            "https://images.unsplash.com/photo-1755710116297-20f8e42d3c28?crop=entropy&cs=tinysrgb&fit=max&fm=jpg&w=400", 4.5f, "11:00-23:00"),
    )
}
