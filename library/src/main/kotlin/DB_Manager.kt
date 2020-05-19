import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.nio.charset.Charset
import com.google.inject.Inject


private val charset: Charset = Charsets.UTF_8
/**
 * Wrapper class for read/write calls
 * each dict is saved as above:
 *      -hash -> value
 *      -hash^key1 -> dict[key1] as ByteArray
 *      -hash^key2 -> dict[key2] as ByteArray
 *      -...
 */
class DB_Manager @Inject constructor(private val db_factory: SecureStorageFactory) {

    private val torrentsDB = db_factory.open("my_torrents".toByteArray(charset))
    private val peersDB = db_factory.open("peers".toByteArray(charset))
    private val trackersDB = db_factory.open("trackers".toByteArray(charset))

    /**
     * saves torrent to database as mentioned above
     */
    fun addTorrent(hash: String, value: ByteArray, dict: TorrentDict){
        val hashBytes = hash.toByteArray(charset)
        val db = torrentsDB
        //db.write(hashBytes, value)
        db.write(hashBytes+"exists".toByteArray(charset), "true".toByteArray(charset))
        for(key in dict.keys) {
            if(key == "announce" || key == "announce-list") {
                val range = dict.getRange(key)
                db.write((hash + key).toByteArray(), value.copyOfRange(range.startIndex(), range.endIndex()))
            }
        }
    }

    fun torrentExists(hash: String): Boolean {
        return torrentExists((hash).toByteArray(charset))
    }

    fun torrentExists(hash: ByteArray): Boolean {
        val db = torrentsDB
        return db.read(hash + "exists".toByteArray(charset))?.isNotEmpty() ?: false
    }

    /**
     * gets value from database
     */
    fun getTorrent(hash: String, key: String = ""): ByteArray? {
        return getTorrent((hash).toByteArray(charset),key)
    }

    fun getTorrent(hash: ByteArray, key: String = ""): ByteArray? {
        if(!torrentExists(hash)) return null
        val db = torrentsDB
        return db.read(hash+key.toByteArray(charset))
    }

    /**
     * delete value from databaase:
     * (writes empty ByteArray to that key)
     */
    fun deleteTorrent(key: String): Unit {
        deleteTorrent(key.toByteArray(charset))
    }

    fun deleteTorrent(key: ByteArray): Unit {
        val db = torrentsDB
        db.write(key+"exists".toByteArray(charset), ByteArray(0))
    }

    fun getPeers(hash: String): ByteArray? {
        val db = torrentsDB
        return db.read(("$hash-peers").toByteArray(charset))
    }

    fun updateAnnounce(announceList: List<List<String>>) {
        //TODO: IMPLEMET
    }

    //TODO: CHANGE to KnownPeer
    fun updatePeersList(hash: String, peersBytes: ByteArray, peers: ArrayList<KnownPeer>) {
        var db = torrentsDB
        db.write(("$hash-peers").toByteArray(charset), peersBytes)
        db = peersDB
        for(peer in peers) {
            db.write(("$hash-${peer.ip}-${peer.port}-valid").toByteArray(charset),
                "true".toByteArray(charset))
        }
    }

    fun updateTracker(hash: String,tracker: String, stats: TorrentDict?) {
        val db = trackersDB
        if(stats != null){
            if(!trackerExists(hash,tracker)){
                db.write("$hash-$tracker-exists".toByteArray(), "true".toByteArray())
                db.write("$hash-$tracker-complete".toByteArray(), "0".toByteArray())
                db.write("$hash-$tracker-downloaded".toByteArray(), "0".toByteArray())
                db.write("$hash-$tracker-incomplete".toByteArray(), "0".toByteArray())
                //db.write("$hash-$tracker-name".toByteArray(), ByteArray(0))
                //name is null by default
            }
            for(key in stats.keys) {
                db.write("$hash-$tracker-$key".toByteArray(), (stats["downloaded"]?.value() as Long).toString().toByteArray())
            }
        }
    }

    fun trackerExists(hash: String, tracker: String): Boolean {
        val db = trackersDB
        return db.read(("$hash-$tracker-exists").toByteArray(charset))?.isNotEmpty() ?: false
    }

    fun invalidatePeer(hash: String, peer: KnownPeer) {
        if(peerIsValid(hash, peer)) {
            val db = peersDB
            return db.write(("$hash-${peer.ip}-${peer.port}-valid").toByteArray(charset), ByteArray(0))
        }
    }

    fun peerIsValid(hash: String, peer: KnownPeer): Boolean {
        val db = peersDB
        return db.read(("$hash-${peer.ip}-${peer.port}-valid").toByteArray(charset))?.isNotEmpty() ?: false
    }

    fun getTrackerStats(hash: String, tracker: String): Scrape? {
        //TODO: deal with tacker failed
        val db = trackersDB
        if(trackerExists(hash, tracker)) {
            val complete = db.read("$hash-$tracker-complete".toByteArray(charset))?.toString(charset)!!.toInt()
            val downloaded = db.read("$hash-$tracker-downloaded".toByteArray(charset))?.toString(charset)!!.toInt()
            val incomplete = db.read("$hash-$tracker-incomplete".toByteArray(charset))?.toString(charset)!!.toInt()
            val name = db.read("$hash-$tracker-name".toByteArray(charset))?.toString(charset)
            return Scrape(complete, downloaded, incomplete, name)
        }
        return null
    }
}
