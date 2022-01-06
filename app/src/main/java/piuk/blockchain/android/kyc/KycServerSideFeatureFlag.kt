package piuk.blockchain.android.kyc

import com.blockchain.remoteconfig.FeatureFlag
import info.blockchain.wallet.api.WalletApi
import io.reactivex.rxjava3.core.Single

class KycServerSideFeatureFlag(private val walletApi: WalletApi) : FeatureFlag {
    override val key: String = "android_ff_kyc_server_side"
    override val readableName: String = "Kyc Server Side"
    override val enabled: Single<Boolean>
        get() =
            walletApi
                .walletOptions
                .singleOrError()
                .map {
                    it.androidFlags["homebrew"] ?: false
                }
}
