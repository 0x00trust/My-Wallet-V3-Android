package piuk.blockchain.android.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import piuk.blockchain.android.databinding.ActivityPinEntryBinding
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class PinEntryActivity : BlockchainActivity() {
    private val binding: ActivityPinEntryBinding by lazy {
        ActivityPinEntryBinding.inflate(layoutInflater)
    }

    private val pinEntryFragment: PinEntryFragment by lazy {
        PinEntryFragment.newInstance(isAfterCreateWallet)
    }

    private val isAfterCreateWallet: Boolean by unsafeLazy {
        intent.getBooleanExtra(EXTRA_IS_AFTER_WALLET_CREATION, false)
    }

    override val alwaysDisableScreenshots: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        supportFragmentManager.beginTransaction()
            .add(binding.pinContainer.id, pinEntryFragment)
            .commitAllowingStateLoss()
    }

    override fun onBackPressed() {
        when {
            pinEntryFragment.isValidatingPinForResult -> {
                finishWithResultCanceled()
            }
            pinEntryFragment.allowExit() -> {
                appUtil.logout()
            }
        }
    }

    private fun finishWithResultCanceled() {
        val intent = Intent()
        setResult(RESULT_CANCELED, intent)
        finish()
    }

    companion object {

        const val REQUEST_CODE_UPDATE = 188
        private const val EXTRA_IS_AFTER_WALLET_CREATION = "piuk.blockchain.android.EXTRA_IS_AFTER_WALLET_CREATION"

        fun start(context: Context) {
            val intent = Intent(context, PinEntryActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        fun startAfterWalletCreation(context: Context) {
            val intent = Intent(context, PinEntryActivity::class.java)
            intent.putExtra(EXTRA_IS_AFTER_WALLET_CREATION, true)
            context.startActivity(intent)
        }
    }
}
