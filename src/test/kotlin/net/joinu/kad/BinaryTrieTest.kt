package net.joinu.kad

import net.joinu.kad.discovery.BinaryTrie
import net.joinu.kad.discovery.TrieNode
import net.joinu.utils.CryptoUtils
import net.joinu.utils.SerializationUtils
import org.junit.Test
import java.math.BigInteger


class BinaryTrieTest {
    @Test
    fun `multiple add and remove operations work okay`() {
        val trie = BinaryTrie(TrieNode(), 20, 256, 20)

        val dataset = (1..1000)
            .map { SerializationUtils.anyToBytes(it) }
            .map { CryptoUtils.hash(it) }
            .map { BigInteger(1, it) }

        dataset.forEachIndexed { idx, data ->
            assert(trie.addData(data)) { "Unable to add data: $data" }

            if (trie.getNeighborhoods().size > 1)
                trie.getNeighborhoods().forEach {
                    assert(it.size >= 20) { "ADD: Invalid cluster count: ${it.size} at element: $idx" }
                }

            assert(trie.flatten().size == idx + 1) { "ADD: Invalid trie size: ${trie.flatten().size} at element: $idx" }
        }

        println(trie)

        dataset.forEachIndexed { idx, data ->
            assert(trie.removeData(data)) { "Unable to remove data: ${data.toString(2)}" }

            if (trie.getNeighborhoods().size > 1)
                trie.getNeighborhoods().forEach {
                    assert(it.size >= 20) { "REMOVE: Invalid cluster count: ${it.size} at element: $idx" }
                }

            assert(trie.flatten().size == (dataset.size - (idx + 1))) { "REMOVE: Invalid trie size: ${trie.flatten().size} at element: $idx" }
        }
    }
}