Changes:

CourseTorrentModule.kt:

line 12: (forgot to do this as we did in TestModule)

ADD: 

	bind<ITorrentHTTP>().to<TorrentHTTP>()


TorrentHTTP.kt

lines 4-12: (there's no reason it was noted. We mocked it anyway. leftover from debugging)
		
UNNOTE 

line 13:
NOTE


CourseTorrent.kt:


lines 209-213: (handled errors correctly and apperantly misunderstood the protocol, so now we access
files[infohash] instead of hex encoding of infohash)
		
REPLACE 

	var resp = torrentHTTP.get(scrape, params)
	val respDict = parser.parse(resp)
	val files = respDict["files"]?.value() as TorrentDict
	val stats = files[coder.string_to_hex(infohash)]?.value() as TorrentDict?
	databases.updateTracker(infohash ,tracker, stats)
			
BY 

	try {
		var resp = torrentHTTP.get(scrape, params)
		val respDict = parser.parse(resp)
		if (respDict["failure reason"] !== null) {
			databases.updateTracker(infohash, tracker, respDict)
		} else {
			val files = respDict["files"]?.value() as TorrentDict
			//val stats = files[coder.string_to_hex(infohash)]?.value() as TorrentDict?
			val stats = files[infohash]?.value() as TorrentDict?
			databases.updateTracker(infohash, tracker, stats)
		}
	}catch (e: Exception){
		databases.updateTrackerConnctionLost(infohash, tracker)
	}

line 251:
REPLACE

	val peersBytes = databases.getPeers(infohash)
	
BY
	
	var peersBytes = databases.getPeers(infohash)

lines 264-270: (apperantly we misunderstood the protocol, so when there's an announce, the new peers are added to the old peers,
and not replace them).

REPLACE

	val start = (parser.parseBytes(peersBytes) { peersBytes[it].toChar() == ':' }).length + 1
	val end = peersBytes.size
	for (i in start until end step 6) {
		val (ip, port) = coder.get_ip_port(peersBytes.copyOfRange(i, i + 6))
		val peer = KnownPeer(ip, port, null)
		if(databases.peerIsValid(infohash, peer))
			res.add(peer)
			
BY

	while(peersBytes!!.isNotEmpty()) {
		val bytesLength = (parser.parseBytes(peersBytes) { peersBytes!![it].toChar() == ':' }).toLong()
		val start = bytesLength.toString().length + 1
		val end = start + bytesLength.toInt()
		for (i in start until end step 6) {
			val (ip, port) = coder.get_ip_port(peersBytes.copyOfRange(i, i + 6))
			val peer = KnownPeer(ip, port, null)
			if (databases.peerIsValid(infohash, peer))
				res.add(peer)
		}
		peersBytes = peersBytes.copyOfRange(end, peersBytes.size)
	}
	
Databases.kt:


line 79:		
REPLACE 

	storageManager.setValue(torrentsDB, hash, "peers", peersBytes)
	
BY

	val oldPeers = storageManager.getValue(torrentsDB, hash, "peers")
	if(oldPeers === null)
		storageManager.setValue(torrentsDB, hash, "peers", peersBytes)
	else {
		storageManager.setValue(torrentsDB, hash, "peers", oldPeers + peersBytes)
	}
	
line 95:
REPLACE

	storageManager.setValue(trackersDB, hash, "$tracker-$key", (stats[key]?.value() as Long).toString().toByteArray())
	
BY

	storageManager.setValue(trackersDB, hash, "$tracker-$key", (stats[key]?.value()).toString().toByteArray())

ADDED FUNCTIONS:

	fun updateTrackerConnctionLost(hash: String, tracker: String) {
        val key = "failure reason"
        storageManager.setExists(trackersDB, "$hash-$tracker")
        storageManager.setValue(trackersDB, hash, "$tracker-$key", "Connection failed".toByteArray())
    }
	
	private fun trackerFailed(hash: String, tracker: String): ByteArray? {
        return storageManager.getValue(trackersDB, "$hash-$tracker", "failure reason")
    }
	
line 114:
REPLACE

	fun getTrackerStats(hash: String, tracker: String): Scrape? {
	
BY

	fun getTrackerStats(hash: String, tracker: String): ScrapeData? {

lines 117-121:
REPLACE

	val complete = storageManager.getValue(trackersDB, "$hash-$tracker", "complete")?.toString(charset)!!.toInt()
	val downloaded = storageManager.getValue(trackersDB, "$hash-$tracker", "downloaded")?.toString(charset)!!.toInt()
	val incomplete = storageManager.getValue(trackersDB, "$hash-$tracker", "incomplete")?.toString(charset)!!.toInt()
	val name = storageManager.getValue(trackersDB, "$hash-$tracker", "name")?.toString(charset)
	return Scrape(complete, downloaded, incomplete, name)
	
BY

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
	














