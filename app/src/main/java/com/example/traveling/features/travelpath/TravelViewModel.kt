package com.example.traveling.features.travelpath

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.traveling.data.model.Attraction
import com.example.traveling.data.model.Destination
import com.example.traveling.data.model.PhotoPostDocument
import com.example.traveling.data.model.RouteStop
import com.example.traveling.data.model.TimeSlot
import com.example.traveling.data.model.TravelRoute
import com.example.traveling.data.model.TravelShareAttractionDocument
import com.example.traveling.data.repository.LocalStorageRepository
import com.example.traveling.data.repository.OpenMeteoService
import com.example.traveling.data.repository.PdfExportService
import com.example.traveling.data.repository.PhotoPostRepository
import com.example.traveling.data.repository.SavedRoutesRepository
import com.example.traveling.data.repository.TravelBridgeRepository
import com.example.traveling.data.repository.TravelRepository
import com.example.traveling.data.repository.WeatherInfo
import com.example.traveling.features.main.TravelPathSeed
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.io.File
import kotlin.math.abs

class TravelViewModel : ViewModel() {

    private val repository = TravelRepository()
    private val weatherService = OpenMeteoService()
    private val pdfService = PdfExportService()
    private var localStorage: LocalStorageRepository? = null
    private val savedRoutesRepo = SavedRoutesRepository()
    private val photoPostRepository = PhotoPostRepository()
    private val travelBridgeRepository = TravelBridgeRepository()

    fun initLocalStorage(context: Context) {
        if (localStorage == null) {
            localStorage = LocalStorageRepository(context.applicationContext)
        }
    }

    private val _destinations = MutableStateFlow<List<Destination>>(emptyList())
    val destinations: StateFlow<List<Destination>> = _destinations.asStateFlow()

    private val _quickCities = MutableStateFlow<List<String>>(emptyList())
    val quickCities: StateFlow<List<String>> = _quickCities.asStateFlow()

    private val _attractions = MutableStateFlow<List<Attraction>>(emptyList())
    val attractions: StateFlow<List<Attraction>> = _attractions.asStateFlow()

    private val _routes = MutableStateFlow<List<TravelRoute>>(emptyList())
    val routes: StateFlow<List<TravelRoute>> = _routes.asStateFlow()

    private val _routeStops = MutableStateFlow<List<RouteStop>>(emptyList())
    val routeStops: StateFlow<List<RouteStop>> = _routeStops.asStateFlow()
    private val _stopTravelSharePhotos = MutableStateFlow<Map<String, List<PhotoPostDocument>>>(emptyMap())
    val stopTravelSharePhotos: StateFlow<Map<String, List<PhotoPostDocument>>> = _stopTravelSharePhotos.asStateFlow()

    private val _suggestedAttractions = MutableStateFlow<List<Attraction>>(emptyList())
    val suggestedAttractions: StateFlow<List<Attraction>> = _suggestedAttractions.asStateFlow()
    private val _travelSharePhotoSuggestions = MutableStateFlow<List<PhotoPostDocument>>(emptyList())
    val travelSharePhotoSuggestions: StateFlow<List<PhotoPostDocument>> =
        _travelSharePhotoSuggestions.asStateFlow()
    private val _isLoadingTravelShareSuggestions = MutableStateFlow(false)
    val isLoadingTravelShareSuggestions: StateFlow<Boolean> = _isLoadingTravelShareSuggestions.asStateFlow()

    private val _selectedDestination = MutableStateFlow<Destination?>(null)
    val selectedDestination: StateFlow<Destination?> = _selectedDestination.asStateFlow()

    private val _selectedRoute = MutableStateFlow<TravelRoute?>(null)
    val selectedRoute: StateFlow<TravelRoute?> = _selectedRoute.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _weather = MutableStateFlow<WeatherInfo?>(null)
    val weather: StateFlow<WeatherInfo?> = _weather.asStateFlow()

    private val _pdfExportPath = MutableStateFlow<File?>(null)
    val pdfExportPath: StateFlow<File?> = _pdfExportPath.asStateFlow()

    private val _currentStep = MutableStateFlow("preferences")
    val currentStep: StateFlow<String> = _currentStep.asStateFlow()

    private val _currentRouteId = MutableStateFlow<String?>(null)
    val currentRouteId: StateFlow<String?> = _currentRouteId.asStateFlow()

    fun setStep(step: String) { _currentStep.value = step }
    fun setCurrentRouteId(id: String?) { _currentRouteId.value = id }

    fun resetPlanningState() {
        _currentStep.value = "preferences"
        _currentRouteId.value = null
        _selectedDestination.value = null
        _selectedRoute.value = null
        _routes.value = emptyList()
        _routeStops.value = emptyList()
        _stopTravelSharePhotos.value = emptyMap()
        _pdfExportPath.value = null
        routeAttractionsMap = emptyMap()

        formDestination.value = ""
        formActivities.value = emptySet()
        formBudget.value = 150f
        formDuration.value = 5f
        formEffort.value = 3f
        formFavoritePlaces.value = emptyList()
        formAvoidRain.value = false
        formAvoidHeat.value = false
        formAvoidCold.value = false
        formTravelShareSourcePostId.value = null
        formTravelSharePlaceName.value = ""
        formTravelShareTags.value = emptyList()
        selectedTravelSharePosts.value = emptyList()
        savedFavoritePlaces = emptyList()
        savedActivities = emptySet()
        savedAvoidRain = false
        savedAvoidHeat = false
        savedAvoidCold = false
    }

    val formDestination = MutableStateFlow("")
    val formActivities = MutableStateFlow(setOf<String>())
    val formBudget = MutableStateFlow(150f)
    val formDuration = MutableStateFlow(5f)
    val formEffort = MutableStateFlow(3f)
    val formFavoritePlaces = MutableStateFlow(listOf<String>())
    val formAvoidRain = MutableStateFlow(false)
    val formAvoidHeat = MutableStateFlow(false)
    val formAvoidCold = MutableStateFlow(false)
    val formTravelShareSourcePostId = MutableStateFlow<String?>(null)
    val formTravelSharePlaceName = MutableStateFlow("")
    val formTravelShareTags = MutableStateFlow<List<String>>(emptyList())
    val travelShareSeedPostId = formTravelShareSourcePostId
    val travelShareSeedPlaceName = formTravelSharePlaceName

    private val _destinationNotFound = MutableStateFlow(false)
    val destinationNotFound: StateFlow<Boolean> = _destinationNotFound.asStateFlow()

    fun checkDestination(name: String) {
        if (name.isBlank()) {
            _destinationNotFound.value = false
            return
        }
        val found = _destinations.value.any { it.name.equals(name, ignoreCase = true) }
        _destinationNotFound.value = !found
    }

    private val activityTypeMapping = mapOf(
        "culture" to listOf("Culture", "Monument"),
        "food" to listOf("Gastronomie"),
        "nature" to listOf("Nature"),
        "leisure" to listOf("Loisirs"),
        "shopping" to listOf("Shopping"),
        "nightlife" to listOf("Loisirs"),
        "sport" to listOf("Nature", "Loisirs"),
        "photo" to listOf("Photo", "Nature", "Monument")
    )

    private var savedFavoritePlaces: List<String> = emptyList()
    private var savedActivities: Set<String> = emptySet()
    private var savedAvoidRain: Boolean = false
    private var savedAvoidHeat: Boolean = false
    private var savedAvoidCold: Boolean = false

    private var routeAttractionsMap: Map<String, List<Attraction>> = emptyMap()
    private var suggestedAttractionsJob: Job? = null
    private var travelShareSuggestionsJob: Job? = null
    private var stopPhotosJob: Job? = null
    private val selectedTravelSharePosts = MutableStateFlow<List<PhotoPostDocument>>(emptyList())

    enum class RegenerationAdjustment {
        INDOOR,
        CHEAPER,
        LESS_WALKING,
        SURPRISE
    }

    init {
        loadDestinations()
    }

    fun applyTravelSharePostSeed(postId: String) {
        if (postId.isBlank()) return

        viewModelScope.launch {
            val post = photoPostRepository.getPostOnce(postId).getOrNull() ?: return@launch
            addTravelSharePhotoCandidate(post)
            applyTravelShareSeed(
                TravelPathSeed(
                    sourcePostId = post.postId,
                    placeName = post.locationName,
                    destinationName = post.travelPathDestinationName ?: post.city,
                    latitude = post.displayLatitude ?: post.latitude ?: post.rawLatitude,
                    longitude = post.displayLongitude ?: post.longitude ?: post.rawLongitude,
                    placeType = post.placeType,
                    tags = post.tags
                )
            )
        }
    }

    fun addTravelSharePhotoCandidate(post: PhotoPostDocument) {
        if (post.postId.isBlank()) return
        if (post.visibility != "public" || post.status != "published") return
        if (!post.hasUsableLocation()) return

        val current = selectedTravelSharePosts.value
        if (current.none { it.postId == post.postId }) {
            selectedTravelSharePosts.value = current + post
        }

        val placeName = post.locationName.trim()
        if (placeName.isNotBlank() &&
            formFavoritePlaces.value.none { it.equals(placeName, ignoreCase = true) }
        ) {
            formFavoritePlaces.value = formFavoritePlaces.value + placeName
        }
    }

    fun applyTravelShareSeed(seed: TravelPathSeed) {
        val placeName = seed.placeName.trim()
        val cleanedTags = seed.tags.map { it.trim() }.filter { it.isNotBlank() }

        formTravelShareSourcePostId.value = seed.sourcePostId
        formTravelSharePlaceName.value = placeName
        formTravelShareTags.value = cleanedTags

        val candidateDestination = seed.destinationName?.trim().orEmpty()
        val exists = _destinations.value.any { it.name.equals(candidateDestination, ignoreCase = true) }
        if (candidateDestination.isNotBlank() && exists) {
            formDestination.value = candidateDestination
        }

        if (placeName.isNotBlank() &&
            formFavoritePlaces.value.none { it.equals(placeName, ignoreCase = true) }
        ) {
            formFavoritePlaces.value = formFavoritePlaces.value + placeName
        }

        val inferred = inferActivitiesFromTravelShareSeed(seed)
        if (inferred.isNotEmpty()) {
            formActivities.value = formActivities.value + inferred
        }
    }

    fun clearTravelShareSeed() {
        val placeName = formTravelSharePlaceName.value
        if (placeName.isNotBlank()) {
            formFavoritePlaces.value = formFavoritePlaces.value.filterNot {
                it.equals(placeName, ignoreCase = true)
            }
        }
        formTravelShareSourcePostId.value = null
        formTravelSharePlaceName.value = ""
        formTravelShareTags.value = emptyList()
        selectedTravelSharePosts.value = emptyList()
    }

    private fun inferActivitiesFromTravelShareSeed(seed: TravelPathSeed): Set<String> {
        val values = buildList {
            add(seed.placeType.lowercase())
            addAll(seed.tags.map { it.lowercase() })
        }

        val inferred = mutableSetOf<String>()
        values.forEach { value ->
            when {
                value.contains("museum") || value.contains("musée") || value.contains("culture") || value.contains("cultural") -> {
                    inferred += "culture"
                }
                value.contains("nature") || value.contains("park") || value.contains("parc") ||
                    value.contains("mountain") || value.contains("beach") || value.contains("lake") || value.contains("forest") -> {
                    inferred += "nature"
                }
                value.contains("restaurant") || value.contains("food") || value.contains("gastronomie") ||
                    value.contains("cafe") || value.contains("café") -> {
                    inferred += "food"
                }
                value.contains("shop") || value.contains("shopping") || value.contains("magasin") || value.contains("market") -> {
                    inferred += "shopping"
                }
                value.contains("street") || value.contains("rue") || value.contains("monument") ||
                    value.contains("architecture") || value.contains("photo") -> {
                    inferred += "photo"
                    inferred += "culture"
                }
            }
        }
        return inferred
    }

    private fun loadDestinations() {
        viewModelScope.launch {
            repository.getDestinations().collect { list ->
                _destinations.value = list
                _quickCities.value = list.map { it.name }
            }
        }
    }

    fun selectDestination(
        destinationName: String,
        budget: Int = 5000,
        activities: Set<String> = emptySet(),
        durationHours: Int = 8,
        effort: Int = 3,
        favoritePlaces: List<String> = emptyList(),
        avoidRain: Boolean = false,
        avoidHeat: Boolean = false,
        avoidCold: Boolean = false
    ) {
        val dest = _destinations.value.find {
            it.name.equals(destinationName, ignoreCase = true)
        } ?: return

        _selectedDestination.value = dest
        savedFavoritePlaces = favoritePlaces
        savedActivities = activities
        savedAvoidRain = avoidRain
        savedAvoidHeat = avoidHeat
        savedAvoidCold = avoidCold

        viewModelScope.launch {
            _isLoading.value = true
            repository.getAttractionsByDestination(dest.id).collect { allAttractions ->
                val linkedTravelSharePosts = runCatching {
                    travelBridgeRepository.getLinkedTravelSharePhotosForDestination(dest.id)
                }.getOrDefault(emptyList())
                val linkedTravelShareAttractions = runCatching {
                    travelBridgeRepository.getTravelShareAttractionsForDestination(dest.id)
                }.getOrDefault(emptyList())
                val candidatePosts = (selectedTravelSharePosts.value + linkedTravelSharePosts)
                    .distinctBy { it.postId }
                val travelShareCandidates = buildTravelSharePhotoAttractionCandidates(
                    posts = candidatePosts,
                    officialAttractions = allAttractions,
                    destination = dest
                )
                val travelShareAttractionCandidates = buildTravelShareStoredAttractionCandidates(
                    attractions = linkedTravelShareAttractions,
                    officialAttractions = allAttractions + travelShareCandidates,
                    destination = dest
                )
                val mergedAttractions = allAttractions + travelShareCandidates + travelShareAttractionCandidates

                _attractions.value = mergedAttractions
                val currentWeather = weatherService.getWeather(dest.lat, dest.lng)
                _weather.value = currentWeather

                val filtered = filterAttractions(mergedAttractions, budget, activities, currentWeather)
                val weatherAdjustedAll = applyWeatherPreferences(mergedAttractions, currentWeather)

                _routes.value = generateRoutes(filtered, weatherAdjustedAll, dest, budget, durationHours, effort)

                if (_routes.value.isNotEmpty()) {
                    val firstRouteAttractions = routeAttractionsMap[_routes.value[0].id] ?: emptyList()
                    _routeStops.value = generateStops(firstRouteAttractions)
                }

                _isLoading.value = false
            }
        }
    }

    fun updateSuggestedAttractions(cityName: String) {
        updateOfficialAttractionSuggestions(cityName)
        updateTravelSharePhotoSuggestions(cityName)
    }

    private fun updateOfficialAttractionSuggestions(cityName: String) {
        val dest = _destinations.value.find { it.name.equals(cityName, ignoreCase = true) }
        if (dest == null) {
            _suggestedAttractions.value = emptyList()
            suggestedAttractionsJob?.cancel()
            suggestedAttractionsJob = null
            return
        }

        suggestedAttractionsJob?.cancel()
        suggestedAttractionsJob = viewModelScope.launch {
            repository.getAttractionsByDestination(dest.id).collect { list ->
                _suggestedAttractions.value = list.sortedByDescending { it.rating }.take(5)
            }
        }
    }

    private fun updateTravelSharePhotoSuggestions(cityName: String) {
        val dest = _destinations.value.find { it.name.equals(cityName, ignoreCase = true) }
        if (cityName.isBlank() || dest == null) {
            _travelSharePhotoSuggestions.value = emptyList()
            travelShareSuggestionsJob?.cancel()
            travelShareSuggestionsJob = null
            _isLoadingTravelShareSuggestions.value = false
            return
        }

        travelShareSuggestionsJob?.cancel()
        travelShareSuggestionsJob = viewModelScope.launch {
            _isLoadingTravelShareSuggestions.value = true
            travelBridgeRepository.observeLinkedTravelSharePhotosForDestination(
                destinationId = dest.id,
                fallbackCity = dest.name
            ).collect { posts ->
                _travelSharePhotoSuggestions.value = posts.sortedByDescending { rankTravelShareSuggestion(it) }
                _isLoadingTravelShareSuggestions.value = false
            }
        }
    }

    fun loadTravelSharePhotosForStops(stops: List<RouteStop>) {
        if (stops.isEmpty()) {
            _stopTravelSharePhotos.value = emptyMap()
            stopPhotosJob?.cancel()
            stopPhotosJob = null
            return
        }

        val destinationName = _selectedDestination.value?.name
        stopPhotosJob?.cancel()
        stopPhotosJob = viewModelScope.launch {
            val result = mutableMapOf<String, List<PhotoPostDocument>>()
            val selectedPosts = selectedTravelSharePosts.value
            stops.take(8).forEach { stop ->
                val linkedPoolPhotos = runCatching {
                    travelBridgeRepository.findPhotosForRouteStop(
                        stopName = stop.name,
                        lat = stop.lat.takeIf { it != 0.0 },
                        lng = stop.lng.takeIf { it != 0.0 },
                        city = destinationName,
                        radiusKm = 0.5
                    )
                }.getOrDefault(emptyList())

                val selectedMatches = selectedPosts.filter { post ->
                    val postLat = post.displayLatitude ?: post.latitude ?: post.rawLatitude
                    val postLng = post.displayLongitude ?: post.longitude ?: post.rawLongitude
                    val byName = areNamesSimilar(post.locationName, stop.name)
                    val byDistance = if (
                        postLat != null && postLng != null &&
                        stop.lat != 0.0 && stop.lng != 0.0
                    ) {
                        haversine(postLat, postLng, stop.lat, stop.lng) <= 0.5
                    } else {
                        false
                    }
                    byName || byDistance
                }

                val photos = (selectedMatches + linkedPoolPhotos)
                    .distinctBy { it.postId }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }

                if (photos.isNotEmpty()) {
                    result[stop.id] = photos.take(6)
                }
            }
            _stopTravelSharePhotos.value = result
        }
    }

    private fun rankTravelShareSuggestion(post: PhotoPostDocument): Int {
        var score = 0
        val selectedActivities = formActivities.value
        val seedTags = formTravelShareTags.value.map { it.lowercase() }
        val postTags = post.tags.map { it.lowercase() }

        if (postTags.any { seedTags.contains(it) }) score += 4
        if (placeTypeMatchesSelectedActivities(post.placeType, selectedActivities)) score += 3
        score += minOf(post.likeCount, 10)
        score += ((post.createdAt?.seconds ?: 0L) / 86_400).toInt()
        return score
    }

    private fun placeTypeMatchesSelectedActivities(placeType: String, activities: Set<String>): Boolean {
        val value = placeType.lowercase()
        if (activities.isEmpty()) return false
        return when {
            value.contains("nature") || value.contains("park") -> "nature" in activities
            value.contains("museum") || value.contains("culture") || value.contains("monument") -> "culture" in activities
            value.contains("food") || value.contains("restaurant") || value.contains("cafe") -> "food" in activities
            value.contains("shop") || value.contains("market") -> "shopping" in activities
            value.contains("street") || value.contains("photo") -> "photo" in activities || "culture" in activities
            else -> false
        }
    }

    private fun buildTravelSharePhotoAttractionCandidates(
        posts: List<PhotoPostDocument>,
        officialAttractions: List<Attraction>,
        destination: Destination
    ): List<Attraction> {
        return posts
            .filter { it.visibility == "public" && it.status == "published" }
            .filter {
                it.travelPathDestinationId.isNullOrBlank() ||
                    it.travelPathDestinationId == destination.id ||
                    it.city.equals(destination.name, ignoreCase = true)
            }
            .filter { it.hasUsableLocation() }
            .filterNot { post ->
                officialAttractions.any { attr -> isDuplicateTravelSharePlace(post, attr) }
            }
            .map { it.toAttractionCandidate(destination.id) }
    }

    private fun PhotoPostDocument.toAttractionCandidate(destinationId: String): Attraction {
        val resolvedLat = displayLatitude ?: latitude ?: rawLatitude ?: 0.0
        val resolvedLng = displayLongitude ?: longitude ?: rawLongitude ?: 0.0
        val attractionType = mapPlaceTypeToAttractionType(placeType, tags)
        return Attraction(
            id = "photo_$postId",
            destinationId = destinationId,
            name = locationName.ifBlank { title.ifBlank { "Lieu TravelShare" } },
            type = attractionType,
            cost = estimateTravelShareCost(attractionType, placeType, tags),
            duration = 45,
            rating = 4.2 + minOf(likeCount, 50) / 100.0,
            description = description.ifBlank { title },
            imageUrl = imageUrls.firstOrNull().orEmpty(),
            lat = resolvedLat,
            lng = resolvedLng,
            tags = (tags + "TravelShare").distinct(),
            imageUrls = imageUrls,
            source = "travelshare",
            sourcePostId = postId
        )
    }

    private fun buildTravelShareStoredAttractionCandidates(
        attractions: List<TravelShareAttractionDocument>,
        officialAttractions: List<Attraction>,
        destination: Destination
    ): List<Attraction> {
        return attractions
            .filter { it.status == "active" && it.destinationId == destination.id }
            .filter { it.name.isNotBlank() && it.lat != 0.0 && it.lng != 0.0 }
            .filterNot { travelShareAttraction ->
                officialAttractions.any { attr -> isDuplicateTravelShareAttraction(travelShareAttraction, attr) }
            }
            .map { it.toAttractionCandidate() }
    }

    private fun TravelShareAttractionDocument.toAttractionCandidate(): Attraction {
        return Attraction(
            id = id,
            destinationId = destinationId,
            name = name,
            type = type.ifBlank { "Photo" },
            cost = cost.takeIf { it > 0 } ?: estimateTravelShareCost(type, type, tags),
            duration = duration,
            rating = rating,
            description = description,
            imageUrl = imageUrl,
            lat = lat,
            lng = lng,
            openHours = openHours,
            closedDay = closedDay,
            effortLevel = effortLevel,
            weatherType = weatherType,
            bestTimeSlots = bestTimeSlots,
            tags = (tags + "TravelShare").distinct(),
            imageUrls = imageUrls.ifEmpty { listOf(imageUrl).filter { it.isNotBlank() } },
            source = source,
            sourcePostId = sourcePostId
        )
    }

    private fun mapPlaceTypeToAttractionType(placeType: String, tags: List<String>): String {
        val terms = (listOf(placeType) + tags).joinToString(" ").lowercase()
        return when {
            terms.contains("museum") || terms.contains("musée") || terms.contains("culture") || terms.contains("cultural") -> "Culture"
            terms.contains("monument") || terms.contains("architecture") || terms.contains("street") || terms.contains("rue") -> "Monument"
            terms.contains("nature") || terms.contains("park") || terms.contains("parc") || terms.contains("beach") ||
                terms.contains("mountain") || terms.contains("lake") || terms.contains("forest") -> "Nature"
            terms.contains("restaurant") || terms.contains("food") || terms.contains("gastronomie") ||
                terms.contains("cafe") || terms.contains("café") -> "Gastronomie"
            terms.contains("shop") || terms.contains("shopping") || terms.contains("market") || terms.contains("magasin") -> "Shopping"
            terms.contains("sport") -> "Loisirs"
            terms.contains("nightlife") || terms.contains("soir") || terms.contains("bar") -> "Loisirs"
            terms.contains("leisure") || terms.contains("loisirs") -> "Loisirs"
            else -> "Photo"
        }
    }

    private fun estimateTravelShareCost(type: String, placeType: String, tags: List<String>): Int {
        val terms = (listOf(type, placeType) + tags).joinToString(" ").lowercase()
        return when {
            terms.contains("restaurant") || terms.contains("food") || terms.contains("gastronomie") ||
                terms.contains("cafe") || terms.contains("café") -> 25
            terms.contains("shop") || terms.contains("shopping") || terms.contains("market") || terms.contains("magasin") -> 20
            terms.contains("museum") || terms.contains("musée") || terms.contains("culture") -> 12
            terms.contains("monument") || terms.contains("architecture") -> 10
            terms.contains("sport") -> 15
            terms.contains("nightlife") || terms.contains("bar") -> 30
            terms.contains("leisure") || terms.contains("loisirs") -> 18
            terms.contains("nature") || terms.contains("park") || terms.contains("parc") ||
                terms.contains("beach") || terms.contains("mountain") || terms.contains("lake") || terms.contains("forest") -> 0
            else -> 5
        }
    }

    private fun isDuplicateTravelSharePlace(post: PhotoPostDocument, attraction: Attraction): Boolean {
        val postLat = post.displayLatitude ?: post.latitude ?: post.rawLatitude
        val postLng = post.displayLongitude ?: post.longitude ?: post.rawLongitude
        if (postLat != null && postLng != null && attraction.lat != 0.0 && attraction.lng != 0.0) {
            val distanceKm = haversine(postLat, postLng, attraction.lat, attraction.lng)
            if (distanceKm <= 0.2) return true
        }
        return areNamesSimilar(post.locationName, attraction.name)
    }

    private fun isDuplicateTravelShareAttraction(travelShareAttraction: TravelShareAttractionDocument, attraction: Attraction): Boolean {
        if (travelShareAttraction.lat != 0.0 && travelShareAttraction.lng != 0.0 && attraction.lat != 0.0 && attraction.lng != 0.0) {
            val distanceKm = haversine(travelShareAttraction.lat, travelShareAttraction.lng, attraction.lat, attraction.lng)
            if (distanceKm <= 0.2) return true
        }
        return areNamesSimilar(travelShareAttraction.name, attraction.name)
    }

    private fun areNamesSimilar(a: String, b: String): Boolean {
        val left = normalizePlaceName(a)
        val right = normalizePlaceName(b)
        if (left.isBlank() || right.isBlank()) return false
        if (left == right) return true
        return left.contains(right) || right.contains(left)
    }

    private fun normalizePlaceName(input: String): String {
        val ascii = Normalizer.normalize(input.lowercase(), Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
        return ascii
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .split("\\s+".toRegex())
            .filter { token ->
                token.isNotBlank() && token !in setOf("the", "le", "la", "les", "de", "des", "du", "d")
            }
            .joinToString(" ")
            .trim()
    }

    private fun PhotoPostDocument.hasUsableLocation(): Boolean {
        val lat = displayLatitude ?: latitude ?: rawLatitude
        val lng = displayLongitude ?: longitude ?: rawLongitude
        return lat != null && lng != null && locationName.isNotBlank()
    }

    private fun filterAttractions(
        all: List<Attraction>,
        budget: Int,
        activities: Set<String>,
        weather: WeatherInfo?
    ): List<Attraction> {

        val wantedTypes = activities.flatMap { activityTypeMapping[it] ?: emptyList() }.toSet()

        var result = all.filter { attr ->
            (wantedTypes.isEmpty() || attr.type in wantedTypes) &&
            attr.cost <= budget
        }

        if (result.size < 3) {
            result = all.filter { attr ->
                attr.cost <= budget
            }
        }

        if (result.size < 3) {
            result = all.sortedByDescending { it.rating }.take(5)
        }

        if (savedFavoritePlaces.isNotEmpty()) {
            val favorites = result.filter { attr ->
                savedFavoritePlaces.any { fav ->
                    attr.name.contains(fav, ignoreCase = true) || fav.contains(attr.name, ignoreCase = true)
                }
            }
            val others = result.filter { it !in favorites }
            result = favorites + others
        }

        return applyWeatherPreferences(result, weather).sortedByDescending { weatherScore(it, weather) }
    }

    private fun applyWeatherPreferences(
        attractions: List<Attraction>,
        weather: WeatherInfo?
    ): List<Attraction> {
        if (weather == null || !shouldPreferShelter(weather)) return attractions

        val sheltered = attractions.filter { it.weatherType == "indoor" || it.weatherType == "both" }
        return if (sheltered.size >= 3) {
            sheltered.sortedByDescending { weatherScore(it, weather) }
        } else {
            attractions.sortedByDescending { weatherScore(it, weather) }
        }
    }

    private fun shouldPreferShelter(weather: WeatherInfo): Boolean {
        return (savedAvoidRain && isRainy(weather.weatherCode)) ||
            (savedAvoidHeat && weather.temperature >= 28.0) ||
            (savedAvoidCold && weather.temperature <= 5.0)
    }

    private fun weatherScore(attr: Attraction, weather: WeatherInfo?): Double {
        if (weather == null || !shouldPreferShelter(weather)) return attr.rating

        val shelterBonus = when (attr.weatherType) {
            "indoor" -> 2.0
            "both" -> 1.0
            else -> -2.0
        }
        val durationPenalty = if (attr.weatherType == "outdoor" && attr.duration > 120) 0.8 else 0.0
        return attr.rating + shelterBonus - durationPenalty
    }

    private fun isRainy(code: Int): Boolean {
        return code in 51..67 || code in 80..82 || code in 95..99
    }

    private fun generateRoutes(
        filtered: List<Attraction>,
        allAttractions: List<Attraction>,
        dest: Destination,
        budget: Int,
        durationHours: Int,
        effort: Int
    ): List<TravelRoute> {
        if (filtered.isEmpty()) return emptyList()

        val maxMinutes = durationHours * 60
        val maxWalkingKm = maxWalkingDistanceForEffort(effort)
        val wantedTypes = savedActivities.flatMap { activityTypeMapping[it] ?: emptyList() }.toSet()

        val cheapBudget = (budget * 0.6).toInt().coerceAtLeast(50)
        val balancedBudget = budget.coerceAtLeast(80)
        val premiumBudget = (budget * 1.5).toInt().coerceAtLeast(140)
        val cheapPool = filtered.sortedBy { it.cost }
        val cheapAttractions = selectWithinConstraints(cheapPool, cheapBudget, maxMinutes, maxWalkingKm, wantedTypes)

        val balancedTargetCost = (balancedBudget / 4).coerceIn(15, 90)
        val balancedPool = filtered.sortedWith(
            compareBy<Attraction> { abs(it.cost - balancedTargetCost) }
                .thenByDescending { it.rating }
        )
        val balancedAttractions = selectWithinConstraints(balancedPool, balancedBudget, maxMinutes, maxWalkingKm, wantedTypes)

        val premiumPool = allAttractions.sortedWith(
            compareByDescending<Attraction> { it.rating + (it.cost.coerceAtMost(250) / 100.0) }
                .thenByDescending { it.cost }
        )
        val premiumAttractions = selectWithinConstraints(
            premiumPool,
            premiumBudget,
            (maxMinutes * 1.3).toInt(),
            maxWalkingKm,
            wantedTypes
        )

        routeAttractionsMap = mapOf(
            "${dest.id}_eco" to cheapAttractions,
            "${dest.id}_bal" to balancedAttractions,
            "${dest.id}_pre" to premiumAttractions
        )

        val routes = mutableListOf<TravelRoute>()

        if (cheapAttractions.isNotEmpty()) {
            routes.add(buildTravelRoute("${dest.id}_eco", "Route Économique", "Budget maîtrisé", cheapAttractions, dest, Pair(0xFF10B981, 0xFF0D9488)))
        }
        if (balancedAttractions.isNotEmpty()) {
            routes.add(buildTravelRoute("${dest.id}_bal", "Route Équilibrée", "Meilleur rapport qualité-prix", balancedAttractions, dest, Pair(0xFFB91C1C, 0xFF991B1B)))
        }
        if (premiumAttractions.isNotEmpty()) {
            routes.add(buildTravelRoute("${dest.id}_pre", "Route Premium", "Expérience haut de gamme", premiumAttractions, dest, Pair(0xFFF59E0B, 0xFFB45309)))
        }

        return routes
    }

    private fun selectWithinConstraints(
        sorted: List<Attraction>,
        maxBudget: Int,
        maxMinutes: Int,
        maxWalkingKm: Double,
        wantedTypes: Set<String>,
        excludeIds: Set<String> = emptySet(),
        minStops: Int = 3
    ): List<Attraction> {

        val available = sorted.filter { it.id !in excludeIds }

        val selected = mutableListOf<Attraction>()
        var totalCost = 0
        var totalMinutes = 0
        var totalWalkingKm = 0.0
        val maxStops = when {
            maxMinutes <= 240 -> 4
            maxMinutes <= 420 -> 6
            else -> 8
        }
        val effectiveMinStops = minStops.coerceAtMost(maxStops)

        fun addCandidate(attr: Attraction, relaxed: Boolean = false): Boolean {
            if (attr in selected || selected.size >= maxStops) return false
            val additionalWalkingKm = additionalWalkingDistanceKm(selected.lastOrNull(), attr)
            val transitTime = walkingMinutes(additionalWalkingKm)
            val newTotalMinutes = totalMinutes + attr.duration + transitTime
            val newTotalCost = totalCost + attr.cost
            val newWalkingKm = totalWalkingKm + additionalWalkingKm
            val allowedBudget = if (relaxed) (maxBudget * 1.15).toInt().coerceAtLeast(maxBudget + 20) else maxBudget
            val allowedMinutes = if (relaxed) (maxMinutes * 1.15).toInt() else maxMinutes
            val allowedWalkingKm = if (relaxed) maxWalkingKm * 1.2 else maxWalkingKm

            return if (newTotalCost <= allowedBudget && newTotalMinutes <= allowedMinutes && newWalkingKm <= allowedWalkingKm) {
                selected.add(attr)
                totalCost = newTotalCost
                totalMinutes = newTotalMinutes
                totalWalkingKm = newWalkingKm
                true
            } else {
                false
            }
        }

        for (attr in available.filter { matchesFavoritePlace(it) }) {
            addCandidate(attr)
        }

        if (wantedTypes.isNotEmpty()) {
            val prefFirst = available.firstOrNull { it.type in wantedTypes && it !in selected }
            if (prefFirst != null) {
                addCandidate(prefFirst)
            }
        }

        val targetSlots = if (maxMinutes >= 360) {
            listOf("matin", "apres-midi", "soir")
        } else {
            listOf("matin", "apres-midi")
        }
        for (slot in targetSlots) {
            if (selected.any { slot in it.bestTimeSlots }) continue
            val slotCandidate = available.firstOrNull { attr ->
                attr !in selected &&
                    slot in attr.bestTimeSlots &&
                    (wantedTypes.isEmpty() || attr.type in wantedTypes || selected.any { it.type in wantedTypes })
            } ?: available.firstOrNull { attr ->
                attr !in selected && slot in attr.bestTimeSlots
            }
            if (slotCandidate != null) {
                addCandidate(slotCandidate)
            }
        }

        for (attr in available) {
            addCandidate(attr)
            if (selected.size >= maxStops) break
        }

        if (selected.size < effectiveMinStops) {
            for (attr in available) {
                addCandidate(attr, relaxed = true)
                if (selected.size >= effectiveMinStops) break
            }
        }

        return selected
    }

    private fun maxWalkingDistanceForEffort(effort: Int): Double {
        return when (effort.coerceIn(1, 5)) {
            1 -> 2.0
            2 -> 5.0
            3 -> 9.0
            4 -> 14.0
            else -> 30.0
        }
    }

    private fun routeWalkingDistanceKm(attractions: List<Attraction>): Double {
        val ordered = orderAttractionsForRoute(attractions)
        return ordered.zipWithNext().sumOf { (from, to) -> additionalWalkingDistanceKm(from, to) }
    }

    private fun additionalWalkingDistanceKm(from: Attraction?, to: Attraction): Double {
        if (from == null) return 0.0
        return if (from.lat != 0.0 && from.lng != 0.0 && to.lat != 0.0 && to.lng != 0.0) {
            haversine(from.lat, from.lng, to.lat, to.lng)
        } else {
            2.0
        }
    }

    private fun walkingMinutes(distanceKm: Double): Int {
        if (distanceKm <= 0.0) return 0
        return (distanceKm * 12).toInt().coerceAtLeast(5)
    }

    private fun inferRouteEffortLevel(
        walkingKm: Double,
        stopsCount: Int,
        totalMinutes: Int,
        attractions: List<Attraction>
    ): Int {
        val base = when {
            walkingKm < 2.0 -> 1
            walkingKm < 5.0 -> 2
            walkingKm < 9.0 -> 3
            walkingKm < 14.0 -> 4
            else -> 5
        }
        val outdoorCount = attractions.count { it.weatherType == "outdoor" || it.type in listOf("Nature", "Monument") }
        val restCount = attractions.count { it.weatherType == "indoor" || it.type in listOf("Gastronomie", "Shopping") }
        val outdoorPenalty = if (outdoorCount >= 3) 1 else 0
        val restBonus = if (restCount >= 2) 1 else 0
        return (base +
            if (stopsCount >= 5) 1 else 0 +
            if (totalMinutes >= 8 * 60) 1 else 0 +
            outdoorPenalty -
            restBonus
        ).coerceIn(1, 5)
    }

    private fun effortLabel(level: Int): String {
        return when (level.coerceIn(1, 5)) {
            1 -> "Très facile"
            2 -> "Facile"
            3 -> "Modéré"
            4 -> "Élevé"
            else -> "Intense"
        }
    }

    private fun routeDurationLabel(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return if (mins > 0) "${hours}h${"%02d".format(mins)}" else "${hours}h"
    }

    private fun orderAttractionsForRoute(attractions: List<Attraction>): List<Attraction> {
        return attractions.sortedBy { attr ->
            when {
                attr.bestTimeSlots.contains("matin") -> 0
                attr.bestTimeSlots.contains("apres-midi") -> 1
                attr.bestTimeSlots.contains("soir") -> 2
                else -> 1
            }
        }
    }

    private fun matchesFavoritePlace(attr: Attraction): Boolean {
        return savedFavoritePlaces.any { favorite ->
            val place = favorite.trim()
            place.isNotBlank() &&
                (attr.name.contains(place, ignoreCase = true) || place.contains(attr.name, ignoreCase = true))
        }
    }

    private fun buildTravelRoute(
        id: String, name: String, subtitle: String,
        attractions: List<Attraction>, dest: Destination,
        colors: Pair<Long, Long>
    ): TravelRoute {
        val walkingKm = routeWalkingDistanceKm(attractions)
        val totalMinutes = attractions.sumOf { it.duration } + walkingMinutes(walkingKm)
        val effortLevel = inferRouteEffortLevel(walkingKm, attractions.size, totalMinutes, attractions)

        return TravelRoute(
            id = id, name = name, subtitle = subtitle,
            destName = dest.name,
            budget = attractions.sumOf { it.cost },
            duration = routeDurationLabel(totalMinutes),
            effort = effortLabel(effortLevel), effortLevel = effortLevel,
            stops = attractions.size,
            rating = (attractions.map { it.rating }.average() * 10).toInt() / 10f,
            reviews = (50..300).random(),
            imageUrl = attractions.firstOrNull()?.imageUrl ?: dest.imageUrl,
            highlights = attractions.map { it.name },
            gradientColors = colors
        )
    }

    fun selectRoute(routeId: String) {
        _selectedRoute.value = _routes.value.find { it.id == routeId }
        val routeAttractions = routeAttractionsMap[routeId] ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _routeStops.value = generateStops(routeAttractions)
            _isLoading.value = false
        }
    }

    private suspend fun generateStops(attractions: List<Attraction>): List<RouteStop> {
        if (attractions.isEmpty()) return emptyList()

        val ordered = orderAttractionsForRoute(attractions)

        var currentHour = 9
        var currentMinute = 0

        val basicStops = ordered.mapIndexed { index, attr ->

            var distString = "Départ"
            var walkStr = ""
            var walkMinutes = 0

            if (index > 0) {
                val prev = ordered[index - 1]
                if (prev.lat != 0.0 && attr.lat != 0.0) {
                    val distKm = haversine(prev.lat, prev.lng, attr.lat, attr.lng)
                    distString = if (distKm < 1.0) "${(distKm * 1000).toInt()} m" else "%.1f km".format(distKm)
                    walkMinutes = (distKm * 12).toInt().coerceAtLeast(5)
                    walkStr = "~$walkMinutes min"
                } else {
                    distString = "~2 km"
                    walkMinutes = 15
                    walkStr = "~15 min"
                }

                currentMinute += walkMinutes
                currentHour += currentMinute / 60
                currentMinute %= 60
            }

            val timeSlot = when {
                currentHour < 12 -> TimeSlot.MATIN
                currentHour < 18 -> TimeSlot.APRES_MIDI
                else -> TimeSlot.SOIR
            }

            val arrivalTime = "%02d:%02d".format(currentHour, currentMinute)
            val durationStr = if (attr.duration >= 60)
                "${attr.duration / 60}h${if (attr.duration % 60 > 0) "%02d".format(attr.duration % 60) else ""}"
            else "${attr.duration} min"

            val stop = RouteStop(
                id = "s${index + 1}",
                name = attr.name,
                type = attr.type,
                timeSlot = timeSlot,
                arrivalTime = arrivalTime,
                duration = durationStr,
                distance = distString,
                walkTime = walkStr,
                cost = attr.cost,
                description = attr.description,
                imageUrl = attr.imageUrl,
                imageUrls = attr.imageUrls.ifEmpty { listOf(attr.imageUrl).filter { it.isNotBlank() } },
                rating = attr.rating.toFloat(),
                openHours = attr.openHours,
                lat = attr.lat,
                lng = attr.lng,
                source = attr.source,
                sourcePostId = attr.sourcePostId
            )

            currentMinute += attr.duration
            currentHour += currentMinute / 60
            currentMinute %= 60

            stop
        }

        try {
            val apiKey = com.example.traveling.BuildConfig.MAPS_API_KEY
            val directionsService = com.example.traveling.data.repository.GoogleDirectionsService()
            val directions = directionsService.getDirections(basicStops, apiKey)

            if (directions.size == basicStops.size - 1) {
                return basicStops.mapIndexed { index, stop ->
                    if (index > 0) {
                        val dir = directions[index - 1]
                        stop.copy(
                            distance = dir.distanceText,
                            walkTime = dir.durationText,
                            polylineToNext = dir.polyline
                        )
                    } else {
                        stop
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return basicStops
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    fun isRouteLiked(routeId: String): Boolean =
        localStorage?.isRouteLiked(routeId) ?: false

    fun toggleRouteLike(routeId: String): Boolean {
        val route = _selectedRoute.value
        val destName = route?.destName ?: _selectedDestination.value?.name ?: ""

        route?.let {
            localStorage?.storeRouteInfo(it, destName)
            localStorage?.cacheRoute(it.id, it, _routeStops.value)
        }
        val nowLiked = localStorage?.toggleRouteLike(routeId) ?: false

        viewModelScope.launch {
            try {
                if (nowLiked && route != null) {
                    savedRoutesRepo.saveRoute(route, _routeStops.value, destName, "liked")
                } else {
                    savedRoutesRepo.removeRoute(routeId, "liked")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return nowLiked
    }

    fun isRouteSaved(routeId: String): Boolean =
        localStorage?.isRouteSaved(routeId) ?: false

    fun toggleRouteSave(routeId: String): Boolean {
        val route = _selectedRoute.value
        val destName = route?.destName ?: _selectedDestination.value?.name ?: ""

        route?.let {
            localStorage?.storeRouteInfo(it, destName)
            localStorage?.cacheRoute(it.id, it, _routeStops.value)
        }
        val nowSaved = localStorage?.toggleRouteSave(routeId) ?: false

        viewModelScope.launch {
            try {
                if (nowSaved && route != null) {
                    savedRoutesRepo.saveRoute(route, _routeStops.value, destName, "saved")
                } else {
                    savedRoutesRepo.removeRoute(routeId, "saved")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return nowSaved
    }

    fun getLikedRoutes() = localStorage?.getLikedRoutes() ?: emptyList()
    fun getSavedRoutes() = localStorage?.getSavedRoutes() ?: emptyList()

    fun loadCachedRoute(routeId: String): Boolean {

        val cached = localStorage?.getCachedRoute(routeId)
        if (cached != null) {
            val (route, stops) = cached
            _selectedRoute.value = route
            _routeStops.value = stops
            return true
        }

        return false
    }

    suspend fun loadCachedRouteAsync(routeId: String): Boolean {

        val cached = localStorage?.getCachedRoute(routeId)
        if (cached != null) {
            val (route, stops) = cached
            _selectedRoute.value = route
            _routeStops.value = stops
            return true
        }

        for (type in listOf("liked", "saved")) {
            val doc = savedRoutesRepo.getRoute(routeId, type)
            if (doc != null) {
                val (route, stops) = savedRoutesRepo.toRouteAndStops(doc)
                _selectedRoute.value = route
                _routeStops.value = stops

                localStorage?.cacheRoute(route.id, route, stops)
                return true
            }
        }
        return false
    }

    fun regenerateCurrentRoute(adjustment: RegenerationAdjustment = RegenerationAdjustment.SURPRISE) {
        val route = _selectedRoute.value ?: return
        val routeId = route.id
        val currentAttractions = routeAttractionsMap[routeId] ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val targetCount = currentAttractions.size.coerceAtLeast(3)
            val candidatePool = _attractions.value.ifEmpty { currentAttractions }
            val adjusted = when (adjustment) {
                RegenerationAdjustment.INDOOR -> candidatePool.sortedByDescending {
                    when (it.weatherType) {
                        "indoor" -> 3
                        "both" -> 2
                        else -> 1
                    }
                }
                RegenerationAdjustment.CHEAPER -> candidatePool.sortedWith(
                    compareBy<Attraction> { it.cost }.thenByDescending { it.rating }
                )
                RegenerationAdjustment.LESS_WALKING -> candidatePool.sortedBy { it.effortLevel }
                RegenerationAdjustment.SURPRISE -> candidatePool.shuffled()
            }.distinctBy { it.id }.take(targetCount).ifEmpty { currentAttractions.shuffled() }
            val adjustedStops = generateStops(adjusted)
            routeAttractionsMap = routeAttractionsMap + (routeId to adjusted)
            _routeStops.value = adjustedStops
            val walkingKm = routeWalkingDistanceKm(adjusted)
            val totalMinutes = adjusted.sumOf { it.duration } + walkingMinutes(walkingKm)
            val effortLevel = inferRouteEffortLevel(walkingKm, adjusted.size, totalMinutes, adjusted)
            _selectedRoute.value = route.copy(
                budget = adjusted.sumOf { it.cost },
                duration = routeDurationLabel(totalMinutes),
                effort = effortLabel(effortLevel),
                effortLevel = effortLevel,
                stops = adjusted.size,
                imageUrl = adjusted.firstOrNull()?.imageUrl ?: route.imageUrl,
                highlights = adjusted.take(3).map { it.name }
            )
            _isLoading.value = false
        }
    }

    fun buildShareIntent(): Intent {
        val route = _selectedRoute.value
        val dest = _selectedDestination.value
        val stops = _routeStops.value

        val text = buildString {
            appendLine("Mon itinéraire ${route?.name ?: ""} - ${dest?.name ?: route?.destName ?: ""}")
            appendLine("Budget : ${route?.budget ?: 0} € | Durée : ${route?.duration ?: ""}")
            appendLine()
            stops.forEachIndexed { i, s ->
                appendLine("${i + 1}. ${s.arrivalTime} - ${s.name} (${s.type})")
            }
            appendLine()
            appendLine("Généré par TravelPath")
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Itinéraire ${dest?.name ?: route?.destName ?: ""}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
    }

    fun cacheCurrentRoute() {
        val route = _selectedRoute.value ?: return
        val stops = _routeStops.value
        if (stops.isEmpty()) return
        val key = "${_selectedDestination.value?.name ?: route.destName.ifBlank { "dest" }}_${route.id}"
        localStorage?.cacheRoute(key, route, stops)
    }

    suspend fun cacheCurrentRouteLite(): Boolean {
        val route = _selectedRoute.value ?: return false
        val stops = _routeStops.value
        if (stops.isEmpty()) return false
        val key = "${_selectedDestination.value?.name ?: route.destName.ifBlank { "dest" }}_${route.id}"
        localStorage?.cacheRouteLite(key, route, stops) ?: return false
        return true
    }

    fun fetchWeather() {
        val dest = _selectedDestination.value ?: return
        if (dest.lat == 0.0 && dest.lng == 0.0) {

            val firstStop = _routeStops.value.firstOrNull { it.lat != 0.0 } ?: return
            fetchWeatherAt(firstStop.lat, firstStop.lng)
        } else {
            fetchWeatherAt(dest.lat, dest.lng)
        }
    }

    private fun fetchWeatherAt(lat: Double, lng: Double) {
        viewModelScope.launch {
            _weather.value = weatherService.getWeather(lat, lng)
        }
    }

    fun exportPdf(context: Context) {
        val route = _selectedRoute.value ?: return
        val stops = _routeStops.value
        val destName = _selectedDestination.value?.name ?: "Destination"
        viewModelScope.launch {
            val file = pdfService.exportItinerary(context, route, stops, destName)
            _pdfExportPath.value = file
        }
    }

    fun clearPdfExport() {
        _pdfExportPath.value = null
    }
}
