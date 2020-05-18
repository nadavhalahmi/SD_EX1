package il.ac.technion.cs.softwaredesign

import Coder
import DB_Manager
import ITorrentHTTP
import TorrentDict
import TorrentHTTP
import TorrentList
import TorrentParser
import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.exceptions.TrackerException
import java.net.URL
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * This is the class implementing CourseTorrent, a BitTorrent client.
 *
 * Currently specified:
 * + Parsing torrent metainfo files (".torrent" files)
 * + Communication with trackers (announce, scrape).
 */
class CourseTorrent @Inject constructor(private val dbManager: DB_Manager, private val torrentHTTP: ITorrentHTTP) {
    private val parser = TorrentParser()
    private val coder = Coder()
    //private val torrentHTTP = ITorrentHTTP()
    /**
     * Load in the torrent metainfo file from [torrent]. The specification for these files can be found here:
     * [Metainfo File Structure](https://wiki.theory.org/index.php/BitTorrentSpecification#Metainfo_File_Structure).
     *
     * After loading a torrent, it will be available in the system, and queries on it will succeed.
     *
     * This is a *create* command.
     *
     * @throws IllegalArgumentException If [torrent] is not a valid metainfo file.
     * @throws IllegalStateException If the infohash of [torrent] is already loaded.
     * @return The infohash of the torrent, i.e., the SHA-1 of the `info` key of [torrent].
     */
    fun load(torrent: ByteArray): String {
        val infoValue: ByteArray
        val dict: TorrentDict
        try {
            //infoValue = parser.getValueByKey(torrent, "info")
            dict = parser.parse(torrent)
            val infoRange = dict.getRange("info")
            infoValue = torrent.copyOfRange(infoRange.startIndex(), infoRange.endIndex())
        }catch (e: Exception){
            throw IllegalArgumentException()
        }
        val infohash = coder.SHAsum(infoValue)
        if(dbManager.torrentExists(infohash))
            throw IllegalStateException()
        dbManager.addTorrent(infohash, torrent, dict)
        return infohash
    }

    /**
     * Remove the torrent identified by [infohash] from the system.
     *
     * This is a *delete* command.
     *
     * @throws IllegalArgumentException If [infohash] is not loaded.
     */
    fun unload(infohash: String): Unit {
        if(!dbManager.torrentExists(infohash))
            throw IllegalArgumentException()
        dbManager.deleteTorrent(infohash)
    }

    /**
     * Return the announce URLs for the loaded torrent identified by [infohash].
     *
     * See [BEP 12](http://bittorrent.org/beps/bep_0012.html) for more information. This method behaves as follows:
     * * If the "announce-list" key exists, it will be used as the source for announce URLs.
     * * If "announce-list" does not exist, "announce" will be used, and the URL it contains will be in tier 1.
     * * The announce URLs should *not* be shuffled.
     *
     * This is a *read* command.
     *
     * @throws IllegalArgumentException If [infohash] is not loaded.
     * @return Tier lists of announce URLs.
     */
    fun announces(infohash: String): List<List<String>> {
        var lst: ByteArray? = dbManager.getTorrent(infohash, "announce-list")
        if(lst === null) {
            lst = dbManager.getTorrent(infohash, "announce")
            if (lst === null)
                throw IllegalArgumentException()
            else
                lst = "ll".toByteArray(Charsets.UTF_8) + lst + "ee".toByteArray(Charsets.UTF_8)
        }
        return ((parser.parseList(lst).value() as TorrentList).toList() as List<List<String>>)
    }

