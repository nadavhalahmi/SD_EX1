package il.ac.technion.cs.softwaredesign
import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory

import DB_Manager
import ITorrentHTTP
import MyStorage
import MyStorageFactory
import TorrentHTTP
import TorrentHTTPDummyImpl
import com.google.inject.Guice
import dev.misfitlabs.kotlinguice4.KotlinModule
import dev.misfitlabs.kotlinguice4.getInstance
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule
import il.ac.technion.cs.softwaredesign.storage.impl.SecureStorageFactoryImpl
import io.mockk.mockk
import java.nio.charset.Charset

class CourseTorrentModule : KotlinModule() {
    override fun configure() {
        //install(SecureStorageModule())
        bind<SecureStorageFactory>().to<MyStorageFactory>()
        bind<SecureStorage>().to<MyStorage>()
        //bind<ITorrentHTTP>().to<TorrentHTTPDummyImpl>()
        bind<ITorrentHTTP>().toInstance(mockk<TorrentHTTP>()) //TODO: DONT forget to use real implementation
    }
}


