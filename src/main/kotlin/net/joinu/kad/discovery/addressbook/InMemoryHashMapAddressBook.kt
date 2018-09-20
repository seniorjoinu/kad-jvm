package net.joinu.kad.discovery.addressbook

import net.joinu.kad.discovery.KAddress
import net.joinu.kad.discovery.KadId


@Deprecated("Provides inconsistent cluster state", replaceWith = ReplaceWith("InMemoryBinaryTreeAddressBook"))
class InMemoryHashMapAddressBook(
    private val myAddress: KAddress,
    private val k: Int = 20,
    private val addresses: HashMap<KadId, KAddress> = hashMapOf()
) : AddressBook {

    override fun getMine() = myAddress

    override fun addRecord(address: KAddress) {
        addresses[address.id] = address
    }

    override fun removeRecord(id: KadId) {
        addresses.remove(id)
    }

    override fun getRecords(): List<KAddress> = addresses.values.toList()

    override fun getRecordById(id: KadId): KAddress? = addresses[id]

    override fun getCluster(of: KadId): List<KAddress> = addresses.values.sortedBy { it.id.xor(of) }.take(k)

    override fun clear() {
        addresses.clear()
        addresses[myAddress.id] = myAddress
    }
}