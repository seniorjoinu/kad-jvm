package net.joinu.kad.discovery

import net.joinu.osen.Address
import java.math.BigInteger


typealias KadId = BigInteger

data class KAddress(val id: KadId, val address: Address)

data class TrieNode(
    val bitIndex: Int = 0,
    val id: String = "",
    val parent: TrieNode? = null,
    var zero: TrieNode? = null,
    var one: TrieNode? = null,
    val kBucket: MutableList<BigInteger> = mutableListOf()
) {
    override fun equals(other: Any?): Boolean {
        other as TrieNode
        return id == other.id
    }

    override fun toString(): String {
        return kBucket.toString()
    }
}

data class BinaryTrie(private val root: TrieNode, private val k: Int) {
    private val leaves: MutableList<TrieNode> = mutableListOf(root)

    fun clear() {
        leaves.clear()
        root.kBucket.clear()
        root.one = null
        root.zero = null
    }

    override fun toString(): String {
        val elems = getNeighborhoodsByName()
            .map {
                "\tprefix = ${it.key}, size = ${it.value.size}, elements = ${it.value.map { it.toString(2) }}"
            }
            .joinToString("\n")

        return "BinaryTrie {\n$elems\n}"
    }

    fun addData(data: BigInteger): Boolean {
        val currentNode = findAppropriateTrieNode(data)
        if (currentNode.kBucket.contains(data)) return false

        if (currentNode.kBucket.size + 1 <= 2 * k) {
            currentNode.kBucket.add(data)

            return true
        }

        val newBitIndex = currentNode.bitIndex + 1
        val zero = TrieNode(newBitIndex, currentNode.id + "0", parent = currentNode)
        val one = TrieNode(newBitIndex, currentNode.id + "1", parent = currentNode)

        currentNode.kBucket.forEach { d ->
            val bit = d.testBit(256 - (currentNode.bitIndex + 1))

            if (bit) one.kBucket.add(d)
            else zero.kBucket.add(d)
        }

        val bit = data.testBit(256 - (currentNode.bitIndex + 1))
        if (bit) one.kBucket.add(data)
        else zero.kBucket.add(data)

        if (zero.kBucket.size >= k && one.kBucket.size >= k) {
            currentNode.zero = zero
            currentNode.one = one
            currentNode.kBucket.clear()

            leaves.remove(currentNode)
            leaves.add(zero)
            leaves.add(one)
        } else {
            currentNode.kBucket.add(data)
        }

        return true
    }

    fun removeData(data: BigInteger): Boolean {
        val currentNode = findAppropriateTrieNode(data)
        val record = currentNode.kBucket.remove(data)
        if (!record) return false
        if (currentNode.kBucket.size >= k) return true
        if (currentNode == root) return true

        val one = currentNode.parent!!.one!!
        val zero = currentNode.parent.zero!!
        currentNode.parent.one = null
        currentNode.parent.zero = null

        currentNode.parent.kBucket.addAll(one.kBucket + zero.kBucket)

        leaves.remove(one)
        leaves.remove(zero)
        leaves.add(currentNode.parent)

        return true
    }

    fun flatten() = leaves.map { it.kBucket }.flatten()

    fun getNeighborhoods() = leaves.map { it.kBucket }.toList()

    fun getNeighborhoodsByName() = leaves.associate { it.id to it.kBucket }

    fun getNeighbors(of: BigInteger): List<BigInteger> {
        val node = findAppropriateTrieNode(of)
        if (node == root) return node.kBucket.toList()

        return node.parent!!.zero!!.kBucket + node.parent.one!!.kBucket
    }

    fun getLeaf(of: BigInteger) = findAppropriateTrieNode(of).kBucket

    private fun findAppropriateTrieNode(data: BigInteger): TrieNode {
        var currentNode = root

        while (true) {
            val bit = data.testBit(256 - (currentNode.bitIndex + 1))

            if (bit && currentNode.one != null) {
                currentNode = currentNode.one!!
                continue
            }

            if (!bit && currentNode.zero != null) {
                currentNode = currentNode.zero!!
                continue
            }

            break
        }

        return currentNode
    }
}
