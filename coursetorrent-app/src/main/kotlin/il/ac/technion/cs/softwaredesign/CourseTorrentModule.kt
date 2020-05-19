package il.ac.technion.cs.softwaredesign

import dev.misfitlabs.kotlinguice4.KotlinModule
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule

class CourseTorrentModule : KotlinModule() {
    override fun configure() {
        install(SecureStorageModule())
//        bind<SecureStorageFactory>().to<MyStorageFactory>()
//        bind<SecureStorage>().to<MyStorage>()
//        bind<ITorrentHTTP>().to<TorrentHTTPDummyImpl>()
//        bind<ITorrentHTTP>().toInstance(mockk<TorrentHTTP>()) //TODO: DONT forget to use real implementation
    }
}


