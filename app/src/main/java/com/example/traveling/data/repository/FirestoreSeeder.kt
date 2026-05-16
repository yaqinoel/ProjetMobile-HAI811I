package com.example.traveling.data.repository

import android.util.Log
import com.example.traveling.data.model.Attraction
import com.example.traveling.data.model.Destination
import com.google.firebase.firestore.FirebaseFirestore
object FirestoreSeeder {

    private val db = FirebaseFirestore.getInstance()
    private const val TAG = "FirestoreSeeder"
    private val oldChineseDestinationIds = setOf("pekin", "xian", "hangzhou", "chengdu", "guilin")

    fun seedAll(clearFirst: Boolean = false) {
        if (clearFirst) {
            // utile seulement en dev pour repartir d'une base propre
            clearCollection("destinations") { seedDestinations() }
            clearCollection("attractions") { seedAttractions() }
        } else {
            seedDestinations()
            seedAttractions()
        }
    }

    private fun seedDestinations() {
        val destinations = listOf(

            Destination("montpellier", "Montpellier", "France",
                "Ville mediterraneenne jeune et ensoleillee, entre ecusson medieval, architecture contemporaine et plages proches.",
                "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=800", 43.6108, 3.8767),
            Destination("toulouse", "Toulouse", "France",
                "La Ville Rose, capitale aeronautique et occitane, connue pour ses briques, ses quais et son art de vivre.",
                "https://images.unsplash.com/photo-1499856871958-5b9627545d1a?w=800", 43.6047, 1.4442),
            Destination("strasbourg", "Strasbourg", "France",
                "Capitale alsacienne et europeenne, entre cathedrale gothique, Petite France et canaux historiques.",
                "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=800", 48.5734, 7.7521),
            Destination("nantes", "Nantes", "France",
                "Ville creative sur la Loire, entre chateau, Machines de l'Ile, patrimoine maritime et quartiers artistiques.",
                "https://images.unsplash.com/photo-1518005020951-eccb494ad742?w=800", 47.2184, -1.5536),
            Destination("lille", "Lille", "France",
                "Metropole chaleureuse du Nord, celebre pour sa Grand-Place, le Vieux-Lille et ses musees.",
                "https://images.unsplash.com/photo-1499856871958-5b9627545d1a?w=800", 50.6292, 3.0573),

            Destination("paris", "Paris", "France",
                "Ville Lumière, capitale de l'art, de la mode et de la gastronomie, iconique pour la Tour Eiffel et le Louvre.",
                "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800", 48.8566, 2.3522),
            Destination("lyon", "Lyon", "France",
                "Capitale de la gastronomie française, ville historique entre Rhône et Saône, classée UNESCO.",
                "https://images.unsplash.com/photo-1524396309943-e03f5249f002?w=800", 45.7640, 4.8357),
            Destination("nice", "Nice", "France",
                "Perle de la Côte d'Azur, promenade des Anglais, mer turquoise et vieille ville colorée.",
                "https://images.unsplash.com/photo-1491166617655-0723a0999cfc?w=800", 43.7102, 7.2620),
            Destination("marseille", "Marseille", "France",
                "Plus ancienne ville de France, Calanques spectaculaires, Vieux-Port animé et Notre-Dame de la Garde.",
                "https://images.unsplash.com/photo-1589394815804-964ed0be2eb5?w=800", 43.2965, 5.3698),
            Destination("bordeaux", "Bordeaux", "France",
                "Capitale mondiale du vin, architecture XVIIIe siècle, Cité du Vin et vignobles prestigieux.",
                "https://images.unsplash.com/photo-1559128010-7c1ad6e1b6a5?w=800", 44.8378, -0.5792),
        )

        val batch = db.batch()
        destinations.forEach { dest ->
            val ref = db.collection("destinations").document(dest.id)
            batch.set(ref, dest)
        }
        batch.commit()
            .addOnSuccessListener { Log.d(TAG, " ${destinations.size} destinations écrites") }
            .addOnFailureListener { Log.e(TAG, " Erreur destinations", it) }
    }

    private fun seedAttractions() {
        val attractions = buildList {

            addAll(montpellierAttractions())
            addAll(toulouseAttractions())
            addAll(strasbourgAttractions())
            addAll(nantesAttractions())
            addAll(lilleAttractions())

            addAll(parisAttractions())
            addAll(lyonAttractions())
            addAll(niceAttractions())
            addAll(marseilleAttractions())
            addAll(bordeauxAttractions())
            addAll(supplementalAttractions().filterNot { it.destinationId in oldChineseDestinationIds })
        }.map { it.withGooglePlaceImages() }

        val batches = attractions.chunked(400)
        batches.forEachIndexed { idx, chunk ->
            // firestore limite les batchs, donc on coupe la liste
            val batch = db.batch()
            chunk.forEach { attr ->
                val ref = db.collection("attractions").document(attr.id)
                batch.set(ref, attr)
            }
            batch.commit()
                .addOnSuccessListener { Log.d(TAG, "Batch ${idx + 1}: ${chunk.size} attractions écrites") }
                .addOnFailureListener { Log.e(TAG, "Erreur attractions batch ${idx + 1}", it) }
        }
    }

    private fun montpellierAttractions() = listOf(
        quickAttraction("mtp_comedie", "montpellier", "Place de la Comedie", "Monument", 0, 45, 4.7, 43.6085, 3.8796, "matin", "outdoor"),
        quickAttraction("mtp_ecusson", "montpellier", "Ecusson medieval", "Culture", 0, 90, 4.6, 43.6114, 3.8767, "matin", "outdoor"),
        quickAttraction("mtp_fabre", "montpellier", "Musee Fabre", "Culture", 9, 105, 4.6, 43.6119, 3.8804, "apres-midi", "indoor"),
        quickAttraction("mtp_jardin_plantes", "montpellier", "Jardin des Plantes", "Nature", 0, 60, 4.5, 43.6145, 3.8711, "apres-midi", "outdoor"),
        quickAttraction("mtp_arc_triomphe", "montpellier", "Arc de Triomphe", "Monument", 0, 45, 4.5, 43.6118, 3.8728, "apres-midi", "outdoor"),
        quickAttraction("mtp_antigone", "montpellier", "Quartier Antigone", "Photo", 0, 60, 4.3, 43.6082, 3.8903, "soir", "outdoor"),
        quickAttraction("mtp_marches_lelez", "montpellier", "Marches du Lez", "Gastronomie", 35, 90, 4.4, 43.6038, 3.9128, "soir", "both"),
        quickAttraction("mtp_aquarium", "montpellier", "Planet Ocean Montpellier", "Loisirs", 19, 120, 4.4, 43.6033, 3.9160, "apres-midi", "indoor")
    )

