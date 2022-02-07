package piuk.blockchain.android.ui.dashboard.assetdetails

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.DashboardPrefs
import piuk.blockchain.android.databinding.DialogCustodialIntroBinding
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics

class CustodyWalletIntroSheet : SlidingModalBottomDialog<DialogCustodialIntroBinding>() {

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogCustodialIntroBinding =
        DialogCustodialIntroBinding.inflate(inflater, container, false)

    private val dashboardPrefs: DashboardPrefs by scopedInject()
    private val model: AssetDetailsModel by scopedInject()

    override fun initControls(binding: DialogCustodialIntroBinding) {
        analytics.logEvent(SimpleBuyAnalytics.CUSTODY_WALLET_CARD_SHOWN)
        binding.ctaButton.setOnClickListener { onCtaClick() }
    }

    override fun onSheetHidden() {
        super.onSheetHidden()
        dialog?.let {
            onCancel(it)
        }
        model.process(ShowAssetDetailsIntent)
    }

    private fun onCtaClick() {
        dashboardPrefs.isCustodialIntroSeen = true
        analytics.logEvent(SimpleBuyAnalytics.CUSTODY_WALLET_CARD_CLICKED)
        model.process(CustodialSheetFinished)
    }

    companion object {
        fun newInstance(): CustodyWalletIntroSheet = CustodyWalletIntroSheet()
    }
}
