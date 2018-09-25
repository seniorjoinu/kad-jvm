package net.joinu

import net.joinu.utils.CryptoUtils
import org.junit.Ignore
import org.junit.Test
import java.math.BigInteger
import java.security.KeyPairGenerator


class KeyGenerationBenchmark {
    @Test
    @Ignore
    fun benchmarkECDSAKeys() {
        val keyGen = KeyPairGenerator.getInstance("EC")

        var difficulty = 1

        var before = System.currentTimeMillis()
        print("Difficulty = $difficulty; ")
        while (difficulty < 50) {
            val key = keyGen.genKeyPair()
            var correct = true
            val keyhash = BigInteger(1, CryptoUtils.hash(key.public.encoded))

            for (i in (255-difficulty..255)) {
                if (keyhash.testBit(i)) {
                    correct = false
                }
            }

            if (correct) {
                difficulty++
                val after = System.currentTimeMillis()

                println("ms wasted = ${after - before} (s = ${(after - before) / 1000.0}, m = ${(after - before) / 1000.0 / 60.0}, h = ${(after - before) / 1000.0 / 60.0 / 60.0}); generated key = ${keyhash.toString(2).padStart(256, '0')}")

                before = System.currentTimeMillis()
                print("Difficulty = $difficulty; ")
            }
        }
    }
}