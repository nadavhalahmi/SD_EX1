import ITorrentHTTP
import java.net.URL

class TorrentHTTPDummyImpl: ITorrentHTTP {
    override fun get(tracker: String, params: HashMap<String, String>): ByteArray {
        return ByteArray(5)
    }
}