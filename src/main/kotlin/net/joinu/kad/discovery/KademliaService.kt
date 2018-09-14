package net.joinu.kad.discovery

import net.joinu.osen.Address
import net.joinu.osen.Message
import net.joinu.osen.P2P
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeoutException


@Service
class KademliaService {
    @Autowired
    private lateinit var addressBook: AddressBook

    @Autowired
    private lateinit var p2p: P2P

    private val asked = hashMapOf<KadId, MutableSet<KAddress>>()

    suspend fun ping(peer: Address): Boolean {
        val message = Message(KAD_TOPIC, KadMsgTypes.PING, addressBook.getMine().id)

        return try {
            val peerId = p2p.sendAndReceive<KadId>(peer, message)
            addressBook.addRecord(KAddress(peerId, peer))
            true
        } catch (e: TimeoutException) {
            false
        }
    }

    suspend fun bootstrap(bootstrapPeer: Address, limit: Int): Boolean {
        if (!ping(bootstrapPeer)) return false

        findNode(addressBook.getMine().id, limit)

        return true
    }

    tailrec suspend fun findNode(findId: KadId, limit: Int): KAddress? {
        // if already have him in address book - return
        val peerFromAddressBook = addressBook.getRecordById(findId)
        if (peerFromAddressBook != null) return peerFromAddressBook

        // otherwise pick closest k-bucket
        val peers = addressBook.getClosest(findId, limit)

        // initialize list of asked peers if it is uninitialized
        if (!asked.containsKey(findId)) asked[findId] = mutableSetOf()

        // if all peers are already asked - return null
        if (peers.map { asked[findId]!!.contains(it) }.all { it }) {
            asked.remove(findId)
            return null
        }

        // filtering peers we already asked
        val peersToAsk = peers.subtract(asked[findId]!!)

        // getting responses from peers
        val responses = peersToAsk.map { sendFindNode(it.address, findId, limit) }

        // update asked list
        asked[findId]!!.addAll(peersToAsk)

        // add all new contacts to address book
        responses
            .filter { it.peersToAsk != null }
            .flatMap { it.peersToAsk!! }
            .forEach { addressBook.addRecord(it) }

        // if target peer found - add it to address book and return, otherwise - repeat with an updated address book
        val peer = responses.firstOrNull { it.peerExact != null }?.peerExact
        return if (peer != null) {
            addressBook.addRecord(peer)
            asked.remove(findId)

            return peer
        } else {
            findNode(findId, limit)
        }
    }

    private suspend fun sendFindNode(peer: Address, findId: KadId, limit: Int): FindNodeResponse {
        val payload = FindNodeRequest(addressBook.getMine().id, findId, limit)
        val message = Message(KAD_TOPIC, KadMsgTypes.FIND_NODE, payload)

        return p2p.sendAndReceive(peer, message)
    }
}