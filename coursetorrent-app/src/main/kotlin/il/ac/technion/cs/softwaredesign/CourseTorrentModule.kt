package il.ac.technion.cs.softwaredesign
import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory

import DB_Manager
import com.google.inject.Guice
import dev.misfitlabs.kotlinguice4.KotlinModule
import dev.misfitlabs.kotlinguice4.getInstance
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule

class CourseTorrentModule : KotlinModule() {
    //private val injector = Guice.createInjector(SecureStorageModule())
    //private val f = injector.getInstance<SecureStorageFactory>()
    override fun configure() {
        install(SecureStorageModule())
        //bind<SecureStorageFactory>().to<>()
    }
}