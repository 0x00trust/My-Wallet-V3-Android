package info.blockchain.balance

import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.ether
import com.blockchain.testutils.gbp
import com.blockchain.testutils.usd
import org.amshove.kluent.`should be equal to`
import org.junit.Test

class ToZeroTest {

    @Test
    fun `bitcoin to zero`() {
        val zero: CryptoValue = 1.bitcoin().toZero()
        zero `should be equal to` CryptoValue.zero(CryptoCurrency.BTC)
    }

    @Test
    fun `ether to zero`() {
        9.1.ether().toZero() `should be equal to` CryptoValue.zero(CryptoCurrency.ETHER)
    }

    @Test
    fun `bitcoin to zero via money`() {
        val bitcoin: Money = 1.bitcoin()
        val zero: Money = bitcoin.toZero()
        zero `should be equal to` CryptoValue.zero(CryptoCurrency.BTC)
    }

    @Test
    fun `gbp toZero`() {
        val zero: FiatValue = 1.2.gbp().toZero()
        zero `should be equal to` 0.gbp()
    }

    @Test
    fun `usd toZero`() {
        9.8.usd().toZero() `should be equal to` 0.usd()
    }

    @Test
    fun `usd to zero via money`() {
        val usd: Money = 1.usd()
        val zero: Money = usd.toZero()
        zero `should be equal to` 0.usd()
    }
}
