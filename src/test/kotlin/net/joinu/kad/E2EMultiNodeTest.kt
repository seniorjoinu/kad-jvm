package net.joinu.kad

import kotlinx.coroutines.experimental.runBlocking
import net.joinu.kad.discovery.KademliaService
import net.joinu.kad.discovery.addressbook.AddressBook
import net.joinu.osen.Address
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner


@RunWith(SpringJUnit4ClassRunner::class)
class E2EMultiNodeTest : MultiNodeTest() {

    init {
        k = 10
        nodesCount = 100
    }

    @After
    fun clearAddresses() {
        val addressBooks = appContexts.map { it.getBean(AddressBook::class.java) }
        addressBooks.forEach { it.clear() }
    }

    @Test
    fun `peers are able to ping each other`() = runBlocking {
        val services = appContexts.map { it.getBean(KademliaService::class.java) }

        services.forEach { service ->
            assert(nodesConfigs.map { service.ping(Address(host, it.p2pPort)) }.all { it })
        }
    }

    @Test
    fun `peers are able to find each other connected 1 by 1 in a ring`() = runBlocking {
        val services = appContexts.map { it.getBean(KademliaService::class.java) }
        val addressBooks = appContexts.map { it.getBean(AddressBook::class.java) }

        // connect each other in ring
        services.forEachIndexed { idx, kademliaService ->
            val port = if ((idx + 1) > nodesConfigs.lastIndex)
                nodesConfigs[0].p2pPort
            else
                nodesConfigs[idx + 1].p2pPort

            kademliaService.ping(Address(host, port))
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
            if (index != 0) assert(kademliaService.bootstrap(Address(host, nodesConfigs[0].p2pPort)))
            println("NODE $index BOOTSTRAPPED")
        }

        val found = services.mapIndexed { idx, service ->
            val foundAll = addressBooks.map { service.findNode(it.getMine().id) }
            println("NODE $idx FOUND ALL OTHER NODES")
            foundAll
        }
        val correct = found.map { it.all { it != null } }.all { it }
        assert(correct)
    }
}