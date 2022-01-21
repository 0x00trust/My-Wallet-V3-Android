package piuk.blockchain.android.ui.start

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.blockchain.commonarch.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.WalletStatus
import org.json.JSONObject
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityPasswordRequiredBinding
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.getTwoFactorDialog
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.android.ui.login.auth.LoginAuthState.Companion.TWO_FA_COUNTDOWN
import piuk.blockchain.android.ui.login.auth.LoginAuthState.Companion.TWO_FA_STEP
import piuk.blockchain.android.ui.recover.RecoverFundsActivity

class PasswordRequiredActivity :
    MvpActivity<PasswordRequiredView, PasswordRequiredPresenter>(),
    PasswordRequiredView {
    private val binding: ActivityPasswordRequiredBinding by lazy {
        ActivityPasswordRequiredBinding.inflate(layoutInflater)
    }

    override val presenter: PasswordRequiredPresenter by scopedInject()
    override val view: PasswordRequiredView = this
    private val walletPrefs: WalletStatus by inject()

    private var isTwoFATimerRunning = false
    private val twoFATimer by lazy {
        object : CountDownTimer(TWO_FA_COUNTDOWN, TWO_FA_STEP) {
            override fun onTick(millisUntilFinished: Long) {
                isTwoFATimerRunning = true
            }

            override fun onFinish() {
                isTwoFATimerRunning = false
                walletPrefs.setResendSmsRetries(3)
            }
        }
    }

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar(
            toolbarTitle = getString(R.string.confirm_password),
            backAction = { onBackPressed() }
        )
        with(binding) {
            buttonContinue.apply {
                onClick = {
                    presenter.onContinueClicked(binding.fieldPassword.text.toString())
                }
                text = getString(R.string.btn_continue)
            }
            buttonForget.apply {
                onClick = {
                    presenter.onForgetWalletClicked()
                }
                text = getString(R.string.wipe_wallet)
            }
            buttonRecover.setOnClickListener { launchRecoveryFlow() }
        }
    }

    override fun onResume() {
        super.onResume()
        presenter.loadWalletGuid()
    }

    override fun showToast(@StringRes messageId: Int, @ToastCustom.ToastType toastType: String) {
        ToastCustom.makeText(this, getString(messageId), ToastCustom.LENGTH_SHORT, toastType)
    }

    override fun showErrorToastWithParameter(@StringRes messageId: Int, message: String) {
        ToastCustom.makeText(this, getString(messageId, message), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR)
    }

    override fun restartPage() {
        val intent = Intent(this, LauncherActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun resetPasswordField() {
        if (!isFinishing) binding.fieldPassword.setText("")
    }

    override fun showWalletGuid(guid: String) {
        binding.walletIdentifier.text = guid
    }

    override fun goToPinPage() {
        startActivity(Intent(this, PinEntryActivity::class.java))
    }

    override fun updateWaitingForAuthDialog(secondsRemaining: Int) =
        updateProgressDialog(getString(R.string.check_email_to_auth_login) + " " + secondsRemaining)

    override fun showForgetWalletWarning() {
        showAlert(
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.warning)
                .setMessage(R.string.forget_wallet_warning)
                .setPositiveButton(R.string.forget_wallet) { _, _ -> presenter.onForgetWalletConfirmed() }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .create()
        )
    }

    override fun showTwoFactorCodeNeededDialog(
        responseObject: JSONObject,
        sessionId: String,
        authType: Int,
        guid: String,
        password: String
    ) {
        hideKeyboard()

        val dialog = getTwoFactorDialog(
            this, authType,
            walletPrefs,
            positiveAction = {
                presenter.submitTwoFactorCode(
                    responseObject,
                    sessionId,
                    guid,
                    password,
                    it
                )
            }, resendAction = { limitReached ->
            if (!limitReached) {
                presenter.requestNew2FaCode(password, guid)
            } else {
                ToastCustom.makeText(
                    this, getString(R.string.two_factor_retries_exceeded),
                    Toast.LENGTH_SHORT, ToastCustom.TYPE_ERROR
                )
                if (!isTwoFATimerRunning) {
                    twoFATimer.start()
                }
            }
        }
        )

        showAlert(dialog)
    }

    override fun onDestroy() {
        dismissProgressDialog()
        presenter.cancelAuthTimer()
        super.onDestroy()
    }

    private fun launchRecoveryFlow() = RecoverFundsActivity.start(this)
}