    private fun toulouseAttractions() = listOf(
        quickAttraction("tl_capitole", "toulouse", "Place du Capitole", "Monument", 0, 60, 4.8, 43.6045, 1.4440, "matin", "outdoor"),
        quickAttraction("tl_basilique", "toulouse", "Basilique Saint-Sernin", "Culture", 0, 60, 4.7, 43.6086, 1.4419, "matin", "indoor"),
        quickAttraction("tl_jacobins", "toulouse", "Couvent des Jacobins", "Culture", 5, 60, 4.6, 43.6032, 1.4380, "apres-midi", "indoor"),
        quickAttraction("tl_garonne", "toulouse", "Quais de Garonne", "Nature", 0, 75, 4.5, 43.5995, 1.4360, "apres-midi", "outdoor"),
        quickAttraction("tl_cite_espace", "toulouse", "Cite de l'Espace", "Loisirs", 26, 180, 4.6, 43.5861, 1.4930, "apres-midi", "both"),
        quickAttraction("tl_halles_victor_hugo", "toulouse", "Halles Victor Hugo", "Gastronomie", 35, 75, 4.6, 43.6074, 1.4471, "matin", "indoor"),
        quickAttraction("tl_carmes", "toulouse", "Quartier des Carmes", "Shopping", 50, 90, 4.4, 43.5976, 1.4440, "soir", "both"),
        quickAttraction("tl_ponts_neufs", "toulouse", "Pont Neuf", "Photo", 0, 45, 4.5, 43.5990, 1.4392, "soir", "outdoor")
    )

    private fun strasbourgAttractions() = listOf(
        quickAttraction("st_cathedrale", "strasbourg", "Cathedrale Notre-Dame", "Monument", 0, 90, 4.9, 48.5819, 7.7506, "matin", "indoor"),
        quickAttraction("st_petite_france", "strasbourg", "Petite France", "Photo", 0, 90, 4.8, 48.5794, 7.7399, "matin", "outdoor"),
        quickAttraction("st_barrage_vauban", "strasbourg", "Barrage Vauban", "Monument", 0, 60, 4.6, 48.5791, 7.7375, "apres-midi", "outdoor"),
        quickAttraction("st_musee_alsacien", "strasbourg", "Musee Alsacien", "Culture", 7, 90, 4.5, 48.5795, 7.7508, "apres-midi", "indoor"),
        quickAttraction("st_parc_orangerie", "strasbourg", "Parc de l'Orangerie", "Nature", 0, 90, 4.6, 48.5924, 7.7763, "apres-midi", "outdoor"),
        quickAttraction("st_parlement", "strasbourg", "Parlement europeen", "Culture", 0, 75, 4.4, 48.5975, 7.7680, "matin", "indoor"),
        quickAttraction("st_winstub", "strasbourg", "Winstub alsacienne", "Gastronomie", 38, 90, 4.5, 48.5822, 7.7455, "soir", "indoor"),
        quickAttraction("st_krutenau", "strasbourg", "Krutenau", "Shopping", 45, 75, 4.3, 48.5780, 7.7563, "soir", "both")
    )

    private fun nantesAttractions() = listOf(
        quickAttraction("na_chateau", "nantes", "Chateau des Ducs de Bretagne", "Culture", 9, 120, 4.6, 47.2163, -1.5495, "matin", "both"),
        quickAttraction("na_machines", "nantes", "Machines de l'Ile", "Loisirs", 9, 120, 4.7, 47.2062, -1.5644, "apres-midi", "both"),
        quickAttraction("na_passage_pommeraye", "nantes", "Passage Pommeraye", "Shopping", 0, 45, 4.7, 47.2135, -1.5603, "matin", "indoor"),
        quickAttraction("na_jardin_plantes", "nantes", "Jardin des Plantes", "Nature", 0, 75, 4.6, 47.2190, -1.5423, "apres-midi", "outdoor"),
        quickAttraction("na_lieu_unique", "nantes", "Lieu Unique", "Culture", 0, 75, 4.4, 47.2153, -1.5458, "soir", "indoor"),
        quickAttraction("na_ile_versailles", "nantes", "Ile de Versailles", "Nature", 0, 60, 4.5, 47.2261, -1.5537, "apres-midi", "outdoor"),
        quickAttraction("na_talusac", "nantes", "Marche de Talensac", "Gastronomie", 28, 75, 4.5, 47.2204, -1.5582, "matin", "indoor"),
        quickAttraction("na_graslin", "nantes", "Quartier Graslin", "Photo", 0, 60, 4.4, 47.2133, -1.5622, "soir", "outdoor")
    )

    private fun lilleAttractions() = listOf(
        quickAttraction("li_grand_place", "lille", "Grand-Place", "Monument", 0, 60, 4.7, 50.6372, 3.0633, "matin", "outdoor"),
        quickAttraction("li_vieux_lille", "lille", "Vieux-Lille", "Shopping", 45, 90, 4.7, 50.6407, 3.0647, "matin", "both"),
        quickAttraction("li_palais_beaux_arts", "lille", "Palais des Beaux-Arts", "Culture", 7, 120, 4.6, 50.6300, 3.0627, "apres-midi", "indoor"),
        quickAttraction("li_citadelle", "lille", "Parc de la Citadelle", "Nature", 0, 90, 4.5, 50.6422, 3.0457, "apres-midi", "outdoor"),
        quickAttraction("li_hospice_comtesse", "lille", "Musee de l'Hospice Comtesse", "Culture", 5, 75, 4.4, 50.6415, 3.0638, "apres-midi", "indoor"),
        quickAttraction("li_wazemmes", "lille", "Marche de Wazemmes", "Gastronomie", 25, 75, 4.4, 50.6266, 3.0505, "matin", "both"),
        quickAttraction("li_gare_saint_sauveur", "lille", "Gare Saint Sauveur", "Loisirs", 0, 75, 4.3, 50.6269, 3.0740, "soir", "indoor"),
        quickAttraction("li_opera", "lille", "Opera de Lille", "Photo", 0, 45, 4.5, 50.6378, 3.0646, "soir", "outdoor")
    )

