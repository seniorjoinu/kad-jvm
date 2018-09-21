package net.joinu.kad

import org.junit.AfterClass
import org.junit.BeforeClass
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import java.util.*


open class MultiNodeTest {
    companion object {
        lateinit var nodesConfigs: List<NodeConfig>

        lateinit var apps: List<SpringApplicationBuilder>
        lateinit var appContexts: List<ConfigurableApplicationContext>

        var nodesCount: Int = 100
        var host = "localhost"
        var k: Int = 20
        var baseWebPort: Int = 8080
        var baseP2PPort: Int = 1337

        @JvmStatic
        @BeforeClass
        fun initNodes() {
            nodesConfigs = createNodes(nodesCount, k, baseWebPort, baseP2PPort)

            apps = nodesConfigs.map { SpringApplicationBuilder(TestApplication::class.java).properties(it.toProperties()) }
            appContexts = apps.map { it.run() }
        }

        @JvmStatic
        @AfterClass
        fun destroyNodes() {
            appContexts.forEach {
                it.close()
            }
        }
    }
}

data class NodeConfig(val webPort: Int, val p2pPort: Int, val k: Int) {
    fun toProperties(): Properties {
        val props = Properties()
        props.setProperty("server.port", webPort.toString())
        props.setProperty("node.port", p2pPort.toString())
        props.setProperty("node.k", k.toString())

        return props
    }
}

fun createNodes(count: Int, k: Int, baseWebPort: Int, baseP2PPort: Int): List<NodeConfig> {
    val nodesPorts = arrayListOf<NodeConfig>()
    for (i in 1..count) {
        nodesPorts.add(NodeConfig(baseWebPort + i, baseP2PPort + i, k))
    }

    return nodesPorts
}