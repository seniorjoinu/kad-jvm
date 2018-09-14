package net.joinu.kad.discovery

import net.joinu.osen.Address
import net.joinu.osen.On
import net.joinu.osen.P2PController
import org.springframework.beans.factory.annotation.Autowired


const val KAD_TOPIC = "_KAD"
object KadMsgTypes {
    const val PING = "0"
    const val FIND_NODE = "1"
}

/**
 * Payload for a FIND_NODE message
 *
 * @param senderId              id of the sender
 * @param findId                id of the peer sender wish to find
 * @param k                     K-parameter (how much peers max do we need in response)
 */
data class FindNodeRequest(val senderId: KadId, val findId: KadId, val k: Int)

/**
 * Payload for a FIND_NODE response. We return a peer address if we know it, otherwise we return a list of closest
 * to target peers.
 *
 * XOR SCALE (direction ->):   |---peers sender already asked-------we-------[peersToAsk]-------[peerExact]---|
 *
 * @param peerExact
 * @param peersToAsk
 */
data class FindNodeResponse(val peerExact: KAddress? = null, val peersToAsk: List<KAddress>? = null)


@P2PController(KAD_TOPIC)
class KadP2PController {
    @Autowired
    private lateinit var addressBook: AddressBook

    @On(KadMsgTypes.PING)
    fun onPing(senderId: KadId, sender: Address): KadId {
        addressBook.addRecord(KAddress(senderId, sender))
        return addressBook.getMine().id
    }

    @On(KadMsgTypes.FIND_NODE)
    fun onFindNode(payload: FindNodeRequest, sender: Address): FindNodeResponse {
        val peerExact = if (addressBook.getMine().id == payload.findId)
            addressBook.getMine()
        else
            addressBook.getRecordById(payload.findId)

        val peersToAsk = addressBook.getClosest(payload.findId, payload.k)

        val result = if (peerExact != null)
            FindNodeResponse(peerExact, null)
        else
            FindNodeResponse(null, peersToAsk)

        addressBook.addRecord(KAddress(payload.senderId, sender))

        return result
    }
}