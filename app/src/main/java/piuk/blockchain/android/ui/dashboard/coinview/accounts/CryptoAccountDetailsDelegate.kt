package piuk.blockchain.android.ui.dashboard.coinview.accounts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.text.buildAnnotatedString
import androidx.recyclerview.widget.RecyclerView
import com.blockchain.coincore.AccountGroup
import com.blockchain.coincore.AssetFilter
import com.blockchain.coincore.BlockchainAccount
import com.blockchain.coincore.CryptoAccount
import com.blockchain.coincore.selectFirstAccount
import com.blockchain.componentlib.basic.ImageResource
import com.blockchain.componentlib.viewextensions.gone
import com.blockchain.componentlib.viewextensions.visible
import com.blockchain.componentlib.viewextensions.visibleIf
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.Currency
import io.reactivex.rxjava3.disposables.CompositeDisposable
import java.text.DecimalFormat
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ViewCoinviewWalletsBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.customviews.account.CellDecorator
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsItem
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsItemNew
import piuk.blockchain.android.ui.resources.AccountIcon
import piuk.blockchain.android.ui.resources.AssetResources

class CryptoAccountDetailsDelegate(
    private val onAccountSelected: (BlockchainAccount, AssetFilter) -> Unit,
    private val disposable: CompositeDisposable,
    private val block: AssetDetailsInfoDecorator,
    private val labels: DefaultLabels,
    private val assetResources: AssetResources
) : AdapterDelegate<AssetDetailsItemNew> {
    override fun isForViewType(items: List<AssetDetailsItemNew>, position: Int): Boolean =
        items[position] is AssetDetailsItemNew.CryptoDetailsInfo

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AssetWalletViewHolder(
            ViewCoinviewWalletsBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            onAccountSelected,
            disposable,
            block,
            labels,
            assetResources
        )

    override fun onBindViewHolder(
        items: List<AssetDetailsItemNew>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as AssetWalletViewHolder).bind(
        items[position] as AssetDetailsItemNew.CryptoDetailsInfo,
        items.indexOfFirst { it is AssetDetailsItemNew.CryptoDetailsInfo } == position
    )
}

private class AssetWalletViewHolder(
    private val binding: ViewCoinviewWalletsBinding,
    private val onAccountSelected: (BlockchainAccount, AssetFilter) -> Unit,
    private val disposable: CompositeDisposable,
    private val block: AssetDetailsInfoDecorator,
    private val labels: DefaultLabels,
    private val assetResources: AssetResources
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        item: AssetDetailsItemNew.CryptoDetailsInfo,
        isFirstItemOfCategory: Boolean
    ) {
        val asset = getAsset(item.account, item.balance.currencyCode)

        with(binding) {
            val walletLabel = when (item.assetFilter) {
                AssetFilter.NonCustodial -> item.account.label
                AssetFilter.Custodial -> labels.getDefaultCustodialWalletLabel()
                AssetFilter.Interest -> labels.getDefaultInterestWalletLabel()
                else -> throw IllegalArgumentException("Filer not supported for account label")
            }
            val account = when (item.account) {
                is CryptoAccount -> item.account
                is AccountGroup -> item.account.selectFirstAccount()
                else -> throw IllegalStateException(
                    "Unsupported account type for asset details ${item.account}"
                )
            }
            val accountIcon = AccountIcon(account, assetResources)

            if (item.actions.isNotEmpty()) {
                assetDetailsNotAvailable.gone()
                assetDetailsAvailable.apply {
                    visible()
                    titleStart = buildAnnotatedString { append(walletLabel) }
                    titleEnd = buildAnnotatedString { append(item.fiatBalance.toStringWithSymbol()) }
                    bodyEnd = buildAnnotatedString { append(item.balance.toStringWithSymbol()) }
                    bodyStart = buildAnnotatedString {
                        append(
                            when (item.assetFilter) {
                                AssetFilter.NonCustodial -> context.getString(R.string.coinview_nc_desc)
                                AssetFilter.Custodial -> context.getString(R.string.coinview_c_available_desc)
                                AssetFilter.Interest -> {
                                    val interestFormat = DecimalFormat("0.#")
                                    val interestRate = interestFormat.format(item.interestRate)
                                    binding.root.context.getString(
                                        R.string.coinview_interest_with_balance, interestRate
                                    )
                                }
                                else -> throw IllegalArgumentException("Not a supported filter")
                            }
                        )
                    }
                    accountIcon.indicator?.let {
                        startImageResource =
                            ImageResource.LocalWithBackgroundAndExternalResources(it, asset.colour, "#FFFFFF", 1F)
                    }
                }
            } else {
                assetDetailsAvailable.gone()
                assetDetailsNotAvailable.apply {
                    visible()
                    isClickable = false
                    primaryText = walletLabel
                    secondaryText = when (item.assetFilter) {
                        AssetFilter.NonCustodial -> context.getString(R.string.coinview_nc_desc)
                        AssetFilter.Custodial -> context.getString(R.string.coinview_c_unavailable_desc, asset.name)
                        AssetFilter.Interest -> {
                            val interestFormat = DecimalFormat("0.#")
                            val interestRate = interestFormat.format(item.interestRate)
                            context.getString(
                                R.string.coinview_interest_no_balance, interestRate
                            )
                        }
                        else -> throw IllegalArgumentException("Not a supported filter")
                    }
                    accountIcon.indicator?.let {
                        startImageResource =
                            ImageResource.LocalWithBackground(it, R.color.grey_400, R.color.white, 1F)
                    }
                    endImageResource =
                        ImageResource.LocalWithBackground(R.drawable.ic_lock, R.color.grey_400, R.color.white, 1F)
                }
            }

            walletsLabel.apply {
                visibleIf { isFirstItemOfCategory }
                title = "Wallets & Accounts"
            }

            root.setOnClickListener {
                onAccountSelected(item.account, item.assetFilter)
            }

            //            walletBalanceFiat.text = item.balance.toStringWithSymbol()
            //            walletBalanceCrypto.text = item.fiatBalance.toStringWithSymbol()
            //            disposable += block(item).view(root.context)
            //                .observeOn(AndroidSchedulers.mainThread())
            //                .subscribe {
            //                    container.addViewToBottomWithConstraints(
            //                        view = it,
            //                        bottomOfView = assetSubtitle,
            //                        startOfView = assetSubtitle,
            //                        endOfView = walletBalanceCrypto
            //                    )
            //                }
        }
    }

    private fun getAsset(account: BlockchainAccount, currency: String): Currency =
        when (account) {
            is CryptoAccount -> account.currency
            is AccountGroup -> account.accounts.filterIsInstance<CryptoAccount>()
                .firstOrNull()?.currency ?: throw IllegalStateException(
                "No crypto accounts found in ${this::class.java} with currency $currency "
            )
            else -> null
        } ?: throw IllegalStateException("Unsupported account type ${this::class.java}")
}
typealias AssetDetailsInfoDecorator = (AssetDetailsItem.CryptoDetailsInfo) -> CellDecorator
