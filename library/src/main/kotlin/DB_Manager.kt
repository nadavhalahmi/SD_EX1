import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.nio.charset.Charset
import com.google.inject.Guice
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

//    fun open_db(db_name: ByteArray){
//        db = db_factory.open(db_name)
//    }
    /**
     * saves torrent to database as mentioned above
     */
    fun add(hash: String, value: ByteArray, dict: TorrentDict, db_name: String = "abc"){
        val hashBytes = hash.toByteArray(charset)
        val db = db_factory.open(db_name.toByteArray(charset))
        //db.write(hashBytes, value)
        db.write(hashBytes+"exists".toByteArray(charset), "true".toByteArray(charset))
        for(key in dict.keys) {
            val range = dict.getRange(key)
            db.write((hash + key).toByteArray(), value.copyOfRange(range.startIndex(), range.endIndex()))
        }
    }

    fun exists(hash: String, key: String = ""): Boolean {
        return exists((hash).toByteArray(charset),key)
    }

    fun exists(hash: ByteArray, key: String = "", db_name: String = "abc"): Boolean {
        val db = db_factory.open(db_name.toByteArray(charset))
        return db.read(hash + "exists".toByteArray(charset))?.isNotEmpty() ?: false
    }

    /**
     * gets value from database
     */
    fun get(hash: String, key: String = ""): ByteArray? {
        return get((hash).toByteArray(charset),key)
    }

    fun get(hash: ByteArray, key: String = "", db_name: String = "abc"): ByteArray? {
        if(!exists(hash)) return null
        val db = db_factory.open(db_name.toByteArray(charset))
        return db.read(hash+key.toByteArray(charset))
    }

    /**
     * delete value from databaase:
     * (writes empty ByteArray to that key)
     */
    fun delete(key: String): Unit {
        delete(key.toByteArray(charset))
    }

    fun delete(key: ByteArray, db_name: String = "abc"): Unit {
        val db = db_factory.open(db_name.toByteArray(charset))
        db.write(key+"exists".toByteArray(charset), ByteArray(0))
    }
}
