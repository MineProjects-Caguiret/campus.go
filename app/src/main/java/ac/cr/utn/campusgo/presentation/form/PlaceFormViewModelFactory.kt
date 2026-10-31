
package ac.cr.utn.campusgo.presentation.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ac.cr.utn.campusgo.domain.services.PhotoService
import ac.cr.utn.campusgo.domain.services.PlaceService

class PlaceFormViewModelFactory(
    private val placeService: PlaceService,
    private val photoService: PhotoService
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(PlaceFormViewModel::class.java)) {
            throw IllegalArgumentException(
                "Unsupported ViewModel class: ${modelClass.name}"
            )
        }

        return PlaceFormViewModel(
            placeService,
            photoService
        ) as T
    }
}
