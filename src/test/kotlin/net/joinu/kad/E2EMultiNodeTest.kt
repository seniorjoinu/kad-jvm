package net.joinu.kad

import kotlinx.coroutines.experimental.runBlocking
import net.joinu.kad.discovery.KademliaService
import net.joinu.kad.discovery.addressbook.AddressBook
import net.joinu.osen.Address
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.util.*


data class Ports(val web: Int, val p2p: Int) {
    fun toProperties(): Properties {
        val props = Properties()
        props.setProperty("server.port", web.toString())
        props.setProperty("node.port", p2p.toString())

        return props
    }
}

fun createNodes(count: Int, baseWebPort: Int = 8080, baseP2PPort: Int = 1337): List<Ports> {
    val nodesPorts = arrayListOf<Ports>()
    for (i in 1..count) {
        nodesPorts.add(Ports(baseWebPort + i, baseP2PPort + i))
    }

    return nodesPorts
}

@RunWith(SpringJUnit4ClassRunner::class)
class E2EMultiNodeTest {
    lateinit var nodesPorts: List<Ports>
    private val host = "localhost"

    private val nodesCount = 10 // change this to change number of nodes

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
    fun `peers are able to ping each other`() = runBlocking {
        val services = appContexts.map { it.getBean(KademliaService::class.java) }

        services.forEach { service ->
            assert(nodesPorts.map { service.ping(Address(host, it.p2p)) }.all { it })
        }
    }

    @Test
    fun `peers are able to find each other via bootstrap-like node`() = runBlocking {
        val services = appContexts.map { it.getBean(KademliaService::class.java) }
        val addressBooks = appContexts.map { it.getBean(AddressBook::class.java) }

        // pinging peers 1 by 1, so all peers connected
        val pivot = services.first()
        assert(nodesPorts.map { pivot.ping(Address(host, it.p2p)) }.all { it })

        services.forEachIndexed { idx, service ->
            val nodes = addressBooks.map { service.findNode(it.getMine().id) }
            assert(nodes.all { it != null })
        }
    }

    @Test
    fun `peers are able to find each other connected 1 by 1 in a ring`() = runBlocking {
        val services = appContexts.map { it.getBean(KademliaService::class.java) }
        val addressBooks = appContexts.map { it.getBean(AddressBook::class.java) }

        // connect each other in ring
        services.forEachIndexed { idx, kademliaService ->
            val peerPort = if ((idx + 1) > nodesPorts.lastIndex)
                nodesPorts[0]
            else
                nodesPorts[idx + 1]

            kademliaService.ping(Address(host, peerPort.p2p))
        }
        assert(addressBooks.map { it.getRecords().isNotEmpty() }.all { it })

        val found = services.map { service ->
            addressBooks.map { service.findNode(it.getMine().id) }
        }
        val correct = found.map { it.all { it != null } }.all { it }
        assert(correct)
    }

    @Test
    fun `peers are able to find each other after bootstrap`() = runBlocking {
        val services = appContexts.map { it.getBean(KademliaService::class.java) }
        val addressBooks = appContexts.map { it.getBean(AddressBook::class.java) }

        services.forEachIndexed { index, kademliaService ->
            if (index != 0) assert(kademliaService.bootstrap(Address(host, nodesPorts[0].p2p)))
        }

        val found = services.map { service ->
            addressBooks.map { service.findNode(it.getMine().id) }
        }
        val correct = found.map { it.all { it != null } }.all { it }
        assert(correct)
    }
}