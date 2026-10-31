package ac.cr.utn.campusgo.presentation

import ac.cr.utn.campusgo.domain.entities.Place

data class PlaceUiState(
    val places: List<Place> = emptyList(),
    val searchText: String = "",
    val selectedCategory: String = ALL_CATEGORIES,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

const val ALL_CATEGORIES = "__all__"