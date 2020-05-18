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
    private val announceDB = db_factory.open("announce".toByteArray(charset))
    private val scrapeDB = db_factory.open("scrape".toByteArray(charset))

    /**
     * saves torrent to database as mentioned above
     */
    fun addTorrent(hash: String, value: ByteArray, dict: TorrentDict){
        val hashBytes = hash.toByteArray(charset)
        val db = torrentsDB
        //db.write(hashBytes, value)
        db.write(hashBytes+"exists".toByteArray(charset), "true".toByteArray(charset))
        for(key in dict.keys) {
            val range = dict.getRange(key)
            db.write((hash + key).toByteArray(), value.copyOfRange(range.startIndex(), range.endIndex()))
        }
    }

    fun addAnnounce(tracker: String, value: ByteArray, dict: TorrentDict){
        val hashBytes = tracker.toByteArray(charset)
        val db = announceDB
        //db.write(hashBytes, value)
        db.write(hashBytes+"exists".toByteArray(charset), "true".toByteArray(charset))
        for(key in dict.keys) {
            val range = dict.getRange(key)
            db.write((tracker + key).toByteArray(), value.copyOfRange(range.startIndex(), range.endIndex()))
        }
    }

    fun addScrape(hash: String, value: ByteArray, dict: TorrentDict){
        val hashBytes = hash.toByteArray(charset)
        val db = scrapeDB
        //db.write(hashBytes, value)
        db.write(hashBytes+"exists".toByteArray(charset), "true".toByteArray(charset))
        for(key in dict.keys) {
            val range = dict.getRange(key)
            db.write((hash + key).toByteArray(), value.copyOfRange(range.startIndex(), range.endIndex()))
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

    fun getAnnonce(tracker: String, key: String = ""): ByteArray? {
        return getAnnounce((tracker).toByteArray(charset),key)
    }

    fun getAnnounce(tracker: ByteArray, key: String = ""): ByteArray? {
        if(!torrentExists(tracker)) return null
        val db = announceDB
        return db.read(tracker+key.toByteArray(charset))
    }

    fun getScrape(hash: String, key: String = ""): ByteArray? {
        return getScrape((hash).toByteArray(charset),key)
    }

    fun getScrape(hash: ByteArray, key: String = ""): ByteArray? {
        if(!torrentExists(hash)) return null
        val db = scrapeDB
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

    fun deleteAnnounce(key: String): Unit {
        deleteAnnounce(key.toByteArray(charset))
    }

    fun deleteAnnounce(key: ByteArray): Unit {
        val db = announceDB
        db.write(key+"exists".toByteArray(charset), ByteArray(0))
    }

    fun getPeers(tracker: String): ByteArray? {
        return getAnnonce(tracker, "peers")
    }

    fun getFiles(tracker: String): ByteArray? {
        return getScrape(tracker, "files")
    }
}
