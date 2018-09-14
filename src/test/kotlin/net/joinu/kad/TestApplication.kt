package net.joinu.kad

import net.joinu.kad.discovery.AddressBook
import net.joinu.kad.discovery.InMemoryAddressBook
import net.joinu.kad.discovery.KAddress
import net.joinu.osen.Address
import net.joinu.osen.P2P
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.support.GenericApplicationContext
import java.math.BigInteger
import java.util.*


@SpringBootApplication
open class TestApplication {
    private val packageToScan = TestApplication::class.java.`package`.name

    @Bean()
    open fun netInitializer(): P2P {
        // default port is 1337, to switch it use "node.port" spring env property
        return P2P(basePackages = arrayOf(packageToScan))
    }

    @Bean()
    open fun addressBook(context: GenericApplicationContext): AddressBook {
        var port = 1337
        val portFromProps = context.environment.getProperty("node.port")
        if (portFromProps != null) {
            port = portFromProps.toInt()
        }

        val id = BigInteger.valueOf(Random().nextLong())
        val address = Address("localhost", port)

        return InMemoryAddressBook(KAddress(id, address))
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(TestApplication::class.java, *args)
}