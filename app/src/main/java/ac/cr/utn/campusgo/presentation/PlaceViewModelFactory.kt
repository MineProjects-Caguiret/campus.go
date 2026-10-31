package ac.cr.utn.campusgo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ac.cr.utn.campusgo.domain.services.PlaceService

class PlaceViewModelFactory(
    private val placeService: PlaceService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(PlaceViewModel::class.java)) {
            throw IllegalArgumentException(
                "Unsupported ViewModel class: ${modelClass.name}"
            )
        }

        return PlaceViewModel(placeService) as T
    }
}