    private fun pekinAttractions() = listOf(
        Attraction("pk_cite_interdite", "pekin", "Cité Interdite", "Culture",
            60, 150, 4.9,
            "Le plus grand complexe de palais antiques au monde, 600 ans d'histoire impériale, un million de trésors nationaux.",
            "https://images.unsplash.com/photo-1508804185872-d7badad00f7d?w=400",
            39.9163, 116.3972, "8:30-17:00", "lundi", 3, "outdoor",
            listOf("matin"), listOf("historique", "UNESCO", "palais")),

        Attraction("pk_jingshan", "pekin", "Parc Jingshan", "Nature",
            2, 45, 4.5,
            "Vue panoramique sur la Cité Interdite depuis le sommet, meilleur point de vue sur l'axe central de Pékin.",
            "https://images.unsplash.com/photo-1590417975344-91bac14d3431?w=400",
            39.9250, 116.3965, "6:00-21:00", "", 2, "outdoor",
            listOf("matin"), listOf("panorama", "parc", "colline")),

        Attraction("pk_quanjude", "pekin", "Canard Laqué Quanjude", "Gastronomie",
            180, 75, 4.3,
            "Restaurant centenaire, authentique canard laqué de Pékin, peau croustillante et viande tendre.",
            "https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?w=400",
            39.8987, 116.3948, "11:00-22:00", "", 1, "indoor",
            listOf("apres-midi"), listOf("gastronomie", "canard", "centenaire")),

        Attraction("pk_temple_ciel", "pekin", "Temple du Ciel", "Monument",
            34, 120, 4.7,
            "Lieu de culte impérial des Ming et Qing, le Hall de la Prière est un chef-d'œuvre architectural chinois.",
            "https://images.unsplash.com/photo-1599571234909-29ed5d1321d6?w=400",
            39.8822, 116.4066, "6:00-22:00", "", 2, "outdoor",
            listOf("apres-midi"), listOf("temple", "UNESCO", "architecture")),

        Attraction("pk_shichahai", "pekin", "Shichahai", "Loisirs",
            0, 60, 4.4,
            "Culture traditionnelle des Hutong, promenade au bord du lac, ambiance populaire de Pékin.",
            "https://images.unsplash.com/photo-1547981609-4b6bfe67ca0b?w=400",
            39.9390, 116.3836, "24/7", "", 2, "outdoor",
            listOf("apres-midi", "soir"), listOf("hutong", "lac", "promenade")),

        Attraction("pk_nanluoguxiang", "pekin", "Nanluoguxiang", "Culture",
            0, 90, 4.6,
            "Hutong le mieux préservé de l'époque Yuan, rue piétonne de boutiques artisanales et de street food.",
            "https://images.unsplash.com/photo-1548804528-9e10e0e1fd31?w=400",
            39.9370, 116.4030, "24/7", "", 1, "outdoor",
            listOf("apres-midi", "soir"), listOf("hutong", "shopping", "street-food")),

        Attraction("pk_fondue_mouton", "pekin", "Fondue au Mouton Pékinoise", "Gastronomie",
            120, 90, 4.5,
            "Fondue traditionnelle au pot de cuivre, sauce sésame et ail sucré, goût authentique de Pékin.",
            "https://images.unsplash.com/photo-1555126634-323283e090fa?w=400",
            39.9310, 116.4050, "11:00-23:00", "", 1, "indoor",
            listOf("soir"), listOf("gastronomie", "fondue", "traditionnel")),

        Attraction("pk_grande_muraille", "pekin", "Grande Muraille (Badaling)", "Monument",
            40, 180, 4.8,
            "Section la plus célèbre de la Grande Muraille, vue spectaculaire sur les montagnes à perte de vue.",
            "https://images.unsplash.com/photo-1508804185872-d7badad00f7d?w=400",
            40.3541, 116.0137, "6:30-19:00", "", 5, "outdoor",
            listOf("matin"), listOf("UNESCO", "merveille", "randonnée")),

        Attraction("pk_helicoptere", "pekin", "Survol de la Muraille en Hélicoptère", "Loisirs",
            2500, 30, 4.9,
            "Expérience VIP : survolez la Grande Muraille au coucher du soleil pour une vue exceptionnelle à couper le souffle.",
            "https://images.unsplash.com/photo-1508804185872-d7badad00f7d?w=400",
            40.3540, 116.0130, "15:00-18:00", "", 1, "outdoor",
            listOf("apres-midi"), listOf("premium", "vip", "hélicoptère", "vol"))
    )

    private fun xianAttractions() = listOf(
        Attraction("xa_terrecuite", "xian", "Armée de Terre Cuite", "Culture",
            120, 180, 4.9,
            "Plus de 8000 soldats en terre cuite grandeur nature, l'une des plus grandes découvertes archéologiques.",
            "https://images.unsplash.com/photo-1591017403286-fd8493524e1e?w=400",
            34.3844, 109.2785, "8:30-18:00", "", 2, "indoor",
            listOf("matin"), listOf("UNESCO", "archéologie", "Qin")),

        Attraction("xa_remparts", "xian", "Remparts de Xi'an", "Monument",
            54, 120, 4.7,
            "Murs d'enceinte Ming les mieux conservés de Chine. Tour à vélo sur les remparts avec vue panoramique.",
            "https://images.unsplash.com/photo-1563779283630-e0358cb8e8c7?w=400",
            34.2605, 108.9430, "8:00-22:00", "", 3, "outdoor",
            listOf("matin", "apres-midi"), listOf("Ming", "vélo", "fortification")),

        Attraction("xa_grande_pagode", "xian", "Grande Pagode de l'Oie Sauvage", "Monument",
            40, 60, 4.6,
            "Pagode bouddhiste Tang du 7e siècle, symbole de Xi'an, spectacle de fontaines musicales.",
            "https://images.unsplash.com/photo-1570698473886-604536e7e2b6?w=400",
            34.2185, 108.9594, "8:00-17:30", "", 2, "outdoor",
            listOf("apres-midi"), listOf("bouddhisme", "Tang", "pagode")),

        Attraction("xa_quartier_musulman", "xian", "Quartier Musulman", "Gastronomie",
            0, 90, 4.5,
            "Rue gastronomique millénaire avec spécialités Hui : roujiamo, yangroupaomo, brochettes d'agneau.",
            "https://images.unsplash.com/photo-1514395462725-fb4566210144?w=400",
            34.2640, 108.9380, "24/7", "", 1, "outdoor",
            listOf("apres-midi", "soir"), listOf("street-food", "Hui", "halal")),

        Attraction("xa_grande_mosquee", "xian", "Grande Mosquée de Xi'an", "Culture",
            25, 45, 4.4,
            "Mosquée millénaire de style chinois traditionnel, mélange unique d'architecture sino-islamique.",
            "https://images.unsplash.com/photo-1600091166971-7f9faad6c1e1?w=400",
            34.2630, 108.9375, "8:00-19:00", "", 1, "indoor",
            listOf("apres-midi"), listOf("mosquée", "Islam", "architecture")),

        Attraction("xa_mont_huashan", "xian", "Mont Huashan", "Nature",
            160, 360, 4.8,
            "L'une des cinq montagnes sacrées de Chine, sentiers vertigineux et vues spectaculaires.",
            "https://images.unsplash.com/photo-1518509562904-e7ef99cdbc86?w=400",
            34.4761, 110.0898, "7:00-19:00", "", 5, "outdoor",
            listOf("matin"), listOf("montagne", "randonnée", "sacré")),
    )

