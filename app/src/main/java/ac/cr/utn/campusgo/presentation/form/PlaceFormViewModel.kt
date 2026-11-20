package ac.cr.utn.campusgo.presentation.form

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ac.cr.utn.campusgo.domain.entities.Place
import ac.cr.utn.campusgo.domain.services.PhotoService
import ac.cr.utn.campusgo.domain.services.PlaceService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class PlaceFormViewModel(
    private val placeService: PlaceService,
    private val photoService: PhotoService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaceFormUiState())
    val uiState: StateFlow<PlaceFormUiState> = _uiState.asStateFlow()

    fun updateName(value: String) {
        _uiState.update { it.copy(name = value, errorMessage = null) }
    }

    fun updateCategory(value: String) {
        _uiState.update { it.copy(category = value, errorMessage = null) }
    }

    fun updateDescription(value: String) {
        _uiState.update { it.copy(description = value, errorMessage = null) }
    }

    fun updateLatitude(value: String) {
        _uiState.update { it.copy(latitude = value, errorMessage = null) }
    }

    fun updateLongitude(value: String) {
        _uiState.update { it.copy(longitude = value, errorMessage = null) }
    }

    fun updateImageUri(value: String?) {
        _uiState.update { it.copy(imageUri = value, errorMessage = null) }
    }

    fun loadPlace(placeId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null, isSaved = false)
            }

            try {
                val place = withContext(Dispatchers.IO) {
                    placeService.getPlaces().firstOrNull { it.id == placeId }
                } ?: throw NoSuchElementException("The requested place was not found.")

                _uiState.update {
                    it.copy(
                        placeId = place.id,
                        name = place.name,
                        category = place.category,
                        description = place.description,
                        latitude = place.latitude?.toString() ?: "",
                        longitude = place.longitude?.toString() ?: "",
                        imageUri = place.imageUri,
                        errorMessage = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: "Unable to load the place.")
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun savePhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, errorMessage = null)
            }

            try {
                val uri = withContext(Dispatchers.IO) {
                    photoService.save(bitmap)
                }

                updateImageUri(uri.toString())
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: "Unable to save the photo.")
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun savePlace() {
        val state = _uiState.value

        val latitudeText = state.latitude.trim()
        val longitudeText = state.longitude.trim()

        val latitude = if (latitudeText.isEmpty()) {
            null
        } else {
            latitudeText.toDoubleOrNull()
        }

        val longitude = if (longitudeText.isEmpty()) {
            null
        } else {
            longitudeText.toDoubleOrNull()
        }

        if ((latitudeText.isNotEmpty() && latitude == null) ||
            (longitudeText.isNotEmpty() && longitude == null)
        ) {
            _uiState.update {
                it.copy(errorMessage = "Ingresá coordenadas numéricas válidas.")
            }
            return
        }

        val place = Place(
            id = state.placeId ?: UUID.randomUUID().toString(),
            name = state.name,
            category = state.category,
            description = state.description.trim(),
            latitude = latitude,
            longitude = longitude,
            imageUri = state.imageUri
        )

        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, isSaved = false, errorMessage = null)
            }

            try {
                withContext(Dispatchers.IO) {
                    placeService.savePlace(place)
                }

                _uiState.update {
                    it.copy(isSaved = true, errorMessage = null)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = exception.message
                            ?: "No se pudo guardar el lugar."
                    )
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearSavedState() {
        _uiState.update { it.copy(isSaved = false) }
    }
}