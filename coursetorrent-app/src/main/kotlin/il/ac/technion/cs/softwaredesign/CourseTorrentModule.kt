package il.ac.technion.cs.softwaredesign
import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory

import DB_Manager
import MyStorage
import MyStorageFactory
import com.google.inject.Guice
import dev.misfitlabs.kotlinguice4.KotlinModule
import dev.misfitlabs.kotlinguice4.getInstance
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule
import il.ac.technion.cs.softwaredesign.storage.impl.SecureStorageFactoryImpl
import java.nio.charset.Charset

class CourseTorrentModule : KotlinModule() {
    override fun configure() {
        //install(SecureStorageModule())
        bind<SecureStorageFactory>().to<MyStorageFactory>()
        bind<SecureStorage>().to<MyStorage>()
    }
}


