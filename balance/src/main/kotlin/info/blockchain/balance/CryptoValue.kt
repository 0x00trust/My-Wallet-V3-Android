package info.blockchain.balance

import java.math.BigDecimal
import java.math.BigInteger

data class CryptoValue(
    val currency: CryptoCurrency,

    /**
     * Amount in the smallest unit of the currency, Satoshi/Wei for example.
     */
    val amount: BigInteger
) {
    /**
     * Amount in the major value of the currency, Bitcoin/Ether for example.
     */
    fun toMajorUnit(): BigDecimal {
        return currency.smallestUnitValueToBigDecimal(amount)
    }

    fun isPositive(): Boolean {
        return amount.signum() == 1
    }

    fun isZero(): Boolean {
        return amount.signum() == 0
    }

    companion object {
        val ZeroBtc = bitcoinFromSatoshis(0L)
        val ZeroBch = bitcoinCashFromSatoshis(0L)
        val ZeroEth = CryptoValue(CryptoCurrency.ETHER, BigInteger.ZERO)

        fun zero(cryptoCurrency: CryptoCurrency) = when (cryptoCurrency) {
            CryptoCurrency.BTC -> ZeroBtc
            CryptoCurrency.BCH -> ZeroBch
            CryptoCurrency.ETHER -> ZeroEth
        }

        fun bitcoinFromSatoshis(satoshi: Long) = CryptoValue(CryptoCurrency.BTC, satoshi.toBigInteger())
        fun bitcoinCashFromSatoshis(satoshi: Long) = CryptoValue(CryptoCurrency.BCH, satoshi.toBigInteger())
        fun etherFromWei(wei: Long) = CryptoValue(CryptoCurrency.ETHER, wei.toBigInteger())

        fun bitcoinFromMajor(bitcoin: Int) = bitcoinFromMajor(bitcoin.toBigDecimal())
        fun bitcoinFromMajor(bitcoin: BigDecimal) = fromMajor(CryptoCurrency.BTC, bitcoin)

        fun bitcoinCashFromMajor(bitcoinCash: Int) = bitcoinCashFromMajor(bitcoinCash.toBigDecimal())
        fun bitcoinCashFromMajor(bitcoinCash: BigDecimal) = fromMajor(CryptoCurrency.BCH, bitcoinCash)

        fun etherFromMajor(ether: Long) = etherFromMajor(ether.toBigDecimal())
        fun etherFromMajor(ether: BigDecimal) = fromMajor(CryptoCurrency.ETHER, ether)

        fun fromMajor(
            currency: CryptoCurrency,
            major: BigDecimal
        ) = CryptoValue(currency, major.movePointRight(currency.dp).toBigInteger())

        fun min(a: CryptoValue, b: CryptoValue) = if (a <= b) a else b

        fun max(a: CryptoValue, b: CryptoValue) = if (a >= b) a else b
    }

    /**
     * Amount in the major value of the currency, Bitcoin/Ether for example.
     */
    fun toMajorUnitDouble() = toMajorUnit().toDouble()
}

operator fun CryptoValue.compareTo(other: CryptoValue): Int {
    ensureComparable(currency, other.currency)
    return amount.compareTo(other.amount)
}

private fun ensureComparable(a: CryptoCurrency, b: CryptoCurrency) {
    if (a != b) throw Exception("Can't compare ${a.symbol} and ${b.symbol}")
}

fun CryptoCurrency.withMajorValue(majorValue: BigDecimal) = CryptoValue.fromMajor(this, majorValue)
