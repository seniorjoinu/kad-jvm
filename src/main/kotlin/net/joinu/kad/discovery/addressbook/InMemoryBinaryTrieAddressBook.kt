package net.joinu.kad.discovery.addressbook

import net.joinu.kad.discovery.*
import java.math.BigInteger


class InMemoryBinaryTrieAddressBook(
    private val myAddress: KAddress,
    private val k: Int = 20,
    private val addresses: BinaryTrie = BinaryTrie(TrieNode(), k)
) : AddressBook {

    private val addressesByIds = hashMapOf<KadId, KAddress>()

    init {
        addressesByIds[myAddress.id] = myAddress
        addresses.addData(myAddress.id)
    }

    override fun addRecord(address: KAddress) {
        if (addresses.addData(address.id))
            addressesByIds[address.id] = address
    }

    override fun removeRecord(id: KadId) {
        if (addresses.removeData(id))
            addressesByIds.remove(id)
    }

    override fun getRecords() = addresses.flatten().map { addressesByIds[it]!! }

    override fun getRecordById(id: KadId) = addressesByIds[id]

    override fun getMine() = myAddress

    override fun getCluster(of: KadId): Cluster {
        val peerIds = addresses.getNeighbors(of).sorted()
        val name = peerIds
            .fold(BigInteger.ZERO) { acc, id -> acc + id } / BigInteger.valueOf(peerIds.size.toLong())

        return Cluster(name, peerIds.map { addressesByIds[it]!! })
    }

    override fun clear() {
        addressesByIds.clear()
        addresses.clear()

        addressesByIds[myAddress.id] = myAddress
        addresses.addData(myAddress.id)
    }
}