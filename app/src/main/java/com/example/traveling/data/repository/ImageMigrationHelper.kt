package com.example.traveling.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * One-time migration helper:
 * 1. Downloads images from reliable source URLs
 * 2. Uploads them to Firebase Storage (images/destinations/ & images/attractions/)
 * 3. Updates Firestore documents with the Firebase Storage download URLs
 *
 * Run once, then remove the trigger.
 */
object ImageMigrationHelper {

    private const val TAG = "ImageMigration"
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // ─── Source image URLs (unique per entity, from picsum.photos with seeds) ───

    private val destinationImages = mapOf(
        "pekin"     to "https://images.pexels.com/photos/2412603/pexels-photo-2412603.jpeg?auto=compress&cs=tinysrgb&w=800",
        "xian"      to "https://images.pexels.com/photos/6152257/pexels-photo-6152257.jpeg?auto=compress&cs=tinysrgb&w=800",
        "hangzhou"  to "https://images.pexels.com/photos/2563681/pexels-photo-2563681.jpeg?auto=compress&cs=tinysrgb&w=800",
        "chengdu"   to "https://images.pexels.com/photos/3802510/pexels-photo-3802510.jpeg?auto=compress&cs=tinysrgb&w=800",
        "guilin"    to "https://images.pexels.com/photos/2387871/pexels-photo-2387871.jpeg?auto=compress&cs=tinysrgb&w=800",
        "paris"     to "https://images.pexels.com/photos/338515/pexels-photo-338515.jpeg?auto=compress&cs=tinysrgb&w=800",
        "lyon"      to "https://images.pexels.com/photos/3573382/pexels-photo-3573382.jpeg?auto=compress&cs=tinysrgb&w=800",
        "nice"      to "https://images.pexels.com/photos/4353813/pexels-photo-4353813.jpeg?auto=compress&cs=tinysrgb&w=800",
        "marseille" to "https://images.pexels.com/photos/4353229/pexels-photo-4353229.jpeg?auto=compress&cs=tinysrgb&w=800",
        "bordeaux"  to "https://images.pexels.com/photos/2889685/pexels-photo-2889685.jpeg?auto=compress&cs=tinysrgb&w=800",
    )

