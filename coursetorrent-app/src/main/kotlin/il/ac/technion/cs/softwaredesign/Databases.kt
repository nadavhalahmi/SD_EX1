package il.ac.technion.cs.softwaredesign

import StorageManager
import TorrentDict
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
class Databases @Inject constructor(private val db_factory: SecureStorageFactory) {
    private val torrentsDB = db_factory.open("my_torrents".toByteArray(charset))
    private val peersDB = db_factory.open("peers".toByteArray(charset))
    private val trackersDB = db_factory.open("trackers".toByteArray(charset))
    private val storageManager = StorageManager()

    /**
     * saves torrent to database as mentioned above
     */
    fun addTorrent(hash: String, value: ByteArray, dict: TorrentDict){
        val db = torrentsDB
        storageManager.setExists(db, hash)
        for(key in dict.keys) {
            if(key == "announce" || key == "announce-list") {
                val range = dict.getRange(key)
                storageManager.setValue(db, hash, key, value.copyOfRange(dict[key]!!.startIndex(), dict[key]!!.endIndex()))
                db.write((hash + key).toByteArray(), value.copyOfRange(range.startIndex(), range.endIndex()))
            }
        }
    }

    fun torrentExists(hash: String): Boolean {
        return storageManager.exists(torrentsDB, hash)
    }

    /**
     * gets value from database
     */
    fun getTorrentField(hash: String, key: String): ByteArray? {
        if(!storageManager.exists(torrentsDB, hash)) return null
        return storageManager.getValue(torrentsDB, hash, key)
    }

    /**
     * delete value from databaase:
     * (writes empty ByteArray to that key)
     */
    fun deleteTorrent(hash: String): Unit {
        storageManager.removeNotExist(torrentsDB, hash)
    }

    fun getPeers(hash: String): ByteArray? {
        return storageManager.getValue(torrentsDB, hash, "peers")
    }

    fun updateAnnounce(hash: String, announceList: List<List<String>>) {
        var newAnnounce = "l"
        for(l in announceList){
            newAnnounce += "l"
            for(url in l){
                newAnnounce += url.toByteArray(charset).size.toString() + ":" +url
            }
            newAnnounce += "e"
        }
        newAnnounce += "e"
        storageManager.setValue(torrentsDB, hash, "announce", newAnnounce.toByteArray(charset))
        storageManager.setValue(torrentsDB, hash, "announce-list", newAnnounce.toByteArray(charset))
    }

    fun updatePeersList(hash: String, peersBytes: ByteArray, peers: HashSet<KnownPeer>) {
        val oldPeers = storageManager.getValue(torrentsDB, hash, "peers")
        if(oldPeers === null)
            storageManager.setValue(torrentsDB, hash, "peers", peersBytes)
        else {
            storageManager.setValue(torrentsDB, hash, "peers", oldPeers + peersBytes)
        }
        for(peer in peers) {
            storageManager.setValid(peersDB, "$hash-${peer.ip}-${peer.port}")
        }
    }

    fun updateTracker(hash: String,tracker: String, stats: TorrentDict?) {
        if(stats != null){
            if(!trackerExists(hash,tracker)){
                storageManager.setExists(trackersDB, "$hash-$tracker")
                storageManager.setValue(trackersDB, hash, "$tracker-complete", "0".toByteArray())
                storageManager.setValue(trackersDB, hash, "$tracker-downloaded", "0".toByteArray())
                storageManager.setValue(trackersDB, hash, "$tracker-incomplete", "0".toByteArray())
                //name is null by default
            }
            for(key in stats.keys) {
                storageManager.setValue(trackersDB, hash, "$tracker-$key", (stats[key]?.value()).toString().toByteArray())
            }
        }
    }

    fun updateTrackerConnctionLost(hash: String, tracker: String) {
        val key = "failure reason"
        storageManager.setExists(trackersDB, "$hash-$tracker")
        storageManager.setValue(trackersDB, hash, "$tracker-$key", "Connection failed".toByteArray())
    }

    fun trackerExists(hash: String, tracker: String): Boolean {
        return storageManager.exists(trackersDB, "$hash-$tracker")
    }

    private fun trackerFailed(hash: String, tracker: String): ByteArray? {
        return storageManager.getValue(trackersDB, "$hash-$tracker", "failure reason")
    }

    fun invalidatePeer(hash: String, peer: KnownPeer) {
        if(peerIsValid(hash, peer)) {
            storageManager.setInValid(peersDB, "$hash-${peer.ip}-${peer.port}")
        }
    }

    fun peerIsValid(hash: String, peer: KnownPeer): Boolean {
        return storageManager.isValid(peersDB, "$hash-${peer.ip}-${peer.port}")
    }

    fun getTrackerStats(hash: String, tracker: String): ScrapeData? {
        //TODO: deal with tacker failed
        if(trackerExists(hash, tracker)) {
            val failMessage = trackerFailed(hash, tracker)
            return if(failMessage === null) {
                val complete = storageManager.getValue(trackersDB, "$hash-$tracker", "complete")?.toString(charset)!!.toInt()
                val downloaded = storageManager.getValue(trackersDB, "$hash-$tracker", "downloaded")?.toString(charset)!!.toInt()
                val incomplete = storageManager.getValue(trackersDB, "$hash-$tracker", "incomplete")?.toString(charset)!!.toInt()
                val name = storageManager.getValue(trackersDB, "$hash-$tracker", "name")?.toString(charset)
                Scrape(complete, downloaded, incomplete, name)
            } else{
                Failure(failMessage.toString(charset))
            }
        }
        return null
    }


}
