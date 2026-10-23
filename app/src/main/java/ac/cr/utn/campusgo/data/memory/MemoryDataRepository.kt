package ac.cr.utn.campusgo.data.memory

import ac.cr.utn.campusgo.domain.entities.Place
import ac.cr.utn.campusgo.domain.repositories.PlaceRepository

class MemoryDataRepository : PlaceRepository {

    private val places = linkedMapOf<String, Place>()

    @Synchronized
    override fun getAll(): List<Place> = places.values.toList()

    @Synchronized
    override fun save(place: Place) {
        require(place.id.isNotBlank()) {
            "The place identifier cannot be blank."
        }
        places[place.id] = place
    }

    @Synchronized
    override fun delete(id: String) {
        require(id.isNotBlank()) {
            "The place identifier cannot be blank."
        }
        places.remove(id)
    }
}