    private fun hangzhouAttractions() = listOf(
        Attraction("hz_lac_ouest", "hangzhou", "Lac de l'Ouest", "Nature",
            0, 120, 4.9,
            "Lac emblématique inscrit à l'UNESCO, pagodes, ponts en zigzag et jardins classiques chinois.",
            "https://images.unsplash.com/photo-1598887142487-3c854d51eabb?w=400",
            30.2500, 120.1300, "24/7", "", 2, "outdoor",
            listOf("matin", "apres-midi"), listOf("UNESCO", "lac", "jardin")),

        Attraction("hz_lingyin", "hangzhou", "Temple Lingyin", "Culture",
            75, 90, 4.7,
            "Temple bouddhiste millénaire niché dans la forêt, sculptures rupestres de Feilai Feng.",
            "https://images.unsplash.com/photo-1600091166971-7f9faad6c1e1?w=400",
            30.2374, 120.0936, "7:00-18:15", "", 2, "outdoor",
            listOf("matin"), listOf("bouddhisme", "temple", "sculptures")),

        Attraction("hz_longjing", "hangzhou", "Village du Thé Longjing", "Nature",
            0, 90, 4.6,
            "Terroir du célèbre thé vert Longjing, plantations en terrasses et dégustation chez l'habitant.",
            "https://images.unsplash.com/photo-1556881286-fc6915169721?w=400",
            30.2150, 120.0870, "8:00-17:00", "", 2, "outdoor",
            listOf("apres-midi"), listOf("thé", "plantation", "dégustation")),

        Attraction("hz_pagode_leifeng", "hangzhou", "Pagode Leifeng", "Monument",
            40, 60, 4.5,
            "Pagode reconstruite au bord du Lac de l'Ouest, liée à la légende du Serpent Blanc.",
            "https://images.unsplash.com/photo-1584464333682-070f3c0b2e1e?w=400",
            30.2315, 120.1490, "8:00-20:30", "", 2, "outdoor",
            listOf("apres-midi"), listOf("légende", "pagode", "vue")),

        Attraction("hz_rue_hefang", "hangzhou", "Rue Hefang", "Loisirs",
            0, 60, 4.3,
            "Rue piétonne historique avec artisanat traditionnel, pharmacies centenaires et snacks locaux.",
            "https://images.unsplash.com/photo-1566991016940-23f94f24dc4c?w=400",
            30.2460, 120.1680, "9:00-22:00", "", 1, "outdoor",
            listOf("apres-midi", "soir"), listOf("shopping", "artisanat", "street-food")),

        Attraction("hz_impression_ouest", "hangzhou", "Spectacle Impression West Lake", "Loisirs",
            300, 70, 4.8,
            "Spectacle nocturne de Zhang Yimou sur le Lac de l'Ouest, danse et lumières sur l'eau.",
            "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=400",
            30.2460, 120.1310, "19:30-20:40", "", 1, "outdoor",
            listOf("soir"), listOf("spectacle", "Zhang Yimou", "nocturne")),
    )

    private fun chengduAttractions() = listOf(
        Attraction("cd_pandas", "chengdu", "Base des Pandas Géants", "Nature",
            55, 150, 4.9,
            "Centre de recherche et d'élevage des pandas géants, observation des bébés pandas.",
            "https://images.unsplash.com/photo-1564349683136-77e08dba1ef7?w=400",
            30.7327, 104.1427, "7:30-18:00", "", 2, "outdoor",
            listOf("matin"), listOf("panda", "animal", "conservation")),

        Attraction("cd_jinli", "chengdu", "Rue Jinli", "Loisirs",
            0, 90, 4.5,
            "Rue antique reconstituée de l'époque Shu, artisanat, cuisine de rue et théâtre d'ombres.",
            "https://images.unsplash.com/photo-1590736969955-71cc94901144?w=400",
            30.6444, 104.0477, "24/7", "", 1, "outdoor",
            listOf("apres-midi", "soir"), listOf("rue-antique", "artisanat", "Shu")),

        Attraction("cd_hotpot", "chengdu", "Fondue Sichuanaise", "Gastronomie",
            100, 90, 4.7,
            "Fondue épicée au poivre du Sichuan, bouillon rouge flottant de piments, expérience gustative intense.",
            "https://images.unsplash.com/photo-1555126634-323283e090fa?w=400",
            30.6570, 104.0660, "11:00-23:00", "", 1, "indoor",
            listOf("soir"), listOf("épicé", "fondue", "Sichuan")),

        Attraction("cd_wuhou", "chengdu", "Temple Wuhou", "Culture",
            50, 90, 4.6,
            "Temple des Trois Royaumes dédié à Zhuge Liang, jardins classiques et musée historique.",
            "https://images.unsplash.com/photo-1600091166971-7f9faad6c1e1?w=400",
            30.6434, 104.0462, "8:00-18:00", "", 2, "indoor",
            listOf("apres-midi"), listOf("Trois-Royaumes", "Zhuge-Liang", "musée")),

        Attraction("cd_leshan", "chengdu", "Grand Bouddha de Leshan", "Monument",
            80, 240, 4.8,
            "Plus grand Bouddha de pierre au monde (71m), sculpté dans la falaise à la confluence de trois rivières.",
            "https://images.unsplash.com/photo-1631552587683-f4f2fc386db6?w=400",
            29.5443, 103.7735, "7:30-18:30", "", 4, "outdoor",
            listOf("matin"), listOf("UNESCO", "Bouddha", "sculpture")),

        Attraction("cd_opera_sichuan", "chengdu", "Opéra du Sichuan (Bianlian)", "Culture",
            150, 90, 4.7,
            "Spectacle traditionnel avec le célèbre 'changement de visage' instantané des masques.",
            "https://images.unsplash.com/photo-1544985361-b420d7a77043?w=400",
            30.6515, 104.0525, "20:00-21:30", "", 1, "indoor",
            listOf("soir"), listOf("opéra", "masque", "spectacle")),

        Attraction("cd_michelin", "chengdu", "Dîner Gastronomique 3 Étoiles", "Gastronomie",
            1600, 120, 4.9,
            "Menu dégustation exceptionnel revisitant les saveurs millénaires du Sichuan dans une villa historique privée.",
            "https://images.unsplash.com/photo-1555126634-323283e090fa?w=400",
            30.6570, 104.0660, "19:00-22:00", "", 1, "indoor",
            listOf("soir"), listOf("premium", "vip", "michelin", "gastronomie"))
    )

    private fun guilinAttractions() = listOf(
        Attraction("gl_riviere_li", "guilin", "Croisière Rivière Li", "Nature",
            210, 240, 4.9,
            "Croisière de Guilin à Yangshuo à travers les paysages karstiques les plus célèbres de Chine.",
            "https://images.unsplash.com/photo-1529921879218-f99546d03a24?w=400",
            25.2700, 110.2900, "8:30-16:00", "", 1, "outdoor",
            listOf("matin"), listOf("croisière", "karst", "rivière")),

        Attraction("gl_riziere_longji", "guilin", "Rizières en Terrasses de Longji", "Nature",
            80, 210, 4.8,
            "Rizières sculptées à flanc de montagne par les Zhuang et Yao, paysage changeant selon les saisons.",
            "https://images.unsplash.com/photo-1589308681559-4c1e71eb5263?w=400",
            25.7880, 110.1130, "8:00-18:00", "", 4, "outdoor",
            listOf("matin"), listOf("rizières", "ethnie", "montagne")),

        Attraction("gl_grotte_flute", "guilin", "Grotte de la Flûte de Roseau", "Nature",
            90, 60, 4.5,
            "Grotte illuminée naturellement sculptée avec stalactites et stalagmites spectaculaires.",
            "https://images.unsplash.com/photo-1504699439244-a03cadd68bba?w=400",
            25.2980, 110.2650, "8:00-17:30", "", 2, "indoor",
            listOf("apres-midi"), listOf("grotte", "géologie", "illumination")),

        Attraction("gl_colline_trompe", "guilin", "Colline de la Trompe d'Éléphant", "Nature",
            55, 45, 4.4,
            "Symbole de Guilin, colline en forme d'éléphant buvant au bord de la rivière Li.",
            "https://images.unsplash.com/photo-1518509562904-e7ef99cdbc86?w=400",
            25.2620, 110.3010, "6:30-19:00", "", 2, "outdoor",
            listOf("apres-midi"), listOf("symbole", "karst", "photo")),

        Attraction("gl_yangshuo", "guilin", "Yangshuo et la Rue de l'Ouest", "Loisirs",
            0, 120, 4.6,
            "Petite ville bohème au milieu des pitons karstiques, paradis des backpackers et de l'escalade.",
            "https://images.unsplash.com/photo-1494783367193-149034c05e8f?w=400",
            24.7655, 110.4890, "24/7", "", 2, "outdoor",
            listOf("apres-midi", "soir"), listOf("backpacker", "escalade", "bar")),

        Attraction("gl_spectacle_liu", "guilin", "Spectacle Impression Liu Sanjie", "Loisirs",
            200, 70, 4.7,
            "Spectacle en plein air de Zhang Yimou avec les montagnes karstiques comme décor naturel.",
            "https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=400",
            24.7540, 110.4820, "19:30-21:00", "", 1, "outdoor",
            listOf("soir"), listOf("spectacle", "Zhang Yimou", "plein-air")),
    )

