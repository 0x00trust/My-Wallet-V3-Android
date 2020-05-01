package com.blockchain.swap.nabu.datamanagers.custodialwalletimpl

import com.blockchain.swap.nabu.datamanagers.BankAccount
import com.blockchain.swap.nabu.datamanagers.BankDetail
import com.blockchain.swap.nabu.datamanagers.BillingAddress
import com.blockchain.swap.nabu.datamanagers.BuyLimits
import com.blockchain.swap.nabu.datamanagers.BuyOrder
import com.blockchain.swap.nabu.datamanagers.BuyOrderList
import com.blockchain.swap.nabu.datamanagers.CardToBeActivated
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.PartnerCredentials
import com.blockchain.swap.nabu.datamanagers.PaymentMethod
import com.blockchain.swap.nabu.datamanagers.Quote
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPair
import com.blockchain.swap.nabu.datamanagers.SimpleBuyPairs
import com.blockchain.swap.nabu.models.simplebuy.CardPartnerAttributes
import com.blockchain.swap.nabu.models.tokenresponse.NabuOfflineTokenResponse
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import java.util.Date
import java.util.concurrent.TimeUnit

// Provide mock data for development and testing etc
class StubCustodialWalletManager : CustodialWalletManager {

    override fun getBuyLimitsAndSupportedCryptoCurrencies(
        nabuOfflineTokenResponse: NabuOfflineTokenResponse,
        fiatCurrency: String
    ): Single<SimpleBuyPairs> =
        Single.just(
            SimpleBuyPairs(
                listOf(
                    SimpleBuyPair(
                        pair = "BTC-USD",
                        buyLimits = BuyLimits(
                            100,
                            5024558
                        )
                    ),
                    SimpleBuyPair(
                        pair = "ETH-USD",
                        buyLimits = BuyLimits(
                            100,
                            5024558
                        )
                    ),
                    SimpleBuyPair(
                        pair = "BCH-USD",
                        buyLimits = BuyLimits(
                            100,
                            5024558
                        )
                    ),
                    SimpleBuyPair(
                        pair = "XLM-USD",
                        buyLimits = BuyLimits(
                            100,
                            5024558
                        )
                    ),
                    SimpleBuyPair(
                        pair = "BTC-EUR",
                        buyLimits = BuyLimits(
                            1006,
                            10000
                        )
                    ),
                    SimpleBuyPair(
                        pair = "ETH-EUR",
                        buyLimits = BuyLimits(
                            1005,
                            10000
                        )
                    ),
                    SimpleBuyPair(
                        pair = "BCH-EUR",
                        buyLimits = BuyLimits(
                            1001,
                            10000
                        )
                    ),
                    SimpleBuyPair(
                        pair = "BTC-GBP",
                        buyLimits = BuyLimits(
                            1006,
                            10000
                        )
                    ),
                    SimpleBuyPair(
                        pair = "ETH-GBP",
                        buyLimits = BuyLimits(
                            1005,
                            10000
                        )
                    ),
                    SimpleBuyPair(
                        pair = "BCH-GBP",
                        buyLimits = BuyLimits(
                            1001,
                            10000
                        )
                    )
                )
            )
        )

    override fun getBankAccountDetails(currency: String): Single<BankAccount> =
        Single.just(
            BankAccount(
                listOf(
                    BankDetail("Bank Name", "LHV"),
                    BankDetail("Bank ID", "DE81 1234 5678 9101 1234 33", true),
                    BankDetail("Bank Code (SWIFT/BIC)", "DEKTDE7GSSS", true),
                    BankDetail("Recipient", "Fred Wilson")
                )
            )
        )

    override fun getQuote(action: String, crypto: CryptoCurrency, amount: FiatValue): Single<Quote> =
        Single.just(Quote(date = Date(),
            fee = FiatValue.zero(amount.currencyCode),
            estimatedAmount = CryptoValue.ZeroBtc))

    override fun createOrder(
        cryptoCurrency: CryptoCurrency,
        amount: FiatValue,
        action: String,
        paymentMethodId: String?,
        stateAction: String?
    ): Single<BuyOrder> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getPredefinedAmounts(currency: String): Single<List<FiatValue>> = Single.just(
        listOf(
            FiatValue.fromMinor(currency, 100000),
            FiatValue.fromMinor(currency, 5000),
            FiatValue.fromMinor(currency, 1000),
            FiatValue.fromMinor(currency, 500)

        ))

    override fun isEligibleForSimpleBuy(fiatCurrency: String): Single<Boolean> =
        Single.just(true)

    override fun isCurrencySupportedForSimpleBuy(fiatCurrency: String): Single<Boolean> =
        Single.just(true)

    override fun getBalanceForAsset(
        crypto: CryptoCurrency
    ): Maybe<CryptoValue> =
        when (crypto) {
            CryptoCurrency.BTC -> Maybe.just(CryptoValue.bitcoinFromSatoshis(726800000))
            CryptoCurrency.ETHER -> Maybe.just(CryptoValue.ZeroEth)
            CryptoCurrency.BCH -> Maybe.empty()
            CryptoCurrency.XLM -> Maybe.empty()
            CryptoCurrency.PAX -> Maybe.just(CryptoValue.usdPaxFromMajor(2785.toBigDecimal()))
            CryptoCurrency.STX -> Maybe.empty()
        }

    override fun getOutstandingBuyOrders(crypto: CryptoCurrency): Single<BuyOrderList> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getAllOutstandingBuyOrders(): Single<BuyOrderList> =
        getAllBuyOrdersFor(CryptoCurrency.BTC)

    override fun getAllBuyOrdersFor(crypto: CryptoCurrency): Single<BuyOrderList> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getBuyOrder(orderId: String): Single<BuyOrder> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getSupportedFiatCurrencies(nabuOfflineTokenResponse: NabuOfflineTokenResponse): Single<List<String>> =
        Single.just(listOf(
            "GBP", "EUR"
        ))

    override fun deleteBuyOrder(orderId: String): Completable {
        return Completable.complete()
    }

    override fun transferFundsToWallet(amount: CryptoValue, walletAddress: String): Completable =
        Completable.timer(5, TimeUnit.SECONDS)

    override fun cancelAllPendingBuys(): Completable {
        return Completable.complete()
    }

    override fun fetchSuggestedPaymentMethod(fiatCurrency: String, isTier2Approved: Boolean):
            Single<List<PaymentMethod>> =
        Single.just(
            emptyList()
        )

    override fun addNewCard(fiatCurrency: String, billingAddress: BillingAddress): Single<CardToBeActivated> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun activateCard(cardId: String, attributes: CardPartnerAttributes): Single<PartnerCredentials> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun getCardDetails(cardId: String): Single<PaymentMethod.Card> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun confirmOrder(orderId: String, attributes: CardPartnerAttributes?): Single<BuyOrder> {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }
}
