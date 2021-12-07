package piuk.blockchain.android.ui.kyc.email.entry

import com.blockchain.network.PollService
import com.blockchain.remoteconfig.FeatureFlag
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import piuk.blockchain.androidcore.data.settings.Email
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcore.utils.extensions.thenSingle

class EmailVerifyInteractor(
    private val emailUpdater: EmailSyncUpdater,
    private val isRedesignEnabled: FeatureFlag
) {

    private val pollEmail = PollService(
        emailUpdater.email()
    ) {
        it.isVerified
    }

    fun fetchEmail(): Single<Email> =
        emailUpdater.email()

    fun pollForEmailStatus(): Single<Email> {
        return cancelPolling().thenSingle {
            pollEmail.start(timerInSec = 1, retries = Integer.MAX_VALUE).map {
                it.value
            }
        }
    }

    fun isRedesignEnabled(): Single<Boolean> = isRedesignEnabled.enabled

    fun resendEmail(email: String): Single<Email> {
        return emailUpdater.updateEmailAndSync(email)
    }

    fun updateEmail(email: String): Single<Email> =
        emailUpdater.updateEmailAndSync(email)

    fun cancelPolling(): Completable =
        Completable.fromCallable {
            pollEmail.cancel.onNext(true)
        }
}
