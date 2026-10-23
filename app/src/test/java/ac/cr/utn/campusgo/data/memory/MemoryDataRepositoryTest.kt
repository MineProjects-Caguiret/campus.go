package ac.cr.utn.campusgo.data.memory

import ac.cr.utn.campusgo.domain.entities.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryDataRepositoryTest {

    private fun createPlace(
        id: String = "place-1",
        name: String = "Central Library"
    ) = Place(
        id = id,
        name = name,
        category = "Biblioteca",
        description = "Campus library",
        latitude = null,
        longitude = null,
        imageUri = null
    )

    @Test
    fun saveAndGetAllReturnsSavedPlace() {
        val repository = MemoryDataRepository()
        val place = createPlace()

        repository.save(place)

        assertEquals(listOf(place), repository.getAll())
    }

    @Test
    fun saveWithExistingIdReplacesPlace() {
        val repository = MemoryDataRepository()
        repository.save(createPlace(name = "Old name"))

        val updated = createPlace(name = "New name")
        repository.save(updated)

        assertEquals(listOf(updated), repository.getAll())
    }

    @Test
    fun deleteRemovesPlace() {
        val repository = MemoryDataRepository()
        repository.save(createPlace())

        repository.delete("place-1")

        assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun saveRejectsBlankId() {
        val repository = MemoryDataRepository()

        assertThrows(IllegalArgumentException::class.java) {
            repository.save(createPlace(id = " "))
        }
    }

    @Test
    fun deleteRejectsBlankId() {
        val repository = MemoryDataRepository()

        assertThrows(IllegalArgumentException::class.java) {
            repository.delete(" ")
        }
    }
}
