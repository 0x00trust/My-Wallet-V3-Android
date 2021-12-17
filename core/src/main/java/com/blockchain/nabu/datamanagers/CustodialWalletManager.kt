package com.blockchain.nabu.datamanagers

import android.annotation.SuppressLint
import com.blockchain.core.limits.LegacyLimits
import com.blockchain.nabu.datamanagers.custodialwalletimpl.CardStatus
import com.blockchain.nabu.datamanagers.custodialwalletimpl.LiveCustodialWalletManager
import com.blockchain.nabu.datamanagers.custodialwalletimpl.OrderType
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.datamanagers.repositories.interest.Eligibility
import com.blockchain.nabu.datamanagers.repositories.interest.InterestLimits
import com.blockchain.nabu.datamanagers.repositories.swap.TradeTransactionItem
import com.blockchain.nabu.models.data.BankTransferDetails
import com.blockchain.nabu.models.data.CryptoWithdrawalFeeAndLimit
import com.blockchain.nabu.models.data.FiatWithdrawalFeeAndLimit
import com.blockchain.nabu.models.data.LinkBankTransfer
import com.blockchain.nabu.models.data.LinkedBank
import com.blockchain.nabu.models.data.RecurringBuy
import com.blockchain.nabu.models.data.RecurringBuyPaymentDetails
import com.blockchain.nabu.models.data.RecurringBuyState
import com.blockchain.nabu.models.responses.banktransfer.ProviderAccountAttrs
import com.blockchain.nabu.models.responses.interest.InterestActivityItemResponse
import com.blockchain.nabu.models.responses.interest.InterestAttributes
import com.blockchain.nabu.models.responses.simplebuy.CustodialWalletOrder
import com.blockchain.nabu.models.responses.simplebuy.RecurringBuyRequestBody
import com.blockchain.nabu.models.responses.simplebuy.SimpleBuyConfirmationAttributes
import com.braintreepayments.cardform.utils.CardType
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.wallet.multiaddress.TransactionSummary
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single
import java.io.Serializable
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Date
import java.util.Locale

enum class OrderState {
    UNKNOWN,
    UNINITIALISED,
    INITIALISED,
    PENDING_CONFIRMATION, // Has created but not confirmed
    AWAITING_FUNDS, // Waiting for a bank transfer etc
    PENDING_EXECUTION, // Funds received, but crypto not yet released (don't know if we'll need this?)
    FINISHED,
    CANCELED,
    FAILED;

    fun isPending(): Boolean =
        this == PENDING_CONFIRMATION ||
            this == PENDING_EXECUTION ||
            this == AWAITING_FUNDS

    fun hasFailed(): Boolean = this == FAILED

    fun isFinished(): Boolean = this == FINISHED

    fun isCancelled(): Boolean = this == CANCELED
}

interface CustodialWalletManager {
    fun getSupportedBuySellCryptoCurrencies(): Single<List<CurrencyPair.CryptoToFiatCurrencyPair>>

    fun getSupportedFiatCurrencies(): Single<List<String>>

    fun fetchFiatWithdrawFeeAndMinLimit(
        asset: String,
        product: Product,
        paymentMethodType: PaymentMethodType
    ): Single<FiatWithdrawalFeeAndLimit>

    fun fetchCryptoWithdrawFeeAndMinLimit(
        asset: AssetInfo,
        product: Product
    ): Single<CryptoWithdrawalFeeAndLimit>

    fun fetchWithdrawLocksTime(
        paymentMethodType: PaymentMethodType,
        fiatCurrency: String
    ): Single<BigInteger>

    fun createOrder(
        custodialWalletOrder: CustodialWalletOrder,
        stateAction: String? = null
    ): Single<BuySellOrder>

    fun createRecurringBuyOrder(
        recurringBuyRequestBody: RecurringBuyRequestBody
    ): Single<RecurringBuyOrder>

    fun createWithdrawOrder(
        amount: Money,
        bankId: String
    ): Completable

    fun getPredefinedAmounts(
        currency: String
    ): Single<List<FiatValue>>

    fun getCustodialFiatTransactions(
        currency: String,
        product: Product,
        type: String? = null
    ): Single<List<FiatTransaction>>

    fun getCustodialCryptoTransactions(
        currency: String,
        product: Product,
        type: String? = null
    ): Single<List<CryptoTransaction>>

    fun getBankAccountDetails(
        currency: String
    ): Single<BankAccount>

    fun getCustodialAccountAddress(asset: AssetInfo): Single<String>

    fun isCurrencySupportedForSimpleBuy(
        fiatCurrency: String
    ): Single<Boolean>

    fun getOutstandingBuyOrders(asset: AssetInfo): Single<BuyOrderList>

    fun getAllOutstandingBuyOrders(): Single<BuyOrderList>

    fun getAllOutstandingOrders(): Single<List<BuySellOrder>>

    fun getAllOrdersFor(asset: AssetInfo): Single<BuyOrderList>

    fun getBuyOrder(orderId: String): Single<BuySellOrder>

    fun deleteBuyOrder(orderId: String): Completable

    fun deleteCard(cardId: String): Completable

    fun removeBank(bank: Bank): Completable

    fun transferFundsToWallet(amount: CryptoValue, walletAddress: String): Single<String>

    // For test/dev
    fun cancelAllPendingOrders(): Completable

    fun updateSupportedCardTypes(fiatCurrency: String): Completable

    fun linkToABank(fiatCurrency: String): Single<LinkBankTransfer>

    fun updateSelectedBankAccount(
        linkingId: String,
        providerAccountId: String = "",
        accountId: String = "",
        attributes: ProviderAccountAttrs
    ): Completable

    fun fetchSuggestedPaymentMethod(
        fiatCurrency: String,
        fetchSddLimits: Boolean,
        onlyEligible: Boolean
    ): Single<List<PaymentMethod>>

    fun getEligiblePaymentMethodTypes(
        fiatCurrency: String
    ): Single<List<EligiblePaymentMethodType>>

    fun getCardAcquirers(): Single<List<PaymentCardAcquirer>>

    fun getBankTransferLimits(
        fiatCurrency: String,
        onlyEligible: Boolean
    ): Single<PaymentLimits>

    fun addNewCard(
        fiatCurrency: String,
        billingAddress: BillingAddress,
        paymentMethodTokens: Map<String, String>? = null
    ): Single<CardToBeActivated>

    fun activateCard(cardId: String, attributes: SimpleBuyConfirmationAttributes): Single<PartnerCredentials>

    fun getCardDetails(cardId: String): Single<PaymentMethod.Card>

    fun fetchUnawareLimitsCards(
        states: List<CardStatus>
    ): Single<List<PaymentMethod.Card>> // fetches the available

    fun confirmOrder(
        orderId: String,
        attributes: SimpleBuyConfirmationAttributes?,
        paymentMethodId: String?,
        isBankPartner: Boolean?
    ): Single<BuySellOrder>

    fun getInterestAccountRates(asset: AssetInfo): Single<Double>

    fun getInterestAccountAddress(asset: AssetInfo): Single<String>

    fun getInterestActivity(asset: AssetInfo): Single<List<InterestActivityItem>>

    fun getInterestLimits(asset: AssetInfo): Single<InterestLimits>

    fun getInterestAvailabilityForAsset(asset: AssetInfo): Single<Boolean>

    fun getInterestEnabledAssets(): Single<List<AssetInfo>>

    fun getInterestEligibilityForAsset(asset: AssetInfo): Single<Eligibility>

    fun startInterestWithdrawal(asset: AssetInfo, amount: Money, address: String): Completable

    fun getSupportedFundsFiats(fiatCurrency: String = defaultFiatCurrency): Single<List<String>>

    fun canTransactWithBankMethods(fiatCurrency: String): Single<Boolean>

    fun getExchangeSendAddressFor(asset: AssetInfo): Maybe<String>

    fun isSimplifiedDueDiligenceEligible(): Single<Boolean>

    fun fetchSimplifiedDueDiligenceUserState(): Single<SimplifiedDueDiligenceUserState>

    fun createCustodialOrder(
        direction: TransferDirection,
        quoteId: String,
        volume: Money,
        destinationAddress: String? = null,
        refundAddress: String? = null
    ): Single<CustodialOrder>

    fun createPendingDeposit(
        crypto: AssetInfo,
        address: String,
        hash: String,
        amount: Money,
        product: Product
    ): Completable

    fun getProductTransferLimits(
        currency: String,
        product: Product,
        orderDirection: TransferDirection? = null
    ): Single<TransferLimits>

    fun getSwapTrades(): Single<List<CustodialOrder>>

    fun getCustodialActivityForAsset(
        cryptoCurrency: AssetInfo,
        directions: Set<TransferDirection>
    ): Single<List<TradeTransactionItem>>

    fun updateOrder(
        id: String,
        success: Boolean
    ): Completable

    fun getLinkedBank(
        id: String
    ): Single<LinkedBank>

    fun getBanks(): Single<List<Bank>>

    fun isFiatCurrencySupported(destination: String): Boolean

    fun startBankTransfer(id: String, amount: Money, currency: String, callback: String? = null): Single<String>

    fun getBankTransferCharge(paymentId: String): Single<BankTransferDetails>

    fun executeCustodialTransfer(amount: Money, origin: Product, destination: Product): Completable

    fun updateOpenBankingConsent(url: String, token: String): Completable

    val defaultFiatCurrency: String

    fun getRecurringBuysForAsset(asset: AssetInfo): Single<List<RecurringBuy>>

    fun getRecurringBuyForId(recurringBuyId: String): Single<RecurringBuy>

    fun cancelRecurringBuy(recurringBuyId: String): Completable
}

data class InterestActivityItem(
    val value: CryptoValue,
    val cryptoCurrency: AssetInfo,
    val id: String,
    val insertedAt: Date,
    val state: InterestState,
    val type: TransactionSummary.TransactionType,
    val extraAttributes: InterestAttributes?
) {
    companion object {
        fun toInterestState(state: String): InterestState =
            when (state) {
                InterestActivityItemResponse.FAILED -> InterestState.FAILED
                InterestActivityItemResponse.REJECTED -> InterestState.REJECTED
                InterestActivityItemResponse.PROCESSING -> InterestState.PROCESSING
                InterestActivityItemResponse.CREATED,
                InterestActivityItemResponse.COMPLETE -> InterestState.COMPLETE
                InterestActivityItemResponse.PENDING -> InterestState.PENDING
                InterestActivityItemResponse.MANUAL_REVIEW -> InterestState.MANUAL_REVIEW
                InterestActivityItemResponse.CLEARED -> InterestState.CLEARED
                InterestActivityItemResponse.REFUNDED -> InterestState.REFUNDED
                else -> InterestState.UNKNOWN
            }

        fun toTransactionType(type: String) =
            when (type) {
                InterestActivityItemResponse.DEPOSIT -> TransactionSummary.TransactionType.DEPOSIT
                InterestActivityItemResponse.WITHDRAWAL -> TransactionSummary.TransactionType.WITHDRAW
                InterestActivityItemResponse.INTEREST_OUTGOING -> TransactionSummary.TransactionType.INTEREST_EARNED
                else -> TransactionSummary.TransactionType.UNKNOWN
            }
    }
}

enum class InterestState {
    FAILED,
    REJECTED,
    PROCESSING,
    COMPLETE,
    PENDING,
    MANUAL_REVIEW,
    CLEARED,
    REFUNDED,
    UNKNOWN
}

data class InterestAccountDetails(
    val balance: CryptoValue,
    val pendingInterest: CryptoValue,
    val pendingDeposit: CryptoValue,
    val totalInterest: CryptoValue,
    val lockedBalance: CryptoValue
)

data class PaymentAttributes(
    val authorisationUrl: String?,
    val status: String?,
    val cardAttributes: CardAttributes = CardAttributes.Empty
) {
    val isCardPayment: Boolean by lazy {
        cardAttributes != CardAttributes.Empty
    }
}

sealed class CardAttributes {

    object Empty : CardAttributes()

    // Very similar to CardProvider, used for BUY
    data class Provider(
        val cardAcquirerName: String,
        val cardAcquirerAccountCode: String,
        val paymentLink: String,
        val paymentState: String,
        val clientSecret: String,
        val publishableApiKey: String
    ) : CardAttributes()

    data class EveryPay(
        val paymentLink: String,
        val paymentState: String
    ) : CardAttributes() {
        companion object {
            const val WAITING_3DS = "WAITING_FOR_3DS_RESPONSE"
        }
    }
}

data class BuySellOrder(
    val id: String,
    val pair: String,
    val fiat: FiatValue,
    val crypto: CryptoValue,
    val paymentMethodId: String,
    val paymentMethodType: PaymentMethodType,
    val state: OrderState = OrderState.UNINITIALISED,
    val expires: Date = Date(),
    val updated: Date = Date(),
    val created: Date = Date(),
    val fee: FiatValue? = null,
    val price: FiatValue? = null,
    val orderValue: Money? = null,
    val attributes: PaymentAttributes? = null,
    val type: OrderType,
    val depositPaymentId: String,
    val approvalErrorStatus: ApprovalErrorStatus = ApprovalErrorStatus.NONE,
    val failureReason: RecurringBuyFailureReason? = null,
    val recurringBuyId: String? = null
)

enum class ApprovalErrorStatus {
    FAILED,
    REJECTED,
    DECLINED,
    EXPIRED,
    UNKNOWN,
    NONE
}

typealias BuyOrderList = List<BuySellOrder>

data class OrderInput(private val symbol: String, private val amount: String? = null)

data class OrderOutput(private val symbol: String, private val amount: String? = null)

data class Bank(
    val id: String,
    val name: String,
    val account: String,
    val state: BankState,
    val currency: String,
    val accountType: String,
    val paymentMethodType: PaymentMethodType,
    val iconUrl: String,
    val canBeUsedToTransact: Boolean = true
) : Serializable {

    @SuppressLint("DefaultLocale") // Yes, lint is broken
    fun toHumanReadableAccount(): String {
        return accountType.toLowerCase(Locale.getDefault()).capitalize(Locale.getDefault())
    }
}

enum class BankState {
    PENDING,
    BLOCKED,
    ACTIVE,
    UNKNOWN
}

data class FiatTransaction(
    val amount: FiatValue,
    val id: String,
    val date: Date,
    val type: TransactionType,
    val state: TransactionState,
    val paymentId: String?
)

data class CryptoTransaction(
    val amount: Money,
    val id: String,
    val date: Date,
    val type: TransactionType,
    val state: TransactionState,
    val receivingAddress: String,
    val fee: Money,
    val txHash: String,
    val currency: String,
    val paymentId: String?
)

enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL
}

enum class TransactionState {
    COMPLETED,
    PENDING,
    FAILED
}

enum class RecurringBuyFailureReason {
    INSUFFICIENT_FUNDS,
    BLOCKED_BENEFICIARY_ID,
    INTERNAL_SERVER_ERROR,
    FAILED_BAD_FILL,
    UNKNOWN
}

enum class CustodialOrderState {
    CREATED,
    PENDING_CONFIRMATION,
    PENDING_LEDGER,
    CANCELED,
    PENDING_EXECUTION,
    PENDING_DEPOSIT,
    FINISH_DEPOSIT,
    PENDING_WITHDRAWAL,
    EXPIRED,
    FINISHED,
    FAILED,
    UNKNOWN;

    private val pendingState: Set<CustodialOrderState>
        get() = setOf(
            PENDING_EXECUTION,
            PENDING_CONFIRMATION,
            PENDING_LEDGER,
            PENDING_DEPOSIT,
            PENDING_WITHDRAWAL,
            FINISH_DEPOSIT
        )

    val isPending: Boolean
        get() = pendingState.contains(this)

    private val failedState: Set<CustodialOrderState>
        get() = setOf(FAILED)

    val hasFailed: Boolean
        get() = failedState.contains(this)

    val displayableState: Boolean
        get() = isPending || this == FINISHED
}

data class BuySellPair(
    val cryptoCurrency: AssetInfo,
    val fiatCurrency: String,
    val buyLimits: BuySellLimits,
    val sellLimits: BuySellLimits
)

data class BuySellPairs(val pairs: List<BuySellPair>)

data class BuySellLimits(private val min: Long, private val max: Long) {
    fun minLimit(currency: String): FiatValue = FiatValue.fromMinor(currency, min)
    fun maxLimit(currency: String): FiatValue = FiatValue.fromMinor(currency, max)
}

enum class TransferDirection {
    ON_CHAIN, // from non-custodial to non-custodial
    FROM_USERKEY, // from non-custodial to custodial
    TO_USERKEY, // from custodial to non-custodial - not in use currently
    INTERNAL; // from custodial to custodial
}

data class BankAccount(val details: List<BankDetail>)

data class BankDetail(val title: String, val value: String, val isCopyable: Boolean = false)

data class PaymentCardAcquirer(
    val cardAcquirerName: String,
    val cardAcquirerAccountCodes: List<String>,
    val apiKey: String
)

internal fun String.toSupportedPartner(): Partner =
    when (this) {
        "EVERYPAY" -> Partner.EVERYPAY
        "CARDPROVIDER" -> Partner.CARDPROVIDER
        else -> Partner.UNKNOWN
    }

sealed class TransactionError : Throwable() {
    object OrderLimitReached : TransactionError()
    object OrderNotCancelable : TransactionError()
    object WithdrawalAlreadyPending : TransactionError()
    object WithdrawalBalanceLocked : TransactionError()
    object WithdrawalInsufficientFunds : TransactionError()
    object UnexpectedError : TransactionError()
    object InternalServerError : TransactionError()
    object AlbertExecutionError : TransactionError()
    object TradingTemporarilyDisabled : TransactionError()
    object InsufficientBalance : TransactionError()
    object OrderBelowMin : TransactionError()
    object OrderAboveMax : TransactionError()
    object SwapDailyLimitExceeded : TransactionError()
    object SwapWeeklyLimitExceeded : TransactionError()
    object SwapYearlyLimitExceeded : TransactionError()
    object InvalidCryptoAddress : TransactionError()
    object InvalidCryptoCurrency : TransactionError()
    object InvalidFiatCurrency : TransactionError()
    object OrderDirectionDisabled : TransactionError()
    object InvalidOrExpiredQuote : TransactionError()
    object IneligibleForSwap : TransactionError()
    object InvalidDestinationAmount : TransactionError()
    object InvalidPostcode : TransactionError()
    object ExecutionFailed : TransactionError()
}

sealed class PaymentMethod(
    val id: String,
    val type: PaymentMethodType,
    open val limits: PaymentLimits,
    val order: Int,
    open val isEligible: Boolean
) : Serializable {

    val availableBalance: Money?
        get() = (this as? Funds)?.balance

    data class UndefinedCard(
        override val limits: PaymentLimits,
        override val isEligible: Boolean
    ) : PaymentMethod(
        UNDEFINED_CARD_PAYMENT_ID, PaymentMethodType.PAYMENT_CARD, limits, UNDEFINED_CARD_PAYMENT_METHOD_ORDER,
        isEligible
    ),
        UndefinedPaymentMethod

    data class Funds(
        val balance: FiatValue,
        val fiatCurrency: String,
        override val limits: PaymentLimits,
        override val isEligible: Boolean
    ) : PaymentMethod(FUNDS_PAYMENT_ID, PaymentMethodType.FUNDS, limits, FUNDS_PAYMENT_METHOD_ORDER, isEligible)

    data class UndefinedBankAccount(
        val fiatCurrency: String,
        override val limits: PaymentLimits,
        override val isEligible: Boolean
    ) :
        PaymentMethod(
            UNDEFINED_BANK_ACCOUNT_ID, PaymentMethodType.FUNDS, limits, UNDEFINED_BANK_ACCOUNT_METHOD_ORDER, isEligible
        ),
        UndefinedPaymentMethod {
        val showAvailability: Boolean
            get() = currenciesRequiringAvailabilityLabel.contains(fiatCurrency)

        companion object {
            private val currenciesRequiringAvailabilityLabel = listOf("USD")
        }
    }

    data class UndefinedBankTransfer(
        override val limits: PaymentLimits,
        override val isEligible: Boolean
    ) :
        PaymentMethod(
            UNDEFINED_BANK_TRANSFER_PAYMENT_ID, PaymentMethodType.BANK_TRANSFER, limits,
            UNDEFINED_BANK_TRANSFER_METHOD_ORDER, isEligible
        ),
        UndefinedPaymentMethod

    data class Bank(
        val bankId: String,
        override val limits: PaymentLimits,
        val bankName: String,
        val accountEnding: String,
        val accountType: String,
        override val isEligible: Boolean,
        val iconUrl: String
    ) : PaymentMethod(bankId, PaymentMethodType.BANK_TRANSFER, limits, BANK_PAYMENT_METHOD_ORDER, isEligible),
        Serializable,
        RecurringBuyPaymentDetails {

        override fun detailedLabel() =
            "$bankName $accountEnding"

        override fun methodName() = bankName

        override fun methodDetails() = "$accountType $accountEnding"

        val uiAccountType: String =
            accountType.lowercase(Locale.getDefault()).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.getDefault()
                ) else it.toString()
            }

        override val paymentDetails: PaymentMethodType
            get() = PaymentMethodType.BANK_TRANSFER
    }

    data class Card(
        val cardId: String,
        override val limits: PaymentLimits,
        private val label: String,
        val endDigits: String,
        val partner: Partner,
        val expireDate: Date,
        val cardType: CardType,
        val status: CardStatus,
        override val isEligible: Boolean
    ) : PaymentMethod(cardId, PaymentMethodType.PAYMENT_CARD, limits, CARD_PAYMENT_METHOD_ORDER, isEligible),
        Serializable,
        RecurringBuyPaymentDetails {

        override fun detailedLabel() =
            "${uiLabel()} ${dottedEndDigits()}"

        override fun methodName() = label

        override fun methodDetails() = "${cardType.name} $endDigits"

        fun uiLabel() =
            label.takeIf { it.isNotEmpty() } ?: cardType.label()

        fun dottedEndDigits() =
            "•••• $endDigits"

        private fun CardType.label(): String =
            when (this) {
                CardType.VISA -> "Visa"
                CardType.MASTERCARD -> "Mastercard"
                CardType.AMEX -> "American Express"
                CardType.DINERS_CLUB -> "Diners Club"
                CardType.MAESTRO -> "Maestro"
                CardType.JCB -> "JCB"
                else -> ""
            }

        override val paymentDetails: PaymentMethodType
            get() = PaymentMethodType.PAYMENT_CARD
    }

    fun canUsedForPaying(): Boolean =
        this is Card || this is Funds || this is Bank

    fun canBeAdded(): Boolean =
        this is UndefinedBankTransfer || this is UndefinedBankAccount || this is UndefinedCard

    open fun detailedLabel(): String = ""

    open fun methodName(): String = ""

    open fun methodDetails(): String = ""

    companion object {
        const val UNDEFINED_CARD_PAYMENT_ID = "UNDEFINED_CARD_PAYMENT_ID"
        const val FUNDS_PAYMENT_ID = "FUNDS_PAYMENT_ID"
        const val UNDEFINED_BANK_ACCOUNT_ID = "UNDEFINED_BANK_ACCOUNT_ID"
        const val UNDEFINED_BANK_TRANSFER_PAYMENT_ID = "UNDEFINED_BANK_TRANSFER_PAYMENT_ID"

        private const val FUNDS_PAYMENT_METHOD_ORDER = 0
        private const val CARD_PAYMENT_METHOD_ORDER = 1
        private const val BANK_PAYMENT_METHOD_ORDER = 2
        private const val UNDEFINED_CARD_PAYMENT_METHOD_ORDER = 3
        private const val UNDEFINED_BANK_TRANSFER_METHOD_ORDER = 4
        private const val UNDEFINED_BANK_ACCOUNT_METHOD_ORDER = 5
    }
}

interface UndefinedPaymentMethod

data class PaymentLimits(override val min: FiatValue, override val max: FiatValue) : Serializable, LegacyLimits {
    constructor(min: Long, max: Long, currency: String) : this(
        FiatValue.fromMinor(currency, min),
        FiatValue.fromMinor(currency, max)
    )
}

enum class Product {
    BUY,
    SELL,
    SAVINGS,
    TRADE
}

data class BillingAddress(
    val countryCode: String,
    val fullName: String,
    val addressLine1: String,
    val addressLine2: String,
    val city: String,
    val postCode: String,
    val state: String?
)

data class CardToBeActivated(val partner: Partner, val cardId: String)

sealed class PartnerCredentials {
    object Unknown : PartnerCredentials()
    data class EverypayPartner(val everyPay: EveryPayCredentials) : PartnerCredentials()
    data class CardProviderPartner(val cardProvider: CardProvider) : PartnerCredentials()
}

data class EveryPayCredentials(
    val apiUsername: String,
    val mobileToken: String,
    val paymentLink: String
)

// Very similar to CardAttributes.Provider, used for card activation
data class CardProvider(
    val cardAcquirerName: String,
    val cardAcquirerAccountCode: String,
    val apiUserID: String,
    val apiToken: String,
    val paymentLink: String,
    val paymentState: String,
    val paymentReference: String,
    val orderReference: String,
    val clientSecret: String,
    val publishableApiKey: String
)

enum class Partner {
    EVERYPAY, // The old flow with EveryPay
    CARDPROVIDER, // The new flow for CardAcquirers, which can be Checkout, Stripe and EveryPay
    UNKNOWN
}

data class TransferQuote(
    val id: String = "",
    val prices: List<PriceTier> = emptyList(),
    val expirationDate: Date = Date(),
    val creationDate: Date = Date(),
    val networkFee: Money,
    val staticFee: Money,
    val sampleDepositAddress: String
)

sealed class CurrencyPair(val rawValue: String) {
    data class CryptoCurrencyPair(val source: AssetInfo, val destination: AssetInfo) :
        CurrencyPair("${source.networkTicker}-${destination.networkTicker}")

    data class CryptoToFiatCurrencyPair(val source: AssetInfo, val destination: String) :
        CurrencyPair("${source.networkTicker}-$destination")

    data class FiatToCryptoCurrencyPair(val source: String, val destination: AssetInfo) :
        CurrencyPair("$source-${destination.networkTicker}")

    fun toSourceMoney(value: BigInteger): Money =
        when (this) {
            is CryptoCurrencyPair -> CryptoValue.fromMinor(source, value)
            is CryptoToFiatCurrencyPair -> CryptoValue.fromMinor(source, value)
            is FiatToCryptoCurrencyPair -> FiatValue.fromMinor(source, value.toLong())
        }

    fun toDestinationMoney(value: BigInteger): Money =
        when (this) {
            is CryptoCurrencyPair -> CryptoValue.fromMinor(destination, value)
            is CryptoToFiatCurrencyPair -> FiatValue.fromMinor(destination, value.toLong())
            is FiatToCryptoCurrencyPair -> CryptoValue.fromMinor(destination, value)
        }

    fun toDestinationMoney(value: BigDecimal): Money =
        when (this) {
            is CryptoCurrencyPair -> CryptoValue.fromMajor(destination, value)
            is CryptoToFiatCurrencyPair -> FiatValue.fromMajor(destination, value)
            is FiatToCryptoCurrencyPair -> CryptoValue.fromMinor(destination, value)
        }

    fun sourceCurrencyCode(): String = when (this) {
        is CryptoCurrencyPair -> source.networkTicker
        is CryptoToFiatCurrencyPair -> source.networkTicker
        is FiatToCryptoCurrencyPair -> source
    }

    fun destinationCurrencyCode(): String = when (this) {
        is CryptoCurrencyPair -> destination.networkTicker
        is CryptoToFiatCurrencyPair -> destination
        is FiatToCryptoCurrencyPair -> destination.networkTicker
    }

    companion object {
        fun fromRawPair(
            rawValue: String,
            assetCatalogue: AssetCatalogue,
            supportedFiatCurrencies: List<String> = LiveCustodialWalletManager.SUPPORTED_FUNDS_CURRENCIES
        ): CurrencyPair? {
            val parts = rawValue.split("-")
            val source: AssetInfo = assetCatalogue.fromNetworkTicker(parts[0]) ?: return null
            val destinationCryptoCurrency: AssetInfo? = assetCatalogue.fromNetworkTicker(parts[1])
            if (destinationCryptoCurrency != null)
                return CryptoCurrencyPair(source, destinationCryptoCurrency)
            if (supportedFiatCurrencies.contains(parts[1]))
                return CryptoToFiatCurrencyPair(source, parts[1])
            return null
        }
    }
}

data class PriceTier(
    val volume: Money,
    val price: Money
)

data class TransferLimits(
    val minLimit: FiatValue,
    val maxOrder: FiatValue,
    val maxLimit: FiatValue
) : LegacyLimits {
    constructor(currency: String) : this(
        minLimit = FiatValue.zero(currency),
        maxOrder = FiatValue.zero(currency),
        maxLimit = FiatValue.zero(currency)
    )

    override val min: Money
        get() = minLimit
    override val max: Money
        get() = maxLimit
}

data class CustodialOrder(
    val id: String,
    val state: CustodialOrderState,
    val depositAddress: String?,
    val createdAt: Date,
    val inputMoney: Money,
    val outputMoney: Money
)

data class EligiblePaymentMethodType(
    val paymentMethodType: PaymentMethodType,
    val currency: String
)

data class SimplifiedDueDiligenceUserState(
    val isVerified: Boolean,
    val stateFinalised: Boolean
)

data class RecurringBuyOrder(
    val state: RecurringBuyState = RecurringBuyState.UNINITIALISED
)
