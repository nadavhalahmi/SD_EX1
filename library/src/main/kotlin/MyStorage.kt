import il.ac.technion.cs.softwaredesign.storage.SecureStorage

class MyStorage: SecureStorage{
    private val db = HashMap<MyByteArray, MyByteArray>()
    private val charset = Charsets.UTF_8
    override fun read(key: ByteArray): ByteArray? {
        //println("reading key "+key.toString(charset))
        return if(db.containsKey(MyByteArray(key)))
            db[MyByteArray(key)]?.arr
        else null
    }
    override fun write(key: ByteArray, value: ByteArray) {
        //println("writing key "+key.toString(charset))
        db[MyByteArray(key)] = MyByteArray(value)
    }
}