    private fun parisAttractions() = listOf(
        Attraction("pa_tour_eiffel", "paris", "Tour Eiffel", "Monument",
            26, 120, 4.8,
            "Symbole universel de Paris, la Dame de Fer offre une vue panoramique incomparable sur la capitale depuis ses 3 étages.",
            "https://images.unsplash.com/photo-1511739001486-6bfe10ce785f?w=400",
            48.8584, 2.2945, "9:30-23:45", "", 3, "outdoor",
            listOf("matin", "soir"), listOf("iconique", "panorama", "monument")),

        Attraction("pa_louvre", "paris", "Musée du Louvre", "Culture",
            17, 180, 4.9,
            "Plus grand musée du monde, abritant la Joconde, la Vénus de Milo et 380 000 œuvres d'art.",
            "https://images.unsplash.com/photo-1499856871958-5b9627545d1a?w=400",
            48.8606, 2.3376, "9:00-18:00", "mardi", 2, "indoor",
            listOf("matin", "apres-midi"), listOf("musée", "art", "Joconde")),

        Attraction("pa_notre_dame", "paris", "Cathédrale Notre-Dame", "Monument",
            0, 60, 4.7,
            "Chef-d'œuvre de l'art gothique, récemment restaurée après l'incendie de 2019, au cœur de l'Île de la Cité.",
            "https://images.unsplash.com/photo-1478391679764-b2d8b3cd1e94?w=400",
            48.8530, 2.3499, "8:00-18:45", "", 2, "indoor",
            listOf("matin"), listOf("gothique", "cathédrale", "histoire")),

        Attraction("pa_montmartre", "paris", "Montmartre & Sacré-Cœur", "Culture",
            0, 120, 4.6,
            "Quartier bohème des artistes, basilique du Sacré-Cœur au sommet offrant une vue splendide sur Paris.",
            "https://images.unsplash.com/photo-1550340499-a6c60fc8287c?w=400",
            48.8867, 2.3431, "6:00-22:30", "", 3, "outdoor",
            listOf("matin", "apres-midi"), listOf("bohème", "artistes", "vue")),

        Attraction("pa_champs_elysees", "paris", "Champs-Élysées & Arc de Triomphe", "Loisirs",
            16, 90, 4.5,
            "La plus belle avenue du monde, du shopping de luxe à l'Arc de Triomphe avec vue sur les 12 avenues.",
            "https://images.unsplash.com/photo-1509439581779-6298f75bf6e5?w=400",
            48.8738, 2.2950, "24/7", "", 2, "outdoor",
            listOf("apres-midi"), listOf("shopping", "avenue", "luxe")),

        Attraction("pa_croissant", "paris", "Petit-déjeuner Parisien", "Gastronomie",
            15, 45, 4.4,
            "Croissant beurré, café crème et tartine dans un café typique parisien avec terrasse sur le trottoir.",
            "https://images.unsplash.com/photo-1555507036-ab1f4038024a?w=400",
            48.8530, 2.3470, "7:00-11:00", "", 1, "indoor",
            listOf("matin"), listOf("croissant", "café", "terrasse")),

        Attraction("pa_seine", "paris", "Croisière sur la Seine", "Loisirs",
            15, 75, 4.7,
            "Balade fluviale passant devant les plus beaux monuments de Paris, magique à la tombée de la nuit.",
            "https://images.unsplash.com/photo-1541452115-47969e5e9b1d?w=400",
            48.8600, 2.3510, "10:00-22:30", "", 1, "outdoor",
            listOf("apres-midi", "soir"), listOf("croisière", "Seine", "romantique")),

        Attraction("pa_bistrot", "paris", "Dîner Bistrot Traditionnel", "Gastronomie",
            35, 90, 4.6,
            "Cuisine française classique : confit de canard, steak-frites et crème brûlée dans un bistrot authentique.",
            "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=400",
            48.8540, 2.3380, "19:00-23:00", "", 1, "indoor",
            listOf("soir"), listOf("bistrot", "gastronomie", "vin")),

        Attraction("pa_yacht_vip", "paris", "Dîner Privé sur Yacht", "Loisirs",
            1200, 150, 4.9,
            "Croisière exclusive sur la Seine avec chef étoilé privé et champagne millésimé face à la Tour Eiffel scintillante.",
            "https://images.unsplash.com/photo-1541452115-47969e5e9b1d?w=400",
            48.8600, 2.3510, "20:00-23:00", "", 1, "outdoor",
            listOf("soir"), listOf("premium", "vip", "yacht", "luxe", "chef")),

        Attraction("pa_crazy_horse", "paris", "Cabaret VIP Crazy Horse", "Culture",
            850, 120, 4.8,
            "Spectacle de cabaret parisien mythique en loge VIP avec caviar et champagne grand cru classé.",
            "https://images.unsplash.com/photo-1544985361-b420d7a77043?w=400",
            48.8665, 2.3015, "20:00-00:00", "", 1, "indoor",
            listOf("soir"), listOf("premium", "vip", "cabaret", "spectacle"))
    )

