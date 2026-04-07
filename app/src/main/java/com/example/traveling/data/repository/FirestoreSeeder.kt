package com.example.traveling.data.repository

import android.util.Log
import com.example.traveling.data.model.Attraction
import com.example.traveling.data.model.Destination
import com.google.firebase.firestore.FirebaseFirestore

/**
 * 用于一次性向 Firestore 批量写入预设数据的工具类。
 * 在 App 中调用一次 seedAll() 即可，成功后可以删除调用。
 *
 * 使用方法:
 *   FirestoreSeeder.seedAll()     // 写入所有数据 (destinations + attractions)
 *   FirestoreSeeder.seedAll(true) // 清空后重新写入
 */
object FirestoreSeeder {

    private val db = FirebaseFirestore.getInstance()
    private const val TAG = "FirestoreSeeder"

    // ═══════════════════════════════════════════════════
    //  PUBLIC API
    // ═══════════════════════════════════════════════════

    /**
     * 批量写入所有预设数据
     * @param clearFirst 是否先清空已有数据再写入
     */
    fun seedAll(clearFirst: Boolean = false) {
        if (clearFirst) {
            clearCollection("destinations") { seedDestinations() }
            clearCollection("attractions") { seedAttractions() }
        } else {
            seedDestinations()
            seedAttractions()
        }
    }

    // ═══════════════════════════════════════════════════
    //  DESTINATIONS — 5 个中国城市
    // ═══════════════════════════════════════════════════

    private fun seedDestinations() {
        val destinations = listOf(
            Destination("pekin", "Pékin", "Chine",
                "Capitale millénaire, cœur politique et culturel de la Chine avec ses palais impériaux et ses hutongs.",
                "https://images.unsplash.com/photo-1603120527222-33f28c2ce89e?w=800", 39.9042, 116.4074),
            Destination("xian", "Xi'an", "Chine",
                "Ancienne capitale de la Route de la Soie, célèbre pour l'Armée de Terre Cuite et les remparts Ming.",
                "https://images.unsplash.com/photo-1558507564-c573429b9ceb?w=800", 34.3416, 108.9398),
            Destination("hangzhou", "Hangzhou", "Chine",
                "Ville du Lac de l'Ouest, paradis terrestre célébré par Marco Polo, capitale du thé Longjing.",
                "https://images.unsplash.com/photo-1647067151201-0b37c7555870?w=800", 30.2741, 120.1551),
            Destination("chengdu", "Chengdu", "Chine",
                "Capitale du Sichuan, patrie des pandas géants et de la cuisine épicée, ville classée UNESCO.",
                "https://images.unsplash.com/photo-1564349683136-77e08dba1ef7?w=800", 30.5728, 104.0668),
            Destination("guilin", "Guilin", "Chine",
                "Paysages karstiques iconiques de la rivière Li, montagnes en pain de sucre et rizières en terrasses.",
                "https://images.unsplash.com/photo-1513415564515-763d91423bdd?w=800", 25.2744, 110.2990),
        )

        val batch = db.batch()
        destinations.forEach { dest ->
            val ref = db.collection("destinations").document(dest.id)
            batch.set(ref, dest)
        }
        batch.commit()
            .addOnSuccessListener { Log.d(TAG, "✅ ${destinations.size} destinations écrites") }
            .addOnFailureListener { Log.e(TAG, "❌ Erreur destinations", it) }
    }

    // ═══════════════════════════════════════════════════
    //  ATTRACTIONS — 景点数据 (每个城市 6-8 个)
    // ═══════════════════════════════════════════════════

    private fun seedAttractions() {
        val attractions = buildList {
            // ─── PÉKIN (北京) ───
            addAll(pekinAttractions())
            // ─── XI'AN (西安) ───
            addAll(xianAttractions())
            // ─── HANGZHOU (杭州) ───
            addAll(hangzhouAttractions())
            // ─── CHENGDU (成都) ───
            addAll(chengduAttractions())
            // ─── GUILIN (桂林) ───
            addAll(guilinAttractions())
        }

        // Firestore 批量写入限制 500 条/batch，这里数据量在 40 条以内，可以一次性写入
        val batch = db.batch()
        attractions.forEach { attr ->
            val ref = db.collection("attractions").document(attr.id)
            batch.set(ref, attr)
        }
        batch.commit()
            .addOnSuccessListener { Log.d(TAG, "✅ ${attractions.size} attractions écrites") }
            .addOnFailureListener { Log.e(TAG, "❌ Erreur attractions", it) }
    }

    // ─────────────────────────────────────
    //  北京景点
    // ─────────────────────────────────────
    private fun pekinAttractions() = listOf(
        Attraction("pk_cite_interdite", "pekin", "Cité Interdite", "Culture",
            60, 150, 4.9,
            "Le plus grand complexe de palais antiques au monde, 600 ans d'histoire impériale, un million de trésors nationaux.",
            "https://images.unsplash.com/photo-1603120527222-33f28c2ce89e?w=400",
            39.9163, 116.3972, "8:30-17:00", "lundi", 3, "outdoor",
            listOf("matin"), listOf("historique", "UNESCO", "palais")),

        Attraction("pk_jingshan", "pekin", "Parc Jingshan", "Nature",
            2, 45, 4.5,
            "Vue panoramique sur la Cité Interdite depuis le sommet, meilleur point de vue sur l'axe central de Pékin.",
            "https://images.unsplash.com/photo-1687524669097-7fc9b204f92f?w=400",
            39.9250, 116.3965, "6:00-21:00", "", 2, "outdoor",
            listOf("matin"), listOf("panorama", "parc", "colline")),

        Attraction("pk_quanjude", "pekin", "Canard Laqué Quanjude", "Gastronomie",
            180, 75, 4.3,
            "Restaurant centenaire, authentique canard laqué de Pékin, peau croustillante et viande tendre.",
            "https://images.unsplash.com/photo-1672891197847-d3a65c11b33d?w=400",
            39.8987, 116.3948, "11:00-22:00", "", 1, "indoor",
            listOf("apres-midi"), listOf("gastronomie", "canard", "centenaire")),

        Attraction("pk_temple_ciel", "pekin", "Temple du Ciel", "Monument",
            34, 120, 4.7,
            "Lieu de culte impérial des Ming et Qing, le Hall de la Prière est un chef-d'œuvre architectural chinois.",
            "https://images.unsplash.com/photo-1709133332724-2f56232c77eb?w=400",
            39.8822, 116.4066, "6:00-22:00", "", 2, "outdoor",
            listOf("apres-midi"), listOf("temple", "UNESCO", "architecture")),

        Attraction("pk_shichahai", "pekin", "Shichahai", "Loisirs",
            0, 60, 4.4,
            "Culture traditionnelle des Hutong, promenade au bord du lac, ambiance populaire de Pékin.",
            "https://images.unsplash.com/photo-1772490184794-4ae50d917785?w=400",
            39.9390, 116.3836, "24/7", "", 2, "outdoor",
            listOf("apres-midi", "soir"), listOf("hutong", "lac", "promenade")),

        Attraction("pk_nanluoguxiang", "pekin", "Nanluoguxiang", "Culture",
            0, 90, 4.6,
            "Hutong le mieux préservé de l'époque Yuan, rue piétonne de boutiques artisanales et de street food.",
            "https://images.unsplash.com/photo-1659466248885-8b7a03205661?w=400",
            39.9370, 116.4030, "24/7", "", 1, "outdoor",
            listOf("apres-midi", "soir"), listOf("hutong", "shopping", "street-food")),

        Attraction("pk_fondue_mouton", "pekin", "Fondue au Mouton Pékinoise", "Gastronomie",
            120, 90, 4.5,
            "Fondue traditionnelle au pot de cuivre, sauce sésame et ail sucré, goût authentique de Pékin.",
            "https://images.unsplash.com/photo-1755710116297-20f8e42d3c28?w=400",
            39.9310, 116.4050, "11:00-23:00", "", 1, "indoor",
            listOf("soir"), listOf("gastronomie", "fondue", "traditionnel")),

        Attraction("pk_grande_muraille", "pekin", "Grande Muraille (Badaling)", "Monument",
            40, 180, 4.8,
            "Section la plus célèbre de la Grande Muraille, vue spectaculaire sur les montagnes à perte de vue.",
            "https://images.unsplash.com/photo-1558507564-c573429b9ceb?w=400",
            40.3541, 116.0137, "6:30-19:00", "", 5, "outdoor",
            listOf("matin"), listOf("UNESCO", "merveille", "randonnée")),
    )

    // ─────────────────────────────────────
    //  西安景点
    // ─────────────────────────────────────
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

    // ─────────────────────────────────────
    //  杭州景点
    // ─────────────────────────────────────
    private fun hangzhouAttractions() = listOf(
        Attraction("hz_lac_ouest", "hangzhou", "Lac de l'Ouest", "Nature",
            0, 120, 4.9,
            "Lac emblématique inscrit à l'UNESCO, pagodes, ponts en zigzag et jardins classiques chinois.",
            "https://images.unsplash.com/photo-1647067151201-0b37c7555870?w=400",
            30.2500, 120.1300, "24/7", "", 2, "outdoor",
            listOf("matin", "apres-midi"), listOf("UNESCO", "lac", "jardin")),

        Attraction("hz_lingyin", "hangzhou", "Temple Lingyin", "Culture",
            75, 90, 4.7,
            "Temple bouddhiste millénaire niché dans la forêt, sculptures rupestres de Feilai Feng.",
            "https://images.unsplash.com/photo-1598887142487-3c854d51eabb?w=400",
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

    // ─────────────────────────────────────
    //  成都景点
    // ─────────────────────────────────────
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
            "https://images.unsplash.com/photo-1584808006861-6f89e0e4cf10?w=400",
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
    )

    // ─────────────────────────────────────
    //  桂林景点
    // ─────────────────────────────────────
    private fun guilinAttractions() = listOf(
        Attraction("gl_riviere_li", "guilin", "Croisière Rivière Li", "Nature",
            210, 240, 4.9,
            "Croisière de Guilin à Yangshuo à travers les paysages karstiques les plus célèbres de Chine.",
            "https://images.unsplash.com/photo-1537531383496-f4749ef56982?w=400",
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
            "https://images.unsplash.com/photo-1513415564515-763d91423bdd?w=400",
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

    // ═══════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════

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
                    .addOnFailureListener { Log.e(TAG, "❌ Erreur clear $name", it) }
            }
            .addOnFailureListener { Log.e(TAG, "❌ Erreur lecture $name", it) }
    }
}
