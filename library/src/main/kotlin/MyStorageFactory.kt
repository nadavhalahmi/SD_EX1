import il.ac.technion.cs.softwaredesign.storage.SecureStorage
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory

class MyStorageFactory: SecureStorageFactory {
    private val dbs = HashMap<MyByteArray, SecureStorage>()
    override fun open(name: ByteArray): SecureStorage {
        if(dbs.containsKey(MyByteArray(name)))
            return dbs[MyByteArray(name)]!!
        dbs[MyByteArray(name)] = MyStorage()
        return dbs[MyByteArray(name)]!!
    }
}