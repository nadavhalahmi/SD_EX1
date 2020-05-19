package il.ac.technion.cs.softwaredesign

import Coder
import ITorrentHTTP
import com.google.inject.Guice
import com.natpryce.hamkrest.*
import com.natpryce.hamkrest.assertion.assertThat
import dev.misfitlabs.kotlinguice4.getInstance
import io.mockk.every
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class MyTest {
    //companion object {
    private val injector = Guice.createInjector(TestModule())
    private val torrent = injector.getInstance<CourseTorrent>()
    private val debian = this::class.java.getResource("/debian-10.3.0-amd64-netinst.iso.torrent").readBytes()
    private val lame = this::class.java.getResource("/lame.torrent").readBytes()
    private val slack = this::class.java.getResource("/Slackware64_14.1.torrent").readBytes()
    //private val server = SimpleHttpServer()
    private val torrentHTTPMock = injector.getInstance<ITorrentHTTP>()
    private val testUtils = TestUtils()
    private val coder = Coder()

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

        val infohash = torrent.load(lame)
        var resp = "d8:intervali360e5:peers"
        val peers = listOf(Pair("127.0.0.22", 6887))
        resp += "6:"+ testUtils.buildPeersValueAsBinaryString(peers) + "e"

        //not sure my ISO... and not UTF8 but it works
        every {torrentHTTPMock.get(any(), any())} returns resp.toByteArray(Charsets.ISO_8859_1)
        /* interval is 360 */
        val interval = torrent.announce(infohash, TorrentEvent.STARTED, 0, 0, 0)

        assertThat(interval, equalTo(360))
        //assertThat(interval, equalTo(900))
        /* Assertion to verify that the tracker was actually called */
    }

    @Test
    fun `client scrapes tracker and updates statistics`() {
        val infohash = torrent.load(lame)
        //val infohash = torrent.load(debian)

        var resp = "d5:flagsd20:min_request_intervali360ee5:filesd20:"
        resp += coder.binary_encode(infohash, simple = true)
        resp += "d8:completei0e10:incompletei0e10:downloadedi0eeee"

        //not sure my ISO... and not UTF8 but it works
        every {torrentHTTPMock.get(any(), any())} returns resp.toByteArray(Charsets.ISO_8859_1)

        /* Tracker has infohash, 0 complete, 0 downloaded, 0 incomplete, no name key */
        assertDoesNotThrow { torrent.scrape(infohash) }

        assertThat(
                torrent.trackerStats(infohash),
                //equalTo(mapOf(Pair("http://bttracker.debian.org:6969/announce", Scrape(783, 18230, 3, null) as il.ac.technion.cs.softwaredesign.ScrapeData)))
                equalTo(mapOf(Pair("https://127.0.0.1:8082/announce", Scrape(0, 0, 0, null) as ScrapeData)))
        )
        /* Assertion to verify that the tracker was actually called */
    }

    @Test
    fun `after announce, client has up-to-date peer list`() {
        val infohash = torrent.load(lame)
        //val infohash = torrent.load(debian)

        var resp = "d8:intervali360e5:peers"
        val peers1 = listOf(Pair("127.0.0.22", 6887))
        val peersString1 = "6:"+ testUtils.buildPeersValueAsBinaryString(peers1) + "e"
        val resp1 = resp + peersString1
        val peers2= listOf(Pair("127.0.0.22", 6887), Pair("127.0.0.21", 6889))
        val peersString2 = "12:"+ testUtils.buildPeersValueAsBinaryString(peers2) + "e"
        val resp2 = resp + peersString2

        //not sure my ISO... and not UTF8 but it works
        every {torrentHTTPMock.get(any(), any())} returns resp1.toByteArray(Charsets.ISO_8859_1)
        /* Returned peer list is: [("127.0.0.22", 6887)] */
        torrent.announce(infohash, TorrentEvent.STARTED, 0, 0, 2703360)

        //not sure my ISO... and not UTF8 but it works
        every {torrentHTTPMock.get(any(), any())} returns resp2.toByteArray(Charsets.ISO_8859_1)
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

        var resp = "d8:intervali360e5:peers"
        val peers1 = listOf(Pair("127.0.0.22", 6887))
        val peersString1 = "6:"+ testUtils.buildPeersValueAsBinaryString(peers1) + "e"
        val resp1 = resp + peersString1
        val peers2= listOf(Pair("127.0.0.22", 6887), Pair("127.0.0.21", 6889))
        val peersString2 = "12:"+ testUtils.buildPeersValueAsBinaryString(peers2) + "e"
        val resp2 = resp + peersString2

        //not sure my ISO... and not UTF8 but it works
        every {torrentHTTPMock.get(any(), any())} returns resp1.toByteArray(Charsets.ISO_8859_1)
        /* Returned peer list is: [("127.0.0.22", 6887)] */
        torrent.announce(infohash, TorrentEvent.STARTED, 0, 0, 2703360)

        //not sure my ISO... and not UTF8 but it works
        every {torrentHTTPMock.get(any(), any())} returns resp2.toByteArray(Charsets.ISO_8859_1)
        /* Returned peer list is: [("127.0.0.22", 6887), ("127.0.0.21", 6889)] */
        torrent.announce(infohash, TorrentEvent.REGULAR, 0, 81920, 2621440)

        torrent.invalidatePeer(infohash, KnownPeer("127.0.0.22", 6887, null))

        assertThat(
                torrent.knownPeers(infohash),
                anyElement(has(KnownPeer::ip, equalTo("127.0.0.22")) and has(KnownPeer::port, equalTo(6887))).not()
        )
    }

    @Test
    fun `client recieve announce error`() {
        val infohash = torrent.load(slack)

        every {torrentHTTPMock.get(any(), any())} throws Exception("announce error")

        assertThrows<Exception> { torrent.announce(infohash, TorrentEvent.STARTED, 0, 0, 0) }

    }

    @Test
    fun `after shuffle still returns same announce list`() {
        val infohash = torrent.load(slack)

        var resp = "d8:intervali360e5:peers"
        val peers = listOf(Pair("127.0.0.22", 6887))
        resp += "6:"+ testUtils.buildPeersValueAsBinaryString(peers) + "e"

        //not sure my ISO... and not UTF8 but it works
        every {torrentHTTPMock.get(any(), any())} returns resp.toByteArray(Charsets.ISO_8859_1)

        //torrent.announce(infohash, TorrentEvent.STARTED, 0, 0, 0)

        val before = torrent.announces(infohash)
        torrent.announce(infohash, TorrentEvent.STARTED, 0, 0, 0)
        val after = torrent.announces(infohash)
        var beforeSet = HashSet<String>()
        var afterSet = HashSet<String>()
        for(l in before){
            for(s in l){
                beforeSet.add(s)
            }
        }
        for(l in after){
            for(s in l){
                afterSet.add(s)
            }
        }
        assert(beforeSet == afterSet)
    }

    @Test
    fun `check peers as list`() {
        val infohash = torrent.load(lame)
        //val infohash = torrent.load(debian)

        var resp = "d8:intervali600e12:min intervali30e8:completei2e10:incompletei3e" +
                "5:peersl" +
                "d2:ip13:73.66.138.2174:porti8999ee" +
                "d2:ip13:73.66.138.2174:porti63014ee" +
                "d2:ip13:73.66.138.2174:porti8999ee" +
                "d2:ip13:73.25.106.1804:porti6881ee" +
                "d2:ip13:73.66.249.1414:porti8999eeee"

        //not sure my ISO... and not UTF8 but it works
        every {torrentHTTPMock.get(any(), any())} returns resp.toByteArray(Charsets.ISO_8859_1)
        torrent.announce(infohash, TorrentEvent.STARTED, 0, 0, 2703360)


        assertThat(
                torrent.knownPeers(infohash),
                anyElement(has(KnownPeer::ip, equalTo("73.66.138.217")) and has(KnownPeer::port, equalTo(8999)))
        )
        assertThat(
                torrent.knownPeers(infohash),
                anyElement(has(KnownPeer::ip, equalTo("73.66.138.217")) and has(KnownPeer::port, equalTo(63014)))
        )
        assertThat(
                torrent.knownPeers(infohash),
                anyElement(has(KnownPeer::ip, equalTo("73.66.138.217")) and has(KnownPeer::port, equalTo(8999)))
        )
        assertThat(
                torrent.knownPeers(infohash),
                anyElement(has(KnownPeer::ip, equalTo("73.25.106.180")) and has(KnownPeer::port, equalTo(6881)))
        )
        assertThat(
                torrent.knownPeers(infohash),
                anyElement(has(KnownPeer::ip, equalTo("73.66.249.141")) and has(KnownPeer::port, equalTo(8999)))
        )
        assertThat(
                torrent.knownPeers(infohash), equalTo(torrent.knownPeers(infohash).distinct())
        )
    }

    @Test
    fun `check announce failure reason`() {
        val infohash = torrent.load(slack)

        var resp = "d14:failure reason20:unregistered torrente"

        //not sure my ISO... and not UTF8 but it works
        every {torrentHTTPMock.get(any(), any())} returns resp.toByteArray(Charsets.ISO_8859_1)

        //torrent.announce(infohash, TorrentEvent.STARTED, 0, 0, 0)


        torrent.announce(infohash, TorrentEvent.STARTED, 0, 0, 0)

    }
}