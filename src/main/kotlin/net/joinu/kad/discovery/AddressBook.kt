package net.joinu.kad.discovery


/**
 * Interface for every kademlia address book implementation.
 * Used to track peers you communicate.
 * Address book should be created when peer knows it's own address.
 *
 * TODO: add clustering
 */
interface AddressBook {
    /**
     * Adds new address to address book
     *
     * @param address       address to add
     */
    fun addRecord(address: KAddress)

    /**
     * Removes address from address book
     *
     * @param id            id of target address
     */
    fun removeRecord(id: KadId)

    /**
     * Returns the whole address book
     *
     * @return              List of addresses
     */
    fun getRecords(): List<KAddress>

    /**
     * Returns address by it's id
     *
     * @param id            target address's id
     * @return              address if presents, null otherwise
     */
    fun getRecordById(id: KadId): KAddress?

    /**
     * Returns address of myself
     *
     * @return              address caller is listening for messages
     */
    fun getMine(): KAddress

    /**
     * Returns XOR-closest to target id addresses
     *
     * @param to            target id
     * @param limit         max number of addresses (it's like k-bucket but k is configurable)
     * @return              list of [limit] closest to [to] addresses
     */
    fun getClosest(to: KadId, limit: Int): List<KAddress>
}

class InMemoryAddressBook(
    private val myAddress: KAddress,
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

    override fun getClosest(to: KadId, limit: Int) = addresses.values.sortedBy { it.id.xor(to) }.take(limit)
}
