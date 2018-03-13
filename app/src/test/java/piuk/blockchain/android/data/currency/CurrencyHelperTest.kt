//package piuk.blockchain.android.data.currency
//
//import com.nhaarman.mockito_kotlin.any
//import com.nhaarman.mockito_kotlin.mock
//import com.nhaarman.mockito_kotlin.verify
//import com.nhaarman.mockito_kotlin.whenever
//import org.amshove.kluent.`should be`
//import org.amshove.kluent.`should equal to`
//import org.junit.Before
//import org.junit.Test
//import org.mockito.ArgumentMatchers
//import piuk.blockchain.android.data.exchangerate.ExchangeRateDataManager
//import piuk.blockchain.android.util.MonetaryUtil
//import piuk.blockchain.android.util.PrefsUtil
//import java.math.BigInteger
//import java.text.DecimalFormat
//import java.text.NumberFormat
//import java.util.*
//
//@Suppress("IllegalIdentifier")
//class CurrencyHelperTest {
//
//    private lateinit var subject: CurrencyHelper
//    private val prefsUtil: PrefsUtil = mock()
//    private val exchangeRateFactory: ExchangeRateDataManager = mock()
//    private val monetaryUtil: MonetaryUtil = mock()
//    private val currencyState: CurrencyState = mock()
//
//    @Before
//    @Throws(Exception::class)
//    fun setUp() {
//        subject = CurrencyHelper(
//                monetaryUtil,
//                Locale.UK,
//                prefsUtil,
//                exchangeRateFactory,
//                currencyState
//        )
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun getBtcUnit() {
//        // Arrange
//        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
//        // Act
//        val value = subject.cryptoUnit
//        // Assert
//        value `should equal to` "BTC"
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun getEthUnit() {
//        // Arrange
//        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.ETHER)
//        // Act
//        val value = subject.cryptoUnit
//        // Assert
//        value `should equal to` "ETH"
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun `getCryptoUnit btc`() {
//        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
//        // Act
//        val value = subject.cryptoUnit
//        // Assert
//        value `should equal to` "BTC"
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun `getCryptoUnit eth`() {
//        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.ETHER)
//        // Act
//        val value = subject.cryptoUnit
//        // Assert
//        value `should equal to` "ETH"
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun getFiatUnit() {
//        // Arrange
//        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
//                .thenReturn("GBP")
//        // Act
//        val value = subject.fiatUnit
//        // Assert
//        value `should equal to` "GBP"
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun `getLastPrice btc`() {
//        // Arrange
//        whenever(exchangeRateFactory.getLastBtcPrice(any())).thenReturn(1000.0)
//        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
//                .thenReturn("GBP")
//        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
//        // Act
//        val value = subject.lastPrice
//        // Assert
//        verify(exchangeRateFactory).getLastBtcPrice(any())
//        value `should equal to` 1000.0
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun `getLastPrice eth`() {
//        // Arrange
//        whenever(exchangeRateFactory.getLastEthPrice(any())).thenReturn(1000.0)
//        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
//                .thenReturn("GBP")
//        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.ETHER)
//        // Act
//        val value = subject.lastPrice
//        // Assert
//        verify(exchangeRateFactory).getLastEthPrice(any())
//        value `should equal to` 1000.0
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun getFormattedBtcString() {
//        // Arrange
//        val format = DecimalFormat.getInstance(Locale.US)
//        whenever(monetaryUtil.getBtcFormat()).thenReturn(format as DecimalFormat)
//        // Act
//        val value = subject.getFormattedBtcString(13.37)
//        // Assert
//        value `should equal to` "13.37"
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun getFormattedFiatString() {
//        // Arrange
//        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
//                .thenReturn("USD")
//        val format = NumberFormat.getCurrencyInstance(Locale.US)
//        whenever(monetaryUtil.getFiatFormat(ArgumentMatchers.anyString()))
//                .thenReturn(format as DecimalFormat)
//        /// Act
//        val value = subject.getFormattedFiatString(13.37)
//        // Assert
//        value `should equal to` "$13.37"
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun getFormattedCryptoStringFromFiat() {
//        // Arrange
//        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
//        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
//                .thenReturn("GBP")
//        whenever(exchangeRateFactory.getLastBtcPrice(any())).thenReturn(4500.0)
//        val format = DecimalFormat.getInstance(Locale.US)
//        whenever(monetaryUtil.getBtcFormat()).thenReturn(format as DecimalFormat)
//        // Act
//        val value = subject.getFormattedCryptoStringFromFiat(13.37)
//        // Assert
//        value `should equal to` "1,337"
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun getFormattedFiatStringFromCrypto() {
//        // Arrange
//        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.BTC)
//        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
//                .thenReturn("GBP")
//        whenever(exchangeRateFactory.getLastBtcPrice(any())).thenReturn(4500.0)
//        val format = DecimalFormat.getInstance(Locale.US)
//        whenever(monetaryUtil.getFiatFormat(any())).thenReturn(format as DecimalFormat)
//        // Act
//        val value = subject.getFormattedFiatStringFromCrypto(13.37)
//        // Assert
//        value `should equal to` "6,016,500"
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun getMaxBtcDecimalLengthBtc() {
//        // Arrange
//        // Act
//        val value = subject.maxBtcDecimalLength
//        // Assert
//        value `should equal to` 8
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun `getMaxCryptoDecimalLength eth`() {
//        // Arrange
//        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrencies.ETHER)
//        // Act
//        val value = subject.maxCryptoDecimalLength
//        // Assert
//        value `should equal to` 18
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun getLongAmount() {
//        // Arrange
//
//        // Act
//        val value = subject.getLongAmount("13.37")
//        // Assert
//        value `should equal to` 1337000000
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun getLongAmountInvalidString() {
//        // Arrange
//
//        // Act
//        val value = subject.getLongAmount("leet")
//        // Assert
//        value `should equal to` 0
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun getDoubleAmount() {
//        // Arrange
//
//        // Act
//        val value = subject.getDoubleAmount("13.37")
//        // Assert
//        value `should equal to` 13.37
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun getDoubleAmountInvalidString() {
//        // Arrange
//
//        // Act
//        val value = subject.getDoubleAmount("leet")
//        // Assert
//        value `should equal to` 0.0
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun getIfAmountInvalid() {
//        // Arrange
//
//        // Act
//        val value = subject.getIfAmountInvalid(BigInteger("2100000000000001"))
//        // Assert
//        value `should be` true
//    }
//
//    @Test
//    @Throws(Exception::class)
//    fun getTextFromSatoshis() {
//        // Arrange
//        whenever(monetaryUtil.getDisplayAmount(10001234)).thenReturn("1,000.1234")
//        // Act
//        val result = subject.getTextFromSatoshis(10001234, ".")
//        // Assert
//        result `should equal to` "1,000.1234"
//    }
//
//}