package piuk.blockchain.android.ui.thepit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.koin.scopedInject
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityPitKycPromoLayoutBinding
import piuk.blockchain.android.databinding.ToolbarGeneralBinding
import piuk.blockchain.android.thepit.PitAnalyticsEvent
import piuk.blockchain.android.ui.base.BaseMvpActivity
import piuk.blockchain.android.ui.customviews.ErrorBottomDialog
import piuk.blockchain.android.urllinks.URL_THE_PIT_LANDING_LEARN_MORE
import piuk.blockchain.android.util.launchUrlInBrowser
import piuk.blockchain.android.util.throttledClicks

class PitPermissionsActivity : PitPermissionsView, BaseMvpActivity<PitPermissionsView, PitPermissionsPresenter>() {

    override fun createPresenter(): PitPermissionsPresenter = pitPermissionsPresenter
    override fun getView(): PitPermissionsView = this
    private val pitPermissionsPresenter: PitPermissionsPresenter by scopedInject()
    private var loadingDialog: PitStateBottomDialog? = null

    private val compositeDisposable = CompositeDisposable()

    private val binding: ActivityPitKycPromoLayoutBinding by lazy {
        ActivityPitKycPromoLayoutBinding.inflate(layoutInflater)
    }

    override fun promptForEmailVerification(email: String) {
        PitVerifyEmailActivity.start(this, email, REQUEST_VERIFY_EMAIL)
    }

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar(
            toolbarTitle = getString(R.string.the_exchange_title),
            backAction = { onBackPressed() }
        )
        binding.connectNow.setOnClickListener {
            doLinkClickHandler()
        }

        compositeDisposable += binding.learnMore.throttledClicks()
            .subscribeBy(
                onNext = {
                    analytics.logEvent(PitAnalyticsEvent.LearnMoreEvent)
                    launchUrlInBrowser(
                        URL_THE_PIT_LANDING_LEARN_MORE +
                            "/?utm_source=android_wallet" +
                            "&utm_medium=wallet_linking"
                    )
                }
            )

        onViewReady()
    }

    private fun doLinkClickHandler() {
        analytics.logEvent(PitAnalyticsEvent.ConnectNowEvent)
        if (intent.isPitToWalletLink) {
            val linkId = intent.pitToWalletLinkId ?: throw IllegalStateException("Link id is missing")
            presenter?.tryToConnectPitToWallet(linkId)
        } else {
            presenter?.tryToConnectWalletToPit()
        }
    }

    override fun onLinkSuccess(pitLinkingUrl: String) {
        launchUrlInBrowser(pitLinkingUrl)
    }

    override fun onLinkFailed(reason: String) {
        PitStateBottomDialog.newInstance(
            PitStateBottomDialog.StateContent(
                ErrorBottomDialog.Content(
                    title = getString(R.string.the_exchange_connection_error_title),
                    description = getString(R.string.the_exchange_connection_error_description),
                    ctaButtonText = R.string.common_try_again,
                    dismissText = 0,
                    icon = R.drawable.vector_pit_request_failure
                ),
                false
            )
        ).apply {
            onCtaClick = {
                doLinkClickHandler()
                dismiss()
            }
        }.apply {
            show(supportFragmentManager, "LoadingBottomDialog")
        }
    }

    override fun onPitLinked() {
        PitStateBottomDialog.newInstance(
            PitStateBottomDialog.StateContent(
                ErrorBottomDialog.Content(
                    title = getString(R.string.the_exchange_connection_success_title),
                    description = getString(R.string.the_exchange_connection_success_description),
                    ctaButtonText = R.string.btn_close,
                    dismissText = 0,
                    icon = R.drawable.vector_pit_request_ok
                ),
                false
            )
        ).apply {
            onCtaClick = {
                dismiss()
            }
        }.apply {
            show(supportFragmentManager, "SuccessBottomDialog")
        }
    }

    override fun showLoading() {
        if (loadingDialog == null) {
            loadingDialog = PitStateBottomDialog.newInstance(
                PitStateBottomDialog.StateContent(
                    ErrorBottomDialog.Content(
                        title = getString(R.string.the_exchange_loading_dialog_title),
                        description = getString(R.string.the_exchange_loading_dialog_description),
                        ctaButtonText = 0,
                        dismissText = 0,
                        icon = 0
                    ),
                    true
                )
            )
            loadingDialog?.show(supportFragmentManager, "LoadingBottomDialog")
        }
    }

    override fun hideLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_VERIFY_EMAIL) {
            presenter?.checkEmailIsVerified()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun showEmailVerifiedDialog() {
        val emailVerifiedBottomDialog =
            PitEmailVerifiedBottomDialog.newInstance(
                ErrorBottomDialog.Content(
                    title = getString(R.string.the_exchange_email_verified_title),
                    description = getString(R.string.the_exchange_email_verified_description),
                    ctaButtonText = R.string.the_exchange_connect_now,
                    dismissText = 0,
                    icon = R.drawable.vector_email_verified
                )
            ).apply {
                onCtaClick = { doLinkClickHandler() }
            }
        emailVerifiedBottomDialog.show(supportFragmentManager, "BottomDialog")
    }

    companion object {
        private const val REQUEST_VERIFY_EMAIL = 7396
        private const val PARAM_LINK_WALLET_TO_PIT = "link_wallet_to_pit"
        private const val PARAM_LINK_ID = "link_id"

        @JvmStatic
        fun start(ctx: Context, linkId: String? = null) {
            Intent(ctx, PitPermissionsActivity::class.java).apply {
                isPitToWalletLink = linkId.isNullOrEmpty().not()
                pitToWalletLinkId = linkId
            }.run { ctx.startActivity(this) }
        }

        private var Intent.isPitToWalletLink: Boolean
            get() = extras?.getBoolean(PARAM_LINK_WALLET_TO_PIT, true) ?: true
            set(v) {
                putExtra(PARAM_LINK_WALLET_TO_PIT, v)
            }

        private var Intent.pitToWalletLinkId: String?
            get() = extras?.getString(PARAM_LINK_ID, null)
            set(v) {
                putExtra(PARAM_LINK_ID, v)
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        super.onBackPressed()
        pitPermissionsPresenter.clearLinkPrefs()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}
