package piuk.blockchain.android.ui.start

import androidx.annotation.StringRes
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import io.reactivex.rxjava3.kotlin.plusAssign
import javax.net.ssl.SSLPeerUnverifiedException
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.MvpPresenter
import piuk.blockchain.android.ui.base.MvpView
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs

interface LoginView : MvpView {
    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String)
    fun startPinEntryActivity()
}

class LoginPresenter(
    private val appUtil: AppUtil,
    private val _payloadDataManager: Lazy<PayloadDataManager>,
    private val prefs: PersistentPrefs,
    private val analytics: Analytics
) : MvpPresenter<LoginView>() {

    override fun onViewAttached() { /* no-op */
    }

    override fun onViewDetached() { /* no-op */
    }

    override val alwaysDisableScreenshots: Boolean = false
    override val enableLogoutTimer: Boolean = false

    internal fun pairWithQR(raw: String?) {

        if (raw == null) view?.showToast(R.string.pairing_failed, ToastCustom.TYPE_ERROR)

        val dataManager = _payloadDataManager.value

        compositeDisposable += dataManager.handleQrCode(raw!!)
            .doOnSubscribe { view?.showProgressDialog(R.string.please_wait) }
            .doOnComplete { prefs.sharedKey = dataManager.wallet!!.sharedKey }
            .doAfterTerminate { view?.dismissProgressDialog() }
            .subscribe({
                prefs.apply {
                    walletGuid = dataManager.wallet!!.guid
                    emailVerified = true
                }
                view?.startPinEntryActivity()

                analytics.logEvent(AnalyticsEvents.WalletAutoPairing)
            }, { throwable ->
                if (throwable is SSLPeerUnverifiedException) {
                    // BaseActivity handles message
                    appUtil.clearCredentials()
                } else {
                    view?.showToast(R.string.pairing_failed, ToastCustom.TYPE_ERROR)
                    appUtil.clearCredentialsAndRestart()
                }
            })
    }
}