    private fun lyonAttractions() = listOf(
        Attraction("ly_vieux_lyon", "lyon", "Vieux Lyon", "Culture",
            0, 120, 4.8,
            "Quartier Renaissance classé UNESCO, traboules secrètes, ruelles médiévales et bouchons lyonnais.",
            "https://images.unsplash.com/photo-1524396309943-e03f5249f002?w=400",
            45.7620, 4.8270, "24/7", "", 2, "outdoor",
            listOf("matin", "apres-midi"), listOf("UNESCO", "Renaissance", "traboule")),

        Attraction("ly_fourviere", "lyon", "Basilique de Fourvière", "Monument",
            0, 60, 4.7,
            "Basilique néo-byzantine dominant Lyon, panorama exceptionnel sur la ville et les Alpes par temps clair.",
            "https://images.unsplash.com/photo-1565019001609-3699fb5cf691?w=400",
            45.7622, 4.8225, "7:00-19:00", "", 3, "outdoor",
            listOf("matin"), listOf("basilique", "panorama", "symbole")),

        Attraction("ly_bouchon", "lyon", "Déjeuner Bouchon Lyonnais", "Gastronomie",
            30, 90, 4.6,
            "Cuisine lyonnaise authentique : quenelle, saucisson brioché, tablier de sapeur et tarte praline.",
            "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=400",
            45.7640, 4.8340, "12:00-14:00", "lundi", 1, "indoor",
            listOf("apres-midi"), listOf("bouchon", "gastronomie", "lyonnais")),

        Attraction("ly_confluence", "lyon", "Musée des Confluences", "Culture",
            12, 120, 4.5,
            "Architecture déconstructiviste spectaculaire, musée d'histoire naturelle et des civilisations.",
            "https://images.unsplash.com/photo-1580060405573-f4c24603b128?w=400",
            45.7326, 4.8181, "10:00-18:00", "lundi", 1, "indoor",
            listOf("apres-midi"), listOf("musée", "architecture", "science")),

        Attraction("ly_parc_tete_or", "lyon", "Parc de la Tête d'Or", "Nature",
            0, 90, 4.6,
            "Plus grand parc urbain de France, zoo gratuit, jardin botanique et lac pour canotage.",
            "https://images.unsplash.com/photo-1585320806297-9794b3e4eeae?w=400",
            45.7814, 4.8557, "6:30-22:30", "", 2, "outdoor",
            listOf("matin", "apres-midi"), listOf("parc", "zoo", "lac")),

        Attraction("ly_presquile", "lyon", "Presqu'île & Place Bellecour", "Loisirs",
            0, 60, 4.4,
            "Cœur commerçant de Lyon, plus grande place piétonne d'Europe, shopping et architecture Haussmannienne.",
            "https://images.unsplash.com/photo-1560969184-10fe8719e047?w=400",
            45.7578, 4.8320, "24/7", "", 1, "outdoor",
            listOf("apres-midi"), listOf("shopping", "place", "centre-ville")),
    )

    private fun niceAttractions() = listOf(
        Attraction("ni_promenade", "nice", "Promenade des Anglais", "Loisirs",
            0, 60, 4.8,
            "Célèbre boulevard en front de mer, 7 km de promenade face à la Baie des Anges et ses eaux turquoise.",
            "https://images.unsplash.com/photo-1491166617655-0723a0999cfc?w=400",
            43.6947, 7.2653, "24/7", "", 2, "outdoor",
            listOf("matin", "apres-midi"), listOf("plage", "mer", "promenade")),

        Attraction("ni_vieux_nice", "nice", "Vieux Nice", "Culture",
            0, 90, 4.7,
            "Dédale de ruelles colorées, marché aux fleurs du Cours Saleya, socca et pissaladière.",
            "https://images.unsplash.com/photo-1534766555764-ce878a857731?w=400",
            43.6971, 7.2756, "24/7", "", 1, "outdoor",
            listOf("matin", "apres-midi"), listOf("marché", "ruelles", "coloré")),

        Attraction("ni_colline_chateau", "nice", "Colline du Château", "Nature",
            0, 45, 4.6,
            "Parc verdoyant au sommet offrant la plus belle vue sur Nice, la Baie des Anges et le port.",
            "https://images.unsplash.com/photo-1533104816931-20fa691ff6ca?w=400",
            43.6952, 7.2819, "8:30-20:00", "", 3, "outdoor",
            listOf("matin"), listOf("panorama", "parc", "vue")),

        Attraction("ni_matisse", "nice", "Musée Matisse", "Culture",
            10, 90, 4.5,
            "Collection permanente de Henri Matisse dans une villa génoise du XVIIe siècle entourée d'oliviers.",
            "https://images.unsplash.com/photo-1577083552431-6e5fd01988ec?w=400",
            43.7195, 7.2757, "10:00-18:00", "mardi", 1, "indoor",
            listOf("apres-midi"), listOf("art", "Matisse", "musée")),

        Attraction("ni_socca", "nice", "Dégustation de Socca", "Gastronomie",
            5, 30, 4.5,
            "Galette de pois chiches croustillante cuite au feu de bois, spécialité emblématique de Nice.",
            "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=400",
            43.6960, 7.2750, "8:00-14:00", "lundi", 1, "outdoor",
            listOf("matin"), listOf("socca", "street-food", "tradition")),

        Attraction("ni_plage", "nice", "Plage de Nice", "Nature",
            0, 120, 4.4,
            "Plages de galets face à la mer Méditerranée, eau cristalline et ambiance Riviera.",
            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400",
            43.6935, 7.2580, "24/7", "", 1, "outdoor",
            listOf("apres-midi"), listOf("plage", "mer", "détente")),

        Attraction("ni_yacht_monaco", "nice", "Location Yacht Privé & Hélicoptère", "Loisirs",
            3500, 240, 4.9,
            "Demi-journée en yacht de luxe vers Monaco, suivi d'un transfert retour sur Nice en hélicoptère privé.",
            "https://images.unsplash.com/photo-1491166617655-0723a0999cfc?w=400",
            43.6960, 7.2830, "10:00-18:00", "", 1, "outdoor",
            listOf("matin", "apres-midi"), listOf("premium", "vip", "yacht", "hélicoptère", "Monaco"))
    )

    private fun marseilleAttractions() = listOf(
        Attraction("ma_notre_dame", "marseille", "Notre-Dame de la Garde", "Monument",
            0, 60, 4.8,
            "Basilique emblématique au sommet de la plus haute colline, la 'Bonne Mère' veille sur Marseille et la mer.",
            "https://images.unsplash.com/photo-1589394815804-964ed0be2eb5?w=400",
            43.2842, 5.3712, "7:00-19:00", "", 3, "outdoor",
            listOf("matin"), listOf("basilique", "panorama", "symbole")),

        Attraction("ma_vieux_port", "marseille", "Vieux-Port", "Loisirs",
            0, 90, 4.7,
            "Cœur historique de Marseille depuis 2600 ans, marché aux poissons, cafés et vue sur le Fort Saint-Jean.",
            "https://images.unsplash.com/photo-1560969184-10fe8719e047?w=400",
            43.2951, 5.3743, "24/7", "", 1, "outdoor",
            listOf("matin", "soir"), listOf("port", "histoire", "ambiance")),

        Attraction("ma_calanques", "marseille", "Parc National des Calanques", "Nature",
            0, 240, 4.9,
            "Fjords calcaires spectaculaires plongeant dans la Méditerranée, eaux turquoise, randonnée et baignade.",
            "https://images.unsplash.com/photo-1571003123894-1f0594d2b5d9?w=400",
            43.2120, 5.4320, "6:00-19:00", "", 5, "outdoor",
            listOf("matin"), listOf("calanques", "randonnée", "nature")),

        Attraction("ma_bouillabaisse", "marseille", "Bouillabaisse Traditionnelle", "Gastronomie",
            45, 90, 4.6,
            "Soupe de poisson provençale mythique avec rouille et croûtons, servie dans les restaurants du Vieux-Port.",
            "https://images.unsplash.com/photo-1565299624946-b28f40a0ae38?w=400",
            43.2940, 5.3730, "12:00-14:30", "", 1, "indoor",
            listOf("apres-midi"), listOf("bouillabaisse", "poisson", "provençal")),

        Attraction("ma_mucem", "marseille", "MuCEM", "Culture",
            11, 120, 4.6,
            "Musée des Civilisations dans un cube de béton et verre ajouré, relié au Fort Saint-Jean par une passerelle.",
            "https://images.unsplash.com/photo-1580060405573-f4c24603b128?w=400",
            43.2967, 5.3608, "10:00-19:00", "mardi", 1, "indoor",
            listOf("apres-midi"), listOf("musée", "architecture", "civilisation")),

        Attraction("ma_frioul", "marseille", "Îles du Frioul & Château d'If", "Nature",
            10, 180, 4.5,
            "Archipel sauvage face à Marseille, forteresse du Comte de Monte-Cristo et criques préservées.",
            "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400",
            43.2800, 5.3250, "9:00-17:00", "", 2, "outdoor",
            listOf("matin"), listOf("île", "Dumas", "bateau")),
    )

    private fun bordeauxAttractions() = listOf(
        Attraction("bo_cite_vin", "bordeaux", "Cité du Vin", "Culture",
            22, 120, 4.7,
            "Musée interactif dédié au vin du monde, architecture unique en forme de carafe, dégustation au belvédère.",
            "https://images.unsplash.com/photo-1506377247377-2a5b3b417ebb?w=400",
            44.8625, -0.5503, "10:00-19:00", "lundi", 1, "indoor",
            listOf("apres-midi"), listOf("vin", "musée", "dégustation")),

        Attraction("bo_place_bourse", "bordeaux", "Place de la Bourse & Miroir d'Eau", "Monument",
            0, 45, 4.8,
            "Place XVIIIe siècle se reflétant dans le plus grand miroir d'eau du monde, spectacle magique de nuit.",
            "https://images.unsplash.com/photo-1559128010-7c1ad6e1b6a5?w=400",
            44.8413, -0.5696, "24/7", "", 1, "outdoor",
            listOf("apres-midi", "soir"), listOf("miroir", "architecture", "photo")),

        Attraction("bo_saint_emilion", "bordeaux", "Saint-Émilion", "Nature",
            0, 240, 4.9,
            "Village médiéval classé UNESCO au cœur des vignobles, églises monolithes et dégustations de grands crus.",
            "https://images.unsplash.com/photo-1506377247377-2a5b3b417ebb?w=400",
            44.8942, -0.1556, "24/7", "", 3, "outdoor",
            listOf("matin"), listOf("UNESCO", "vignoble", "médiéval")),

        Attraction("bo_canele", "bordeaux", "Dégustation de Canelés", "Gastronomie",
            5, 30, 4.6,
            "Petit gâteau croustillant au rhum et vanille, spécialité emblématique de Bordeaux depuis le XVIIIe.",
            "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=400",
            44.8378, -0.5730, "8:00-19:00", "", 1, "indoor",
            listOf("matin", "apres-midi"), listOf("canelé", "pâtisserie", "rhum")),

        Attraction("bo_darwin", "bordeaux", "Écosystème Darwin", "Loisirs",
            0, 90, 4.4,
            "Ancienne caserne militaire reconvertie en lieu alternatif éco-responsable, street art, skatepark et restaurants bio.",
            "https://images.unsplash.com/photo-1560969184-10fe8719e047?w=400",
            44.8490, -0.5472, "7:00-2:00", "", 1, "outdoor",
            listOf("apres-midi"), listOf("alternatif", "street-art", "bio")),

        Attraction("bo_jardin_public", "bordeaux", "Jardin Public", "Nature",
            0, 60, 4.5,
            "Élégant jardin à la française et à l'anglaise, havre de paix au cœur de Bordeaux avec canards et cygnes.",
            "https://images.unsplash.com/photo-1585320806297-9794b3e4eeae?w=400",
            44.8490, -0.5792, "7:00-21:00", "", 1, "outdoor",
            listOf("matin", "apres-midi"), listOf("jardin", "parc", "détente")),
    )

    private fun supplementalAttractions() = listOf(
        quickAttraction("pk_lama_temple", "pekin", "Temple des Lamas", "Culture", 25, 75, 4.6, 39.9475, 116.4172, "matin", "both"),
        quickAttraction("pk_798_art", "pekin", "Quartier d'Art 798", "Photo", 0, 90, 4.4, 39.9841, 116.4950, "apres-midi", "both"),
        quickAttraction("pk_beihai", "pekin", "Parc Beihai", "Nature", 10, 75, 4.5, 39.9255, 116.3896, "apres-midi", "outdoor"),
        quickAttraction("pk_wangfujing", "pekin", "Wangfujing", "Shopping", 80, 90, 4.3, 39.9138, 116.4117, "soir", "both"),

        quickAttraction("xa_bell_tower", "xian", "Tour de la Cloche", "Monument", 30, 60, 4.6, 34.2610, 108.9424, "matin", "both"),
        quickAttraction("xa_yongxingfang", "xian", "Yongxingfang", "Gastronomie", 45, 75, 4.4, 34.2712, 108.9622, "apres-midi", "both"),
        quickAttraction("xa_tang_paradise", "xian", "Tang Paradise", "Loisirs", 120, 120, 4.5, 34.2159, 108.9865, "soir", "outdoor"),
        quickAttraction("xa_huaqing", "xian", "Palais Huaqing", "Culture", 120, 120, 4.5, 34.3596, 109.2142, "matin", "outdoor"),

        quickAttraction("hz_xixi", "hangzhou", "Zone Humide Xixi", "Nature", 80, 150, 4.6, 30.2670, 120.0606, "matin", "outdoor"),
        quickAttraction("hz_tea_museum", "hangzhou", "Musee National du The", "Culture", 0, 75, 4.5, 30.2366, 120.1074, "apres-midi", "both"),
        quickAttraction("hz_qinghefang", "hangzhou", "Rue Qinghefang", "Shopping", 50, 90, 4.3, 30.2445, 120.1712, "soir", "both"),
        quickAttraction("hz_grand_canal", "hangzhou", "Grand Canal", "Loisirs", 60, 90, 4.4, 30.3175, 120.1414, "soir", "outdoor"),

        quickAttraction("cd_people_park", "chengdu", "Parc du Peuple", "Loisirs", 20, 75, 4.5, 30.6597, 104.0631, "matin", "outdoor"),
        quickAttraction("cd_kuanzhai", "chengdu", "Ruelles Kuan Zhai", "Shopping", 40, 90, 4.4, 30.6694, 104.0570, "apres-midi", "both"),
        quickAttraction("cd_opera_sichuan", "chengdu", "Opera du Sichuan", "Culture", 160, 90, 4.7, 30.6580, 104.0648, "soir", "indoor"),
        quickAttraction("cd_taikoo", "chengdu", "Taikoo Li", "Shopping", 120, 90, 4.5, 30.6535, 104.0808, "soir", "both"),

        quickAttraction("gl_elephant_hill", "guilin", "Colline de la Trompe d'Elephant", "Nature", 55, 75, 4.5, 25.2650, 110.2950, "matin", "outdoor"),
        quickAttraction("gl_reed_flute", "guilin", "Grotte de la Flute de Roseau", "Nature", 90, 90, 4.4, 25.3057, 110.2767, "apres-midi", "indoor"),
        quickAttraction("gl_two_rivers", "guilin", "Deux Rivieres et Quatre Lacs", "Loisirs", 120, 90, 4.6, 25.2760, 110.2920, "soir", "outdoor"),
        quickAttraction("gl_east_west_alley", "guilin", "East West Alley", "Gastronomie", 45, 75, 4.3, 25.2822, 110.2960, "soir", "both"),

        quickAttraction("pa_orsay", "paris", "Musee d'Orsay", "Culture", 16, 120, 4.8, 48.8600, 2.3266, "matin", "indoor"),
        quickAttraction("pa_luxembourg", "paris", "Jardin du Luxembourg", "Nature", 0, 60, 4.7, 48.8462, 2.3372, "apres-midi", "outdoor"),
        quickAttraction("pa_sainte_chapelle", "paris", "Sainte-Chapelle", "Monument", 13, 60, 4.8, 48.8554, 2.3450, "apres-midi", "indoor"),
        quickAttraction("pa_canal_saint_martin", "paris", "Canal Saint-Martin", "Loisirs", 35, 90, 4.4, 48.8708, 2.3650, "soir", "outdoor"),

        quickAttraction("ly_parc_tete_or", "lyon", "Parc de la Tete d'Or", "Nature", 0, 90, 4.7, 45.7772, 4.8555, "matin", "outdoor"),
        quickAttraction("ly_halles_bocuse", "lyon", "Halles Paul Bocuse", "Gastronomie", 35, 75, 4.6, 45.7637, 4.8506, "apres-midi", "indoor"),
        quickAttraction("ly_theatre_romain", "lyon", "Theatres Romains", "Monument", 0, 60, 4.5, 45.7599, 4.8199, "apres-midi", "outdoor"),
        quickAttraction("ly_presquile", "lyon", "Presqu'ile", "Shopping", 70, 90, 4.4, 45.7605, 4.8357, "soir", "both"),

        quickAttraction("ni_chateau", "nice", "Colline du Chateau", "Nature", 0, 75, 4.8, 43.6957, 7.2800, "matin", "outdoor"),
        quickAttraction("ni_cours_saleya", "nice", "Cours Saleya", "Gastronomie", 25, 75, 4.5, 43.6955, 7.2760, "matin", "both"),
        quickAttraction("ni_matisse", "nice", "Musee Matisse", "Culture", 10, 90, 4.4, 43.7195, 7.2760, "apres-midi", "indoor"),
        quickAttraction("ni_port_lympia", "nice", "Port Lympia", "Loisirs", 45, 75, 4.4, 43.6974, 7.2850, "soir", "outdoor"),

        quickAttraction("ma_panier", "marseille", "Le Panier", "Photo", 0, 75, 4.5, 43.2992, 5.3674, "matin", "outdoor"),
        quickAttraction("ma_longchamp", "marseille", "Palais Longchamp", "Monument", 8, 90, 4.6, 43.3049, 5.3941, "apres-midi", "both"),
        quickAttraction("ma_noailles", "marseille", "Noailles", "Gastronomie", 30, 75, 4.3, 43.2962, 5.3800, "apres-midi", "both"),
        quickAttraction("ma_corniche", "marseille", "Corniche Kennedy", "Nature", 35, 90, 4.6, 43.2808, 5.3560, "soir", "outdoor"),

        quickAttraction("bo_musee_aquitaine", "bordeaux", "Musee d'Aquitaine", "Culture", 8, 90, 4.5, 44.8351, -0.5752, "matin", "indoor"),
        quickAttraction("bo_sainte_catherine", "bordeaux", "Rue Sainte-Catherine", "Shopping", 60, 90, 4.3, 44.8390, -0.5740, "apres-midi", "both"),
        quickAttraction("bo_quais", "bordeaux", "Quais de Garonne", "Loisirs", 0, 75, 4.6, 44.8460, -0.5650, "soir", "outdoor"),
        quickAttraction("bo_bassin_lumieres", "bordeaux", "Bassins des Lumieres", "Culture", 16, 90, 4.7, 44.8655, -0.5585, "soir", "indoor")
    )

    private fun quickAttraction(
        id: String,
        destinationId: String,
        name: String,
        type: String,
        cost: Int,
        duration: Int,
        rating: Double,
        lat: Double,
        lng: Double,
        slot: String,
        weatherType: String
    ) = Attraction(
        id, destinationId, name, type,
        cost, duration, rating,
        "Point d'interet supplementaire pour diversifier les itineraires par prix, duree et moment de la journee.",
        imageForType(type),
        lat, lng, "9:00-21:00", "", 1, weatherType,
        listOf(slot), listOf(type.lowercase(), slot, "supplementaire")
    )

    private fun imageForType(type: String): String {
        return when (type) {
            "Culture" -> "https://images.unsplash.com/photo-1543349689-9a4d426bee8e?w=400"
            "Gastronomie" -> "https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400"
            "Nature" -> "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?w=400"
            "Shopping" -> "https://images.unsplash.com/photo-1519567770579-c2fc5436bcf9?w=400"
            "Photo" -> "https://images.unsplash.com/photo-1518005020951-eccb494ad742?w=400"
            else -> "https://images.unsplash.com/photo-1518998053901-5348d3961a04?w=400"
        }
    }

    private fun Attraction.withGooglePlaceImages(): Attraction {
        val images = googleStreetViewImages(lat, lng)
        return copy(
            imageUrl = images.firstOrNull() ?: imageUrl,
            imageUrls = images.ifEmpty { imageUrls.ifEmpty { listOf(imageUrl).filter { it.isNotBlank() } } }
        )
    }

    private fun googleStreetViewImages(lat: Double, lng: Double): List<String> {
        if (lat == 0.0 && lng == 0.0) return emptyList()
        val apiKey = com.example.traveling.BuildConfig.MAPS_API_KEY
        if (apiKey.isBlank()) return emptyList()
        return listOf(0, 120, 240).map { heading ->
            "https://maps.googleapis.com/maps/api/streetview" +
                "?size=900x600" +
                "&location=$lat,$lng" +
                "&heading=$heading" +
                "&pitch=5" +
                "&fov=80" +
                "&source=outdoor" +
                "&key=$apiKey"
        }
    }

    private fun clearCollection(name: String, onComplete: () -> Unit) {
        db.collection(name).get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit()
                    .addOnSuccessListener {
                        Log.d(TAG, "🗑️ Collection '$name' vidée")
                        onComplete()
                    }
                    .addOnFailureListener { Log.e(TAG, "Erreur clear $name", it) }
            }
            .addOnFailureListener { Log.e(TAG, "Erreur lecture $name", it) }
    }
}
