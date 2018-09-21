package net.joinu.kad

import kotlinx.coroutines.experimental.runBlocking
import net.joinu.kad.discovery.KademliaService
import net.joinu.kad.discovery.addressbook.AddressBook
import net.joinu.kad.discovery.addressbook.getMyCluster
import net.joinu.osen.Address
import org.junit.After
import org.junit.Test


class E2EClusterTest : MultiNodeTest() {
    init {
        k = 3
        nodesCount = 100
    }

    @After
    fun clearAddresses() {
        val addressBooks = MultiNodeTest.appContexts.map { it.getBean(AddressBook::class.java) }
        addressBooks.forEach { it.clear() }
    }

    @Test
    fun `peers contained in a same cluster are always have it in consistent state`() = runBlocking {
        val services = appContexts.map { it.getBean(KademliaService::class.java) }
        val addressBooks = appContexts.map { it.getBean(AddressBook::class.java) }

        services.forEachIndexed { index, kademliaService ->
            if (index == 0) return@forEachIndexed

            assert(kademliaService.bootstrap(Address(host, nodesConfigs[0].p2pPort)))

            val clusters = addressBooks.map { it.getMyCluster() }
            val clustersByLabels = clusters.groupBy { it.name }

            val clustersConsistent = clustersByLabels.values.all { clustersWithSameName ->
                val first = clustersWithSameName.first().peers.toSet()
                clustersWithSameName.all { cluster -> cluster.peers.toSet() == first }
            }

            assert(clustersConsistent)
        }

        services.forEachIndexed { index, kademliaService ->
            kademliaService.byeMyCluster()

            val clusters = addressBooks.map { it.getMyCluster() }
            val clustersByLabels = clusters.groupBy { it.name }

            val clustersConsistent = clustersByLabels.values.all { clustersWithSameName ->
                val first = clustersWithSameName.first().peers.toSet()
                clustersWithSameName.all { cluster -> cluster.peers.toSet() == first }
            }

            assert(clustersConsistent)
        }
    }
}