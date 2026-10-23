package ac.cr.utn.campusgo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ac.cr.utn.campusgo.domain.entities.Place
import ac.cr.utn.campusgo.domain.services.PlaceService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaceViewModel(
    private val service: PlaceService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaceUiState())
    val uiState: StateFlow<PlaceUiState> = _uiState.asStateFlow()

    private var allPlaces: List<Place> = emptyList()

    init {
        loadPlaces()
    }

    fun loadPlaces() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null)
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    service.getPlaces()
                }

                allPlaces = result
                publishPlaces()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                reportError(exception, "Unable to load places.")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateSearch(query: String) {
        _uiState.update { it.copy(searchText = query) }
        publishPlaces()
    }

    fun updateCategory(category: String) {
        _uiState.update {
            it.copy(selectedCategory = category)
        }
        publishPlaces()
    }

    fun savePlace(place: Place) {
        executeOperation("Unable to save the place.") {
            service.savePlace(place)
            allPlaces = service.getPlaces()
            publishPlaces()
        }
    }

    fun deletePlace(id: String) {
        executeOperation("Unable to delete the place.") {
            service.deletePlace(id)
            allPlaces = service.getPlaces()
            publishPlaces()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun executeOperation(
        fallbackMessage: String,
        operation: suspend () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null)
            }

            try {
                withContext(Dispatchers.IO) {
                    operation()
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                reportError(exception, fallbackMessage)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun publishPlaces() {
        val state = _uiState.value
        val query = state.searchText.trim()

        val filtered = allPlaces
            .asSequence()
            .filter { place ->
                state.selectedCategory == ALL_CATEGORIES ||
                        place.category == state.selectedCategory
            }
            .filter { place ->
                val searchable =
                    "${place.name} ${place.description} ${place.category}"
                searchable.contains(query, ignoreCase = true)
            }
            .sortedBy { it.name.lowercase() }
            .toList()

        _uiState.update { it.copy(places = filtered) }
    }

    private fun reportError(
        exception: Exception,
        fallbackMessage: String
    ) {
        _uiState.update {
            it.copy(
                errorMessage = exception.message
                    ?.takeIf(String::isNotBlank)
                    ?: fallbackMessage
            )
        }
    }
}