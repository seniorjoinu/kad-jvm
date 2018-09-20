package net.joinu.kad.discovery.addressbook

import net.joinu.kad.discovery.BinaryTrie
import net.joinu.kad.discovery.KAddress
import net.joinu.kad.discovery.KadId
import net.joinu.kad.discovery.TrieNode


class InMemoryBinaryTrieAddressBook(
    private val myAddress: KAddress,
    private val k: Int = 20,
    private val addresses: BinaryTrie = BinaryTrie(TrieNode(kBucket = mutableListOf(myAddress.id)), k)
) : AddressBook {

    private val addressesByIds = hashMapOf<KadId, KAddress>()

    init {
        addressesByIds[myAddress.id] = myAddress
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

    override fun getCluster(of: KadId) = addresses.getNeighbors(of).map { addressesByIds[it]!! }
}