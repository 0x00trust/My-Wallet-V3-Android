package piuk.blockchain.android.ui.transactionflow.flow.customisations

import android.content.Context
import android.widget.FrameLayout
import com.blockchain.coincore.AssetAction
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.plugin.ConfirmSheetWidget

interface TransactionConfirmationCustomisations {
    fun confirmTitle(state: TransactionState): String
    fun confirmCtaText(state: TransactionState): String
    fun cancelButtonText(action: AssetAction): String
    fun cancelButtonVisible(action: AssetAction): Boolean
    fun confirmListItemTitle(assetAction: AssetAction): String
    fun confirmDisclaimerBlurb(state: TransactionState, context: Context): CharSequence
    fun confirmDisclaimerVisibility(assetAction: AssetAction): Boolean
    fun amountHeaderConfirmationVisible(state: TransactionState): Boolean
    fun confirmInstallHeaderView(
        ctx: Context,
        frame: FrameLayout,
        state: TransactionState
    ): ConfirmSheetWidget
}
