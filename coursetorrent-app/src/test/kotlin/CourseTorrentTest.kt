package il.ac.technion.cs.softwaredesign

import MyByteArray
import com.google.inject.Guice
import com.natpryce.hamkrest.allElements
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasElement
import com.natpryce.hamkrest.hasSize
import dev.misfitlabs.kotlinguice4.getInstance

//import io.mockk.every
//
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.util.Formatter;
//import io.mockk.mockk
//import io.mockk.mockkStatic
//import io.mockk.slot
//import net.bytebuddy.asm.Advice
import org.junit.jupiter.api.*


class CourseTorrentTest {
    companion object {
        private val injector = Guice.createInjector(CourseTorrentModule())
        private val torrent = injector.getInstance<CourseTorrent>()
        private val debian = this::class.java.getResource("/debian-10.3.0-amd64-netinst.iso.torrent").readBytes()
        private val lame = this::class.java.getResource("/lame.torrent").readBytes()
        private val announceList2Urls = this::class.java.getResource("/announce-list2urls.torrent").readBytes()
        private val my_db = HashMap<MyByteArray, MyByteArray>()
//        private val keySlot = slot<ByteArray>()
//        private val valueSlot = slot<ByteArray>()
        private val charset = Charsets.UTF_8
//        @BeforeAll
//        @JvmStatic
//        internal fun initMocks() {
//            mockkStatic("il.ac.technion.cs.softwaredesign.storage.SecureStorageKt")
//            every { write(key=capture(keySlot), value = capture(valueSlot)) } answers
//                    {my_db[MyByteArray(keySlot.captured)] = MyByteArray(valueSlot.captured)
//                        println("writing key "+keySlot.captured.toString(charset))}
//            every { read(key=capture(keySlot)) } answers {
//                println("reading key "+keySlot.captured.toString(charset))
//                if(my_db.containsKey(MyByteArray(keySlot.captured)))
//                    my_db[MyByteArray(keySlot.captured)]?.arr
//                else null  }
//        }


    }
    @Test
    fun `after load, infohash calculated correctly`() {
        val infohash = torrent.load(debian)

        assertThat(infohash, equalTo("5a8062c076fa85e8056451c0d9aa04349ae27909"))
        torrent.unload(infohash)
    }

    @Test
    fun `after load, announce is correct`() {
        val infohash = torrent.load(debian)

        val announces = torrent.announces(infohash)

        assertThat(announces, allElements(hasSize(equalTo(1))))
        assertThat(announces, hasSize(equalTo(1)))
        assertThat(announces, allElements(hasElement("http://bttracker.debian.org:6969/announce")))
        torrent.unload(infohash)
    }

    @Test
    fun `after unload, torrent in unloaded`() {
        val infohash = torrent.load(debian)

        torrent.unload(infohash)

        assertThrows<IllegalArgumentException> { torrent.unload(infohash) }
    }

    @Test
    fun `load unload load unload does not crash`(){
        val infohash = torrent.load(debian)
        torrent.unload(infohash)
        torrent.load(debian)
        torrent.unload(infohash)
    }

    @Test
    fun `announce-list 2 urls`() {
        val infohash = torrent.load(announceList2Urls)

        val announces = torrent.announces(infohash)

        assertThat(announces, allElements(hasSize(equalTo(2))))
        assertThat(announces, hasSize(equalTo(1)))
        assertThat(announces, allElements(hasElement("http://bttracker.debian.org:6969/announce")))
        assertThat(announces, allElements(hasElement("http://bttracker.debian.org:6969/announce2")))
        torrent.unload(infohash)
    }

    @Test
    fun `load load crashes`(){
        val infohash = torrent.load(debian)
        assertThrows<IllegalStateException> { torrent.load(debian) }
        torrent.unload(infohash)
    }

    @Test
    fun `load two torrents works`(){
        val infohash1 = torrent.load(debian)
        val infohash2 = torrent.load(announceList2Urls)
        val announce1 = torrent.announces(infohash1)
        val announce2 = torrent.announces(infohash2)
        assertThat(announce1, allElements(hasElement("http://bttracker.debian.org:6969/announce")))
        assertThat(announce2, allElements(hasElement("http://bttracker.debian.org:6969/announce")))
        assertThat(announce2, allElements(hasElement("http://bttracker.debian.org:6969/announce2")))
        torrent.unload(infohash2)
        torrent.unload(infohash1)
    }

    @Test
    fun `after unload, cant get announce`(){
        val infohash = torrent.load(debian)
        torrent.unload(infohash)
        assertThrows<IllegalArgumentException> { torrent.announces(infohash) }
    }
}