package piuk.blockchain.android.coincore.eth

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.ethereum.EthereumAccount
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SendTransaction
import piuk.blockchain.android.coincore.impl.CryptoSingleAccountNonCustodialBase
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

internal class EthCryptoWalletAccount(
    override val label: String,
    private val address: String,
    private val ethDataManager: EthDataManager,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoSingleAccountNonCustodialBase(CryptoCurrency.ETHER) {

    constructor(
        ethDataManager: EthDataManager,
        jsonAccount: EthereumAccount,
        exchangeRates: ExchangeRateDataManager
    ) : this(
        jsonAccount.label,
        jsonAccount.address,
        ethDataManager,
        exchangeRates
    )

    override val balance: Single<CryptoValue>
        get() = ethDataManager.fetchEthAddress()
            .singleOrError()
            .map { CryptoValue(CryptoCurrency.ETHER, it.getTotalBalance()) }

    override val receiveAddress: Single<String>
        get() = Single.just(address)

    override val activity: Single<ActivitySummaryList>
        get() = ethDataManager.getLatestBlockNumber()
            .flatMap { latestBlock ->
                ethDataManager.getEthTransactions()
                    .map { list ->
                        list.map { transaction ->
                            val ethFeeForPaxTransaction = transaction.to.equals(
                                ethDataManager.getErc20TokenData(CryptoCurrency.PAX).contractAddress,
                                ignoreCase = true
                            )
                            EthActivitySummaryItem(
                                ethDataManager,
                                transaction,
                                ethFeeForPaxTransaction,
                                latestBlock.number.toLong(),
                                exchangeRates,
                                account = this
                            ) as ActivitySummaryItem
                        }
                    }
            }
            .doOnSuccess { setHasTransactions(it.isNotEmpty()) }

    override val isDefault: Boolean = true // Only one ETH account, so always default

    override fun createPendingSend(address: ReceiveAddress): Single<SendTransaction> =
        // Check type of Address here, and create Custodial or Swap or Sell or
        // however this is going to work.
        //
        // For now, while I prototype this, just make the eth -> on chain eth object

        balance.map { balance ->
            EthSendTransaction(
                ethDataManager,
                this,
                address as CryptoAddress,
                balance,
                ethDataManager.requireSecondPassword
            )
        }

}
