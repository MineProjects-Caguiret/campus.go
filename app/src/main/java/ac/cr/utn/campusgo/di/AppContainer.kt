package ac.cr.utn.campusgo.di

import android.content.Context
import ac.cr.utn.campusgo.data.datasource.local.InternalPhotoLocalDataSource
import ac.cr.utn.campusgo.data.datasource.local.SharedPreferencesPlaceDataSource
import ac.cr.utn.campusgo.data.repositories.PhotoRepositoryImpl
import ac.cr.utn.campusgo.data.repositories.PlaceRepositoryImpl
import ac.cr.utn.campusgo.domain.services.PhotoService
import ac.cr.utn.campusgo.domain.services.PlaceService

class AppContainer(context: Context) {

    private val applicationContext = context.applicationContext

    private val placeLocalDataSource by lazy {
        SharedPreferencesPlaceDataSource(applicationContext)
    }

    private val photoLocalDataSource by lazy {
        InternalPhotoLocalDataSource(applicationContext)
    }

    private val placeRepository by lazy {
        PlaceRepositoryImpl(placeLocalDataSource)
    }

    private val photoRepository by lazy {
        PhotoRepositoryImpl(photoLocalDataSource)
    }

    val photoService: PhotoService by lazy {
        PhotoService(photoRepository)
    }

    val placeService: PlaceService by lazy {
        PlaceService(placeRepository, photoService)
    }
}