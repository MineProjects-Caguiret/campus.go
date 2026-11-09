package ac.cr.utn.campusgo.presentation.form

import ac.cr.utn.campusgo.domain.entities.Place

data class PlaceFormUiState(
    val placeId: String? = null,
    val name: String = "",
    val category: String = "Edificio",
    val description: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val imageUri: String? = null,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
) {
    fun toPlace(): Place {
        val latitudeValue = latitude.trim().toDoubleOrNull()
        val longitudeValue = longitude.trim().toDoubleOrNull()

        return Place(
            id = placeId ?: java.util.UUID.randomUUID().toString(),
            name = name.trim(),
            category = category,
            description = description.trim(),
            latitude = latitudeValue,
            longitude = longitudeValue,
            imageUri = imageUri
        )
    }
}