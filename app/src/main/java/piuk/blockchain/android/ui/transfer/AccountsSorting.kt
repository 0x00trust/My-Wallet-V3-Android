package piuk.blockchain.android.ui.transfer

import com.blockchain.coincore.AccountsSorter
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.NonCustodialAccount
import com.blockchain.preferences.DashboardPrefs
import info.blockchain.balance.AssetCatalogue
import info.blockchain.balance.AssetInfo
import io.reactivex.rxjava3.core.Single

interface AccountsSorting {
    fun sorter(): AccountsSorter
}

class DashboardAccountsSorting(
    private val dashboardPrefs: DashboardPrefs,
    private val assetCatalogue: AssetCatalogue
) : AccountsSorting {

    override fun sorter(): AccountsSorter = { list ->
        Single.fromCallable { getOrdering() }
            .map { orderedAssets ->
                val sortedList = list.sortedWith(
                    compareBy(
                        {
                            (it as? CryptoAccount)?.let { cryptoAccount ->
                                orderedAssets.indexOf(cryptoAccount.currency)
                            } ?: 0
                        },
                        { it !is NonCustodialAccount },
                        { !it.isDefault }
                    )
                )
                sortedList
            }
    }

    private fun getOrdering(): List<AssetInfo> =
        dashboardPrefs.dashboardAssetOrder
            .takeIf { it.isNotEmpty() }?.let {
                it.mapNotNull { ticker -> assetCatalogue.assetInfoFromNetworkTicker(ticker) }
            } ?: assetCatalogue.supportedCryptoAssets.sortedBy { it.displayTicker }
}
