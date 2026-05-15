package com.example.traveling.features.travelpath

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TravelPathScreen(
    isAnonymous: Boolean = false,
    initialDestination: String? = null,
    initialTravelSharePostId: String? = null,
    resetOnEnterToken: Int = 0,
    onTravelShareSeedConsumed: () -> Unit = {},
    onOpenPhotoDetail: (String) -> Unit = {},
    travelViewModel: TravelViewModel = viewModel()
) {
    val step by travelViewModel.currentStep.collectAsState()
    val selectedRouteId by travelViewModel.currentRouteId.collectAsState()

    LaunchedEffect(resetOnEnterToken) {
        if (resetOnEnterToken > 0 && initialTravelSharePostId.isNullOrBlank()) {
            travelViewModel.resetPlanningState()
        }
    }

    LaunchedEffect(initialTravelSharePostId) {
        if (!initialTravelSharePostId.isNullOrBlank()) {
            travelViewModel.applyTravelSharePostSeed(initialTravelSharePostId)
            travelViewModel.setStep("preferences")
            onTravelShareSeedConsumed()
        }
    }

    when {
        step == "detail" && selectedRouteId != null -> {
            RouteDetailScreen(
                routeId = selectedRouteId!!,
                onBack = {
                    travelViewModel.setCurrentRouteId(null)
                    travelViewModel.setStep("results")
                },
                onOpenPhotoDetail = onOpenPhotoDetail
            )
        }
        step == "loading" -> LoadingScreen()
        step == "results" -> ResultsScreen(
            travelViewModel = travelViewModel,
            onBack = { travelViewModel.resetPlanningState() },
            onViewDetail = { id ->
                travelViewModel.selectRoute(id)
                travelViewModel.setCurrentRouteId(id)
                travelViewModel.setStep("detail")
            }
        )
        else -> key(resetOnEnterToken) {
            PreferencesForm(
                initialDestination = initialDestination,
                travelViewModel = travelViewModel,
                onGenerate = {
                    travelViewModel.setStep("loading")
                },
                onLoadingComplete = { travelViewModel.setStep("results") }
            )
        }
    }

    if (step == "loading") {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1800)
            travelViewModel.setStep("results")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TravelPathScreenPreview() {
    TravelPathScreen(
        isAnonymous = false,
        initialDestination = null
    )
}
