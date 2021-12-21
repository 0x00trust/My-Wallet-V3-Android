package piuk.blockchain.android.ui.resources

import android.content.res.Resources
import android.graphics.Color
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import info.blockchain.balance.AssetInfo
import info.blockchain.balance.Currency
import info.blockchain.balance.FiatCurrency
import piuk.blockchain.android.R

interface AssetResources {

    @ColorInt
    fun assetColor(asset: Currency): Int

    fun loadAssetIcon(imageView: ImageView, asset: Currency)

    fun makeBlockExplorerUrl(asset: AssetInfo, transactionHash: String): String

    @DrawableRes
    fun fiatCurrencyIcon(currency: FiatCurrency): Int
}

internal class AssetResourcesImpl(val resources: Resources) : AssetResources {

    override fun assetColor(asset: Currency): Int =
        Color.parseColor(asset.colour)

    override fun loadAssetIcon(imageView: ImageView, asset: Currency) {
        Glide.with(imageView.context)
            .load(asset.logo)
            .circleCrop()
            .apply(RequestOptions().placeholder(R.drawable.ic_default_asset_logo))
            .into(imageView)
    }

    override fun makeBlockExplorerUrl(asset: AssetInfo, transactionHash: String): String =
        asset.txExplorerUrlBase?.let {
            "$it$transactionHash"
        } ?: ""

    override fun fiatCurrencyIcon(currency: FiatCurrency): Int =
        when (currency.networkTicker) {
            "EUR" -> R.drawable.ic_funds_euro
            "GBP" -> R.drawable.ic_funds_gbp
            else -> R.drawable.ic_funds_usd
        }
}