    private val attractionImages = mapOf(
        // ── Pékin ──
        "pk_cite_interdite"  to "https://images.pexels.com/photos/2846034/pexels-photo-2846034.jpeg?auto=compress&cs=tinysrgb&w=600",
        "pk_jingshan"        to "https://images.pexels.com/photos/3408744/pexels-photo-3408744.jpeg?auto=compress&cs=tinysrgb&w=600",
        "pk_quanjude"        to "https://images.pexels.com/photos/2313686/pexels-photo-2313686.jpeg?auto=compress&cs=tinysrgb&w=600",
        "pk_temple_ciel"     to "https://images.pexels.com/photos/6152103/pexels-photo-6152103.jpeg?auto=compress&cs=tinysrgb&w=600",
        "pk_shichahai"       to "https://images.pexels.com/photos/2846076/pexels-photo-2846076.jpeg?auto=compress&cs=tinysrgb&w=600",
        "pk_nanluoguxiang"   to "https://images.pexels.com/photos/5563472/pexels-photo-5563472.jpeg?auto=compress&cs=tinysrgb&w=600",
        "pk_fondue_mouton"   to "https://images.pexels.com/photos/3659862/pexels-photo-3659862.jpeg?auto=compress&cs=tinysrgb&w=600",
        "pk_grande_muraille" to "https://images.pexels.com/photos/2412166/pexels-photo-2412166.jpeg?auto=compress&cs=tinysrgb&w=600",
        "pk_helicoptere"     to "https://images.pexels.com/photos/3768146/pexels-photo-3768146.jpeg?auto=compress&cs=tinysrgb&w=600",
        // ── Xi'an ──
        "xa_terrecuite"      to "https://images.pexels.com/photos/6152257/pexels-photo-6152257.jpeg?auto=compress&cs=tinysrgb&w=600",
        "xa_remparts"        to "https://images.pexels.com/photos/5563610/pexels-photo-5563610.jpeg?auto=compress&cs=tinysrgb&w=600",
        "xa_grande_pagode"   to "https://images.pexels.com/photos/6152099/pexels-photo-6152099.jpeg?auto=compress&cs=tinysrgb&w=600",
        "xa_quartier_musulman" to "https://images.pexels.com/photos/2641886/pexels-photo-2641886.jpeg?auto=compress&cs=tinysrgb&w=600",
        "xa_grande_mosquee"  to "https://images.pexels.com/photos/3689859/pexels-photo-3689859.jpeg?auto=compress&cs=tinysrgb&w=600",
        "xa_mont_huashan"    to "https://images.pexels.com/photos/2835436/pexels-photo-2835436.jpeg?auto=compress&cs=tinysrgb&w=600",
        // ── Hangzhou ──
        "hz_lac_ouest"       to "https://images.pexels.com/photos/2563681/pexels-photo-2563681.jpeg?auto=compress&cs=tinysrgb&w=600",
        "hz_lingyin"         to "https://images.pexels.com/photos/5563468/pexels-photo-5563468.jpeg?auto=compress&cs=tinysrgb&w=600",
        "hz_longjing"        to "https://images.pexels.com/photos/1417945/pexels-photo-1417945.jpeg?auto=compress&cs=tinysrgb&w=600",
        "hz_pagode_leifeng"  to "https://images.pexels.com/photos/3408354/pexels-photo-3408354.jpeg?auto=compress&cs=tinysrgb&w=600",
        "hz_rue_hefang"      to "https://images.pexels.com/photos/3408353/pexels-photo-3408353.jpeg?auto=compress&cs=tinysrgb&w=600",
        "hz_impression_ouest" to "https://images.pexels.com/photos/1190298/pexels-photo-1190298.jpeg?auto=compress&cs=tinysrgb&w=600",
        // ── Chengdu ──
        "cd_pandas"          to "https://images.pexels.com/photos/3608263/pexels-photo-3608263.jpeg?auto=compress&cs=tinysrgb&w=600",
        "cd_jinli"           to "https://images.pexels.com/photos/5563471/pexels-photo-5563471.jpeg?auto=compress&cs=tinysrgb&w=600",
        "cd_hotpot"          to "https://images.pexels.com/photos/2474661/pexels-photo-2474661.jpeg?auto=compress&cs=tinysrgb&w=600",
        "cd_wuhou"           to "https://images.pexels.com/photos/3408356/pexels-photo-3408356.jpeg?auto=compress&cs=tinysrgb&w=600",
        "cd_leshan"          to "https://images.pexels.com/photos/5563605/pexels-photo-5563605.jpeg?auto=compress&cs=tinysrgb&w=600",
        "cd_opera_sichuan"   to "https://images.pexels.com/photos/2263436/pexels-photo-2263436.jpeg?auto=compress&cs=tinysrgb&w=600",
        "cd_michelin"        to "https://images.pexels.com/photos/1640777/pexels-photo-1640777.jpeg?auto=compress&cs=tinysrgb&w=600",
        // ── Guilin ──
        "gl_riviere_li"      to "https://images.pexels.com/photos/2387871/pexels-photo-2387871.jpeg?auto=compress&cs=tinysrgb&w=600",
        "gl_riziere_longji"  to "https://images.pexels.com/photos/2589454/pexels-photo-2589454.jpeg?auto=compress&cs=tinysrgb&w=600",
        "gl_grotte_flute"    to "https://images.pexels.com/photos/2108845/pexels-photo-2108845.jpeg?auto=compress&cs=tinysrgb&w=600",
        "gl_colline_trompe"  to "https://images.pexels.com/photos/2387873/pexels-photo-2387873.jpeg?auto=compress&cs=tinysrgb&w=600",
        "gl_yangshuo"        to "https://images.pexels.com/photos/2387869/pexels-photo-2387869.jpeg?auto=compress&cs=tinysrgb&w=600",
        "gl_spectacle_liu"   to "https://images.pexels.com/photos/2263436/pexels-photo-2263436.jpeg?auto=compress&cs=tinysrgb&w=600",
        // ── Paris ──
        "pa_tour_eiffel"     to "https://images.pexels.com/photos/699466/pexels-photo-699466.jpeg?auto=compress&cs=tinysrgb&w=600",
        "pa_louvre"          to "https://images.pexels.com/photos/2363/france-landmark-lights-night.jpg?auto=compress&cs=tinysrgb&w=600",
        "pa_notre_dame"      to "https://images.pexels.com/photos/2344/cars-france-landmark-lights.jpg?auto=compress&cs=tinysrgb&w=600",
        "pa_montmartre"      to "https://images.pexels.com/photos/2082103/pexels-photo-2082103.jpeg?auto=compress&cs=tinysrgb&w=600",
        "pa_champs_elysees"  to "https://images.pexels.com/photos/1850619/pexels-photo-1850619.jpeg?auto=compress&cs=tinysrgb&w=600",
        "pa_croissant"       to "https://images.pexels.com/photos/1775043/pexels-photo-1775043.jpeg?auto=compress&cs=tinysrgb&w=600",
        "pa_seine"           to "https://images.pexels.com/photos/2738173/pexels-photo-2738173.jpeg?auto=compress&cs=tinysrgb&w=600",
        "pa_bistrot"         to "https://images.pexels.com/photos/67468/pexels-photo-67468.jpeg?auto=compress&cs=tinysrgb&w=600",
        "pa_yacht_vip"       to "https://images.pexels.com/photos/1268855/pexels-photo-1268855.jpeg?auto=compress&cs=tinysrgb&w=600",
        "pa_crazy_horse"     to "https://images.pexels.com/photos/1763075/pexels-photo-1763075.jpeg?auto=compress&cs=tinysrgb&w=600",
        // ── Lyon ──
        "ly_vieux_lyon"      to "https://images.pexels.com/photos/3573382/pexels-photo-3573382.jpeg?auto=compress&cs=tinysrgb&w=600",
        "ly_fourviere"       to "https://images.pexels.com/photos/2404046/pexels-photo-2404046.jpeg?auto=compress&cs=tinysrgb&w=600",
        "ly_bouchon"         to "https://images.pexels.com/photos/1307698/pexels-photo-1307698.jpeg?auto=compress&cs=tinysrgb&w=600",
        "ly_confluence"      to "https://images.pexels.com/photos/2404043/pexels-photo-2404043.jpeg?auto=compress&cs=tinysrgb&w=600",
        "ly_parc_tete_or"    to "https://images.pexels.com/photos/1179229/pexels-photo-1179229.jpeg?auto=compress&cs=tinysrgb&w=600",
        "ly_presquile"       to "https://images.pexels.com/photos/2404049/pexels-photo-2404049.jpeg?auto=compress&cs=tinysrgb&w=600",
        // ── Nice ──
        "ni_promenade"       to "https://images.pexels.com/photos/4353813/pexels-photo-4353813.jpeg?auto=compress&cs=tinysrgb&w=600",
        "ni_vieux_nice"      to "https://images.pexels.com/photos/3225528/pexels-photo-3225528.jpeg?auto=compress&cs=tinysrgb&w=600",
        "ni_colline_chateau" to "https://images.pexels.com/photos/1534057/pexels-photo-1534057.jpeg?auto=compress&cs=tinysrgb&w=600",
        "ni_matisse"         to "https://images.pexels.com/photos/3004909/pexels-photo-3004909.jpeg?auto=compress&cs=tinysrgb&w=600",
        "ni_socca"           to "https://images.pexels.com/photos/1565982/pexels-photo-1565982.jpeg?auto=compress&cs=tinysrgb&w=600",
        "ni_plage"           to "https://images.pexels.com/photos/1174732/pexels-photo-1174732.jpeg?auto=compress&cs=tinysrgb&w=600",
        "ni_yacht_monaco"    to "https://images.pexels.com/photos/1007836/pexels-photo-1007836.jpeg?auto=compress&cs=tinysrgb&w=600",
        // ── Marseille ──
        "ma_notre_dame"      to "https://images.pexels.com/photos/4353229/pexels-photo-4353229.jpeg?auto=compress&cs=tinysrgb&w=600",
        "ma_vieux_port"      to "https://images.pexels.com/photos/3225517/pexels-photo-3225517.jpeg?auto=compress&cs=tinysrgb&w=600",
        "ma_calanques"       to "https://images.pexels.com/photos/1450353/pexels-photo-1450353.jpeg?auto=compress&cs=tinysrgb&w=600",
        "ma_bouillabaisse"   to "https://images.pexels.com/photos/1516415/pexels-photo-1516415.jpeg?auto=compress&cs=tinysrgb&w=600",
        "ma_mucem"           to "https://images.pexels.com/photos/2404047/pexels-photo-2404047.jpeg?auto=compress&cs=tinysrgb&w=600",
        "ma_frioul"          to "https://images.pexels.com/photos/1320684/pexels-photo-1320684.jpeg?auto=compress&cs=tinysrgb&w=600",
        // ── Bordeaux ──
        "bo_cite_vin"        to "https://images.pexels.com/photos/2889685/pexels-photo-2889685.jpeg?auto=compress&cs=tinysrgb&w=600",
        "bo_place_bourse"    to "https://images.pexels.com/photos/3214958/pexels-photo-3214958.jpeg?auto=compress&cs=tinysrgb&w=600",
        "bo_saint_emilion"   to "https://images.pexels.com/photos/442116/pexels-photo-442116.jpeg?auto=compress&cs=tinysrgb&w=600",
        "bo_canele"          to "https://images.pexels.com/photos/205961/pexels-photo-205961.jpeg?auto=compress&cs=tinysrgb&w=600",
        "bo_darwin"          to "https://images.pexels.com/photos/1647121/pexels-photo-1647121.jpeg?auto=compress&cs=tinysrgb&w=600",
        "bo_jardin_public"   to "https://images.pexels.com/photos/158028/belvedere-hotel-park-gardens-702758-158028.jpeg?auto=compress&cs=tinysrgb&w=600",
    )

