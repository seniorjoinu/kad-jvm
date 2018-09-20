package net.joinu.kad.discovery.addressbook

import net.joinu.kad.discovery.KAddress
import net.joinu.kad.discovery.KadId


/**
 * Interface for every kademlia address book implementation.
 * Used to track peers you communicate.
 * Address book should be created when peer knows it's own address.
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
     * Returns the cluster of target id
     *
     * @param of            target id
     * @return              list of K..K*4 closest to [of] addresses
     */
    fun getCluster(of: KadId): List<KAddress>

    /**
     * Clears address book
     */
    fun clear()
}

fun AddressBook.getMyCluster() = getCluster(getMine().id)
fun AddressBook.getMyClusterExceptMe() = getMyCluster().filter { it != getMine() }