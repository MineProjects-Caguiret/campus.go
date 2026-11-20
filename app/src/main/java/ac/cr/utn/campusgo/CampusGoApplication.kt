package ac.cr.utn.campusgo

import android.app.Application
import ac.cr.utn.campusgo.di.AppContainer

class CampusGoApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(applicationContext)
    }
}