    // ─── Migration Entry Point ───

    suspend fun migrateAll(onProgress: (String) -> Unit) {
        onProgress("🚀 Début de la migration des images...")
        var successCount = 0
        var errorCount = 0

        // 1. Migrate destinations
        onProgress("📍 Migration des destinations (${destinationImages.size})...")
        for ((id, sourceUrl) in destinationImages) {
            try {
                val storageUrl = downloadAndUpload(sourceUrl, "images/destinations/$id.jpg")
                db.collection("destinations").document(id)
                    .update("imageUrl", storageUrl).await()
                successCount++
                onProgress("  ✅ Destination: $id")
            } catch (e: Exception) {
                errorCount++
                onProgress("  ❌ Destination: $id — ${e.message}")
                Log.e(TAG, "Error migrating destination $id", e)
            }
        }

        // 2. Migrate attractions
        onProgress("🏛️ Migration des attractions (${attractionImages.size})...")
        for ((id, sourceUrl) in attractionImages) {
            try {
                val storageUrl = downloadAndUpload(sourceUrl, "images/attractions/$id.jpg")
                db.collection("attractions").document(id)
                    .update("imageUrl", storageUrl).await()
                successCount++
                onProgress("  ✅ Attraction: $id")
            } catch (e: Exception) {
                errorCount++
                onProgress("  ❌ Attraction: $id — ${e.message}")
                Log.e(TAG, "Error migrating attraction $id", e)
            }
        }

        onProgress("🏁 Migration terminée: $successCount réussies, $errorCount erreurs")
    }

    // ─── Download from URL → Upload to Firebase Storage → Return download URL ───

    private suspend fun downloadAndUpload(sourceUrl: String, storagePath: String): String {
        return withContext(Dispatchers.IO) {
            // Download image bytes
            val bytes = URL(sourceUrl).readBytes()

            // Upload to Firebase Storage
            val ref = storage.reference.child(storagePath)
            ref.putBytes(bytes).await()

            // Get permanent download URL
            ref.downloadUrl.await().toString()
        }
    }
}
