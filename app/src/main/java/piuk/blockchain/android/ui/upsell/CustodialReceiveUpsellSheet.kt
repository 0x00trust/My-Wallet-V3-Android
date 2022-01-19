package piuk.blockchain.android.ui.upsell

import android.view.LayoutInflater
import android.view.ViewGroup
import com.blockchain.commonarch.presentation.base.HostedBottomSheet
import com.blockchain.commonarch.presentation.base.SlidingModalBottomDialog
import piuk.blockchain.android.databinding.DialogSheetUpsellCustodialReceiveBinding

interface UpsellHost : HostedBottomSheet.Host {
    fun startUpsellKyc()
}

internal /*package*/ class CustodialReceiveUpsellSheet :
    SlidingModalBottomDialog<DialogSheetUpsellCustodialReceiveBinding>() {
    override val host: UpsellHost by lazy {
        super.host as? UpsellHost ?: throw IllegalStateException(
            "Host fragment is not a CustodialReceiveUpsellSheet.Host"
        )
    }

    override fun initBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogSheetUpsellCustodialReceiveBinding =
        DialogSheetUpsellCustodialReceiveBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogSheetUpsellCustodialReceiveBinding) {
        binding.verifyNowCta.setOnClickListener {
            host.startUpsellKyc()
            dismiss()
        }
    }
}
