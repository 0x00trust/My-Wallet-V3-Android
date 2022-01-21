package piuk.blockchain.android.ui.start

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import com.blockchain.commonarch.databinding.ToolbarGeneralBinding
import com.blockchain.componentlib.viewextensions.hideKeyboard
import com.blockchain.koin.scopedInject
import com.blockchain.preferences.WalletStatus
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import org.koin.android.ext.android.inject
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityManualPairingBinding
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.getTwoFactorDialog
import piuk.blockchain.android.ui.login.auth.LoginAuthState.Companion.TWO_FA_COUNTDOWN
import piuk.blockchain.android.ui.login.auth.LoginAuthState.Companion.TWO_FA_STEP
import piuk.blockchain.android.util.AfterTextChangedWatcher

class ManualPairingActivity : MvpActivity<ManualPairingView, ManualPairingPresenter>(), ManualPairingView {

    private val binding: ActivityManualPairingBinding by lazy {
        ActivityManualPairingBinding.inflate(layoutInflater)
    }

    private val prefilledGuid: String by lazy {
        intent.getStringExtra(PREFILLED_GUID) ?: ""
    }

    override val view: ManualPairingView = this
    override val presenter: ManualPairingPresenter by scopedInject()
    private val walletPrefs: WalletStatus by inject()

    override val toolbarBinding: ToolbarGeneralBinding
        get() = binding.toolbar

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

    private val guid: String
        get() = binding.walletId.text.toString()
    private val password: String
        get() = binding.walletPass.text.toString()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateToolbar(
            toolbarTitle = getString(R.string.manual_pairing),
            backAction = { onBackPressed() }
        )
        with(binding) {
            binding.walletId.disableInputForDemoAccount()
            commandNext.setOnClickListener { presenter.onContinueClicked(guid, password) }
            binding.walletId.setText(prefilledGuid)
            walletPass.setOnEditorActionListener { _, i, _ ->
                if (i == EditorInfo.IME_ACTION_GO) {
                    presenter.onContinueClicked(guid, password)
                }
                true
            }
        }
    }

    override fun showToast(@StringRes messageId: Int, @ToastCustom.ToastType toastType: String) {
        ToastCustom.makeText(this, getString(messageId), ToastCustom.LENGTH_SHORT, toastType)
    }

    override fun showErrorToastWithParameter(@StringRes messageId: Int, message: String) {
        ToastCustom.makeText(this, getString(messageId, message), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR)
    }

    override fun goToPinPage() {
        startActivity(Intent(this, PinEntryActivity::class.java))
    }

    override fun updateWaitingForAuthDialog(secondsRemaining: Int) {
        updateProgressDialog(getString(R.string.check_email_to_auth_login) + " " + secondsRemaining)
    }

    override fun showTwoFactorCodeNeededDialog(
        responseObject: JSONObject,
        sessionId: String,
        authType: Int,
        guid: String,
        password: String
    ) {

        hideKeyboard()

        val dialog = getTwoFactorDialog(this, authType, walletPrefs, positiveAction = {
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
                    ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_ERROR
                )
                if (!isTwoFATimerRunning) {
                    twoFATimer.start()
                }
            }
        })

        showAlert(dialog)
    }

    override fun resetPasswordField() {
        if (!isFinishing)
            binding.walletPass.setText("")
    }

    public override fun onDestroy() {
        currentFocus?.let {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
        dismissProgressDialog()
        presenter.cancelAuthTimer()
        super.onDestroy()
    }

    companion object {
        private const val PREFILLED_GUID = "PREFILLED_GUID"
        fun newInstance(activity: Activity, guid: String?): Intent {
            val intent = Intent(activity, ManualPairingActivity::class.java)
            intent.putExtra(PREFILLED_GUID, guid)
            return intent
        }
    }

    private fun TextInputEditText.disableInputForDemoAccount() {
        addTextChangedListener(object : AfterTextChangedWatcher() {
            override fun afterTextChanged(text: Editable) {
                if (text.toString().isNotEmpty() && text.toString() == BuildConfig.PLAY_STORE_DEMO_WALLET_ID)
                    inputType = InputType.TYPE_NULL
            }
        })
    }
}
