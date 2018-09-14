package net.joinu.kad.discovery

import net.joinu.osen.Address
import java.math.BigInteger


typealias KadId = BigInteger

data class KAddress(val id: KadId, val address: Address)

