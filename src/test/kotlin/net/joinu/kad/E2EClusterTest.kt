package net.joinu.kad

import kotlinx.coroutines.experimental.runBlocking
import net.joinu.kad.discovery.KAddress
import net.joinu.kad.discovery.KadId
import net.joinu.kad.discovery.KademliaService
import net.joinu.kad.discovery.addressbook.AddressBook
import net.joinu.kad.discovery.addressbook.getMyCluster
import net.joinu.osen.Address
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import java.math.BigInteger


class E2EClusterTest {
    lateinit var nodesPorts: List<Ports>
    private val host = "localhost"

    private val nodesCount = 1000 // change this to change number of nodes

    lateinit var apps: List<SpringApplicationBuilder>
    lateinit var appContexts: List<ConfigurableApplicationContext>

    @Before
    fun initNodes() {
        nodesPorts = createNodes(nodesCount)

        apps = nodesPorts.map { SpringApplicationBuilder(TestApplication::class.java).properties(it.toProperties()) }
        appContexts = apps.map { it.run() }
    }

    @After
    fun destroyNodes() {
        appContexts.forEach {
            it.close()
        }
    }

    @Test
    fun `peers contained in a same cluster are always have it in consistent state`() = runBlocking {
        val services = appContexts.map { it.getBean(KademliaService::class.java) }
        val addressBooks = appContexts.map { it.getBean(AddressBook::class.java) }

        services.forEachIndexed { index, kademliaService ->
            if (index == 0) return@forEachIndexed

            assert(kademliaService.bootstrap(Address(host, nodesPorts[0].p2p)))

            val clusters = addressBooks.map { it.getMyCluster() }
            val clustersByLabels = clusters.groupBy { getClusterLabel(it) }

            val clustersConsistent = clustersByLabels.values.all { clustersWithSameName ->
                val first = clustersWithSameName.first().toSet()
                clustersWithSameName.all { cluster -> cluster.toSet() == first }
            }

            assert(clustersConsistent)
        }

        services.forEachIndexed { index, kademliaService ->
            kademliaService.byeAll()

            val clusters = addressBooks.map { it.getMyCluster() }
            val clustersByLabels = clusters.groupBy { getClusterLabel(it) }

            val clustersConsistent = clustersByLabels.values.all { clustersWithSameName ->
                val first = clustersWithSameName.first().toSet()
                clustersWithSameName.all { cluster -> cluster.toSet() == first }
            }

            assert(clustersConsistent)
        }
    }
}

fun getClusterLabel(cluster: List<KAddress>): KadId {
    val sum = cluster.fold(BigInteger.ZERO) { acc, addr -> acc + addr.id }
    val size = BigInteger.valueOf(cluster.size.toLong())

    if (size == BigInteger.ZERO) {
        println()
    }

    return sum / size
}