package il.ac.technion.cs.softwaredesign

import com.google.inject.Guice
import com.natpryce.hamkrest.*
import com.natpryce.hamkrest.assertion.assertThat
import dev.misfitlabs.kotlinguice4.getInstance
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class CourseTorrentTestHW1 {
    //companion object {
        private val injector = Guice.createInjector(CourseTorrentModule())
        private val torrent = injector.getInstance<CourseTorrent>()
        private val debian = this::class.java.getResource("/debian-10.3.0-amd64-netinst.iso.torrent").readBytes()
        private val lame = this::class.java.getResource("/lame.torrent").readBytes()
        private val server = SimpleHttpServer()

//        @BeforeAll
//        @JvmStatic
//        internal fun initServer() {
//            server.start()
//        }
    //}
    @Test
    fun `after load, infohash calculated correctly`() {
        val infohash = torrent.load(debian)

        assertThat(infohash, equalTo("5a8062c076fa85e8056451c0d9aa04349ae27909"))
        //torrent.unload(infohash)
    }

    @Test
    fun `after load, announce is correct`() {
        val infohash = torrent.load(debian)

        val announces = assertDoesNotThrow { torrent.announces(infohash) }

        assertThat(announces, allElements(hasSize(equalTo(1))))
        assertThat(announces, hasSize(equalTo(1)))
        assertThat(announces, allElements(hasElement("http://bttracker.debian.org:6969/announce")))
        //torrent.unload(infohash)
    }

    @Test
    fun `client announces to tracker`() {
        //val infohash = torrent.load(lame)
        val infohash = torrent.load(debian)

        /* interval is 360 */
        val interval = torrent.announce(infohash, TorrentEvent.STARTED, 0, 0, 0)

        //assertThat(interval, equalTo(360))
        assertThat(interval, equalTo(900))
        /* Assertion to verify that the tracker was actually called */
    }

    @Test
    fun `client scrapes tracker and updates statistics`() {
        //val infohash = torrent.load(lame)
        val infohash = torrent.load(debian)

        /* Tracker has infohash, 0 complete, 0 downloaded, 0 incomplete, no name key */
        assertDoesNotThrow { torrent.scrape(infohash) }

        assertThat(
            torrent.trackerStats(infohash),
            equalTo(mapOf(Pair("http://bttracker.debian.org:6969/announce", Scrape(783, 18230, 3, null) as ScrapeData)))
            //equalTo(mapOf(Pair("https://127.0.0.1:8082/announce", Scrape(0, 0, 0, null) as il.ac.technion.cs.softwaredesign.ScrapeData)))
        )
        /* Assertion to verify that the tracker was actually called */
    }

    @Test
    fun `after announce, client has up-to-date peer list`() {
        //val infohash = torrent.load(lame)
        val infohash = torrent.load(debian)

        /* Returned peer list is: [("127.0.0.22", 6887)] */
        torrent.announce(infohash, TorrentEvent.STARTED, 0, 0, 2703360)
        /* Returned peer list is: [("127.0.0.22", 6887), ("127.0.0.21", 6889)] */
        torrent.announce(infohash, TorrentEvent.REGULAR, 0, 81920, 2621440)


        assertThat(
            torrent.knownPeers(infohash),
            anyElement(has(KnownPeer::ip, equalTo("127.0.0.22")) and has(KnownPeer::port, equalTo(6887)))
        )
        assertThat(
            torrent.knownPeers(infohash),
            anyElement(has(KnownPeer::ip, equalTo("127.0.0.21")) and has(KnownPeer::port, equalTo(6889)))
        )
        assertThat(
            torrent.knownPeers(infohash), equalTo(torrent.knownPeers(infohash).distinct())
        )
    }

    @Test
    fun `peers are invalidated correctly`() {
        val infohash = torrent.load(lame)
        /* Returned peer list is: [("127.0.0.22", 6887)] */
        torrent.announce(infohash, TorrentEvent.STARTED, 0, 0, 2703360)
        /* Returned peer list is: [("127.0.0.22", 6887), ("127.0.0.21", 6889)] */
        torrent.announce(infohash, TorrentEvent.REGULAR, 0, 81920, 2621440)

        torrent.invalidatePeer(infohash, KnownPeer("127.0.0.22", 6887, null))

        assertThat(
            torrent.knownPeers(infohash),
            anyElement(has(KnownPeer::ip, equalTo("127.0.0.22")) and has(KnownPeer::port, equalTo(6887))).not()
        )
    }
}