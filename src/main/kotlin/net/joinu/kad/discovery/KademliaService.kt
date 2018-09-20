package net.joinu.kad.discovery

import net.joinu.kad.discovery.addressbook.AddressBook
import net.joinu.kad.discovery.addressbook.getMyClusterExceptMe
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

    suspend fun bootstrap(bootstrapPeer: Address): Boolean {
        if (!ping(bootstrapPeer)) return false

        findNode(addressBook.getMine().id)

        val myCluster = addressBook.getCluster(addressBook.getMine().id)
        myCluster.forEach { greet(it.address) }

        return true
    }

    suspend fun byeAll() {
        addressBook.getMyClusterExceptMe().forEach { bye(it.address) }
    }

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

    suspend fun greet(peer: Address) {
        val message = Message(KAD_TOPIC, KadMsgTypes.GREET, addressBook.getMine().id)
        p2p.send(peer, message)
    }

    suspend fun bye(peer: Address) {
        val message = Message(KAD_TOPIC, KadMsgTypes.BYE, addressBook.getMine().id)
        p2p.send(peer, message)

        val peerAddr = addressBook.getRecords().find { it.address == peer } ?: return
        addressBook.removeRecord(peerAddr.id)
    }

    suspend fun findNode(findId: KadId): KAddress? {
        // if already have him in address book - return
        val peerFromAddressBook = addressBook.getRecordById(findId)
        if (peerFromAddressBook != null) return peerFromAddressBook

        // initialize list of asked peers if it is uninitialized
        if (!asked.containsKey(findId)) asked[findId] = mutableSetOf()

        // first of all search the closest known peers (from cluster)
        while (true) {
            val peers = addressBook.getCluster(findId)
            val closestPeer = peers
                .sortedBy { it.id.xor(findId) }
                .firstOrNull { !asked[findId]!!.contains(it) } ?: break

            val result = sendFindNodeAndProcessResult(closestPeer, findId)
            if (result != null) return result
        }

        // if that didn't work - search other peers which you know
        while (true) {
            val peers = addressBook.getRecords()
            val closestPeer = peers
                .sortedBy { it.id.xor(findId) }
                .firstOrNull { !asked[findId]!!.contains(it) }

            // if we can't find a single node who knows about [findId] - return nothing
            if (closestPeer == null) {
                asked.remove(findId)
                return null
            }

            val result = sendFindNodeAndProcessResult(closestPeer, findId)
            if (result != null) return result
        }
    }

    private suspend fun sendFindNodeAndProcessResult(peer: KAddress, findId: KadId): KAddress? {
        val response = sendFindNode(peer.address, findId)

        if (response.peerExact != null) {
            addressBook.addRecord(response.peerExact)
            asked.remove(findId)

            return response.peerExact
        }

        asked[findId]!!.add(peer)

        response.peersToAsk!!.forEach { addressBook.addRecord(it) }

        return null
    }

    private suspend fun sendFindNode(peer: Address, findId: KadId): FindNodeResponse {
        val payload = FindNodeRequest(addressBook.getMine().id, findId)
        val message = Message(KAD_TOPIC, KadMsgTypes.FIND_NODE, payload)

        return p2p.sendAndReceive(peer, message)
    }
}