    /**
     * Send an "announce" HTTP request to a single tracker of the torrent identified by [infohash], and update the
     * internal state according to the response. The specification for these requests can be found here:
     * [Tracker Protocol](https://wiki.theory.org/index.php/BitTorrentSpecification#Tracker_HTTP.2FHTTPS_Protocol).
     *
     * If [event] is [TorrentEvent.STARTED], shuffle the announce-list before selecting a tracker (future calls to
     * [announces] should return the shuffled list). See [BEP 12](http://bittorrent.org/beps/bep_0012.html) for more
     * information on shuffling and selecting a tracker.
     *
     * [event], [uploaded], [downloaded], and [left] should be included in the tracker request.
     *
     * The "compact" parameter in the request should be set to "1", and the implementation should support both compact
     * and non-compact peer lists.
     *
     * Peer ID should be set to "-CS1000-{Student ID}{Random numbers}", where {Student ID} is the first 6 characters
     * from the hex-encoded SHA-1 hash of the student's ID numbers (i.e., `hex(sha1(student1id + student2id))`), and
     * {Random numbers} are 6 random characters in the range [0-9a-zA-Z] generated at instance creation.
     *
     * If the connection to the tracker failed or the tracker returned a failure reason, the next tracker in the list
     * will be contacted and the announce-list will be updated as per
     * [BEP 12](http://bittorrent.org/beps/bep_0012.html).
     * If the final tracker in the announce-list has failed, then a [TrackerException] will be thrown.
     *
     * This is an *update* command.
     *
     * @throws TrackerException If the tracker returned a "failure reason". The failure reason will be the exception
     * message.
     * @throws IllegalArgumentException If [infohash] is not loaded.
     * @return The interval in seconds that the client should wait before announcing again.
     */
    fun announce(infohash: String, event: TorrentEvent, uploaded: Long, downloaded: Long, left: Long): Int{
        if(!dbManager.torrentExists(infohash))
            throw java.lang.IllegalArgumentException()
        val randLen = 6
        val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val announceList = announces(infohash = infohash)
        var respDict : TorrentDict? = null
        if(event == TorrentEvent.STARTED)
            announceList.shuffled() //TODO: CHECK suffeld in db
        for(l in announceList){
            for(tracker in l){
                val params = HashMap<String, String>()
                params["info_hash"] = coder.binary_encode(infohash)
                params["event"] = event.toString().toLowerCase()
                params["uploaded"] = uploaded.toString()
                params["downloaded"] = downloaded.toString()
                params["left"] = left.toString()
                params["compact"] = "1"
                val randomString = (1..randLen)
                        .map { kotlin.random.Random.nextInt(0, charPool.size) }
                        .map(charPool::get)
                        .joinToString("")
                val ids = coder.SHAsum(("206784258"+"314628090").toByteArray()).slice(0 until 6)
                params["peer_id"] = "-CS1000-$ids$randomString"
                var resp = torrentHTTP.get(tracker, params)
                respDict = parser.parse(resp)
                dbManager.addAnnounce(tracker=tracker, value=resp, dict=respDict)
                if(respDict["interval"] != null)
                    return (respDict["interval"]?.value() as Long).toInt()
            }
        }
        if(respDict != null)
            throw TrackerException(respDict["failure reason"]?.value().toString())
        else
            throw throw TrackerException("generic announce exception")
    }

    /**
     * Scrape all trackers identified by a torrent, and store the statistics provided. The specification for the scrape
     * request can be found here:
     * [Scrape Protocol](https://wiki.theory.org/index.php/BitTorrentSpecification#Tracker_.27scrape.27_Convention).
     *
     * All known trackers for the torrent will be scraped.
     *
     * This is an *update* command.
     *
     * @throws IllegalArgumentException If [infohash] is not loaded.
     */
    fun scrape(infohash: String): Unit{
        if(!dbManager.torrentExists(infohash))
            throw java.lang.IllegalArgumentException()
        val announceList: List<List<String>> = announces(infohash = infohash)
        for(l in announceList){
            for(tracker in l){
                if(tracker.split('/').last().startsWith("announce")) {
                    val lastIndex = tracker.lastIndexOf(char = '/')
                    val scrape = tracker.slice(0..lastIndex) + "scrape" + tracker.slice((lastIndex + "/announce".length) until tracker.length)
                    val params = HashMap<String, String>()
                    params["info_hash"] = coder.binary_encode(infohash)
                    var resp = torrentHTTP.get(scrape, params)
                    val respDict = parser.parse(resp)
                    dbManager.addScrape(hash = tracker, value = resp, dict = respDict)
//                    val files: TorrentDict = respDict["files"]?.value() as TorrentDict
//                    val data: TorrentDict = files[coder.string_to_hex(infohash)]?.value() as TorrentDict
                }
            }
        }
    }

    /**
     * Invalidate a previously known peer for this torrent.
     *
     * If [peer] is not a known peer for this torrent, do nothing.
     *
     * This is an *update* command.
     *
     * @throws IllegalArgumentException If [infohash] is not lodbaded.
     */
    fun invalidatePeer(infohash: String, peer: KnownPeer): Unit{
        if(!dbManager.torrentExists(infohash))
            throw java.lang.IllegalArgumentException()
        dbManager.deleteAnnounce(infohash+peer::peerId)
    }

    /**
     * Return all known peers for the torrent identified by [infohash], in sorted order. This list should contain all
     * the peers that the client can attempt to connect to, in ascending numerical order. Note that this is not the
     * lexicographical ordering of the string representation of the IP addresses: i.e., "127.0.0.2" should come before
     * "127.0.0.100".
     *
     * The list contains unique peers, and does not include peers that have been invalidated.
     *
     * This is a *read* command.
     *
     * @throws IllegalArgumentException If [infohash] is not loaded.
     * @return Sorted list of known peers.
     */
    fun knownPeers(infohash: String): List<KnownPeer>{
        if(!dbManager.torrentExists(infohash))
            throw java.lang.IllegalArgumentException()
        val res = ArrayList<KnownPeer>()
        val announceList = announces(infohash)
        for(l in announceList){
            for(tracker in l){
                val peersBytes = dbManager.getPeers(tracker)
                if(peersBytes != null){
                    try{
                        val peers = parser.parseList(peersBytes) //TODO: FIX
                    }catch (e: Throwable){
                        val start = (parser.parseBytes(peersBytes) {peersBytes[it].toChar() == ':'}).length + 1
                        val end = peersBytes.size
                        for(i in start until end step 6 ){
                            val (ip, port) = coder.get_ip_port(peersBytes.copyOfRange(i, i+6))
                            res.add(KnownPeer(ip = ip, port = port, peerId = null))
                        }
                    }
                }
            }
        }
        return res
    }

    /**
     * Return all known statistics from trackers of the torrent identified by [infohash]. The statistics displayed
     * represent the latest information seen from a tracker.
     *
     * The statistics are updated by [announce] and [scrape] calls. If a response from a tracker was never seen, it
     * will not be included in the result. If one of the values of [ScrapeData] was not included in any tracker response
     * (e.g., "downloaded"), it would be set to 0 (but if there was a previous result that did include that value, the
     * previous result would be shown).
     *
     * If the last response from the tracker was a failure, the failure reason would be returned ([ScrapeData] is
     * defined to allow for this). If the failure was a failed connection to the tracker, the reason should be set to
     * "Connection failed".
     *
     * This is a *read* command.
     *
     * @throws IllegalArgumentException If [infohash] is not loaded.
     * @return A mapping from tracker announce URL to statistics.
     */
    fun trackerStats(infohash: String): Map<String, ScrapeData> {
        if(!dbManager.torrentExists(infohash))
            throw java.lang.IllegalArgumentException()
        val res = HashMap<String, ScrapeData>()
        val announceList = announces(infohash)
        for(l in announceList){
            for(tracker in l){
                val filesBytes = dbManager.getFiles(tracker)
                if(filesBytes != null){
                    val files = parser.parse(filesBytes)
                    val data: TorrentDict = files[coder.string_to_hex(infohash)]?.value() as TorrentDict
                    val scrape = Scrape(complete = (data["complete"]?.value() as Long).toInt(), downloaded = (data["downloaded"]?.value() as Long).toInt(),
                            incomplete = (data["incomplete"]?.value() as Long).toInt(), name = data["name"]?.value()?.toString())
                    res[tracker] = scrape
                }
            }
        }
        return res
    }
}