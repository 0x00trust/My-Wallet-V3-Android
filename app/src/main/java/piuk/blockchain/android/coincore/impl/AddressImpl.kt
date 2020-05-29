package piuk.blockchain.android.coincore.impl

import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.android.coincore.CryptoAddress

internal class ExchangeAddress(
    asset: CryptoCurrency,
    address: String,
    labels: DefaultLabels
) : CryptoAddress(asset, address) {
    override val label = labels.getDefaultExchangeWalletLabel(asset)
}

//internal class CustodialAddress(
//    asset: CryptoCurrency,
//    address: String,
//    labels: DefaultLabels
//) : CryptoAddress(asset, address) {
//    override val label = labels.getDefaultCustodialWalletLabel(asset)
//}

internal class EnteredAddress(
    asset: CryptoCurrency,
    address: String
) : CryptoAddress(asset, address) {
    override val label = address
}

internal class BitpayAddress
internal class WalletAddress
internal class ExternalAddress
