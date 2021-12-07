package piuk.blockchain.android.ui.kyc.email.entry

import com.blockchain.logging.CrashLogger
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.zipWith
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class EmailVeriffModel(
    private val interactor: EmailVerifyInteractor,
    uiScheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<EmailVeriffState, EmailVeriffIntent>(
    EmailVeriffState(), uiScheduler, environmentConfig, crashLogger
) {

    override fun performAction(previousState: EmailVeriffState, intent: EmailVeriffIntent): Disposable? =
        when (intent) {
            EmailVeriffIntent.FetchEmail -> interactor.fetchEmail().subscribeBy(
                onSuccess = {
                    process(EmailVeriffIntent.EmailUpdated(it))
                }, onError = {
                process(EmailVeriffIntent.ErrorEmailVerification)
            }
            )
            EmailVeriffIntent.CancelEmailVerification -> interactor.cancelPolling().subscribeBy(
                onComplete = {},
                onError = {}
            )
            EmailVeriffIntent.StartEmailVerification ->
                interactor.fetchEmail().zipWith(
                    interactor.isRedesignEnabled()
                ).doOnSuccess { (_, enabled) ->
                    process(EmailVeriffIntent.UpdateRedesignState(enabled))
                }.flatMapObservable { (email, _) ->
                    if (!email.isVerified) {
                        interactor.pollForEmailStatus().toObservable().startWithItem(email)
                    } else {
                        Observable.just(email)
                    }
                }.subscribeBy(
                    onNext = {
                        process(EmailVeriffIntent.EmailUpdated(it))
                    }, onError = {
                    process(EmailVeriffIntent.ErrorEmailVerification)
                }
                )

            EmailVeriffIntent.ResendEmail -> interactor.fetchEmail().flatMap { interactor.resendEmail(it.address) }
                .subscribeBy(onSuccess = {
                    process(EmailVeriffIntent.EmailUpdated(it))
                }, onError = {})

            EmailVeriffIntent.UpdateEmail -> {
                check(previousState.emailInput != null)
                interactor.updateEmail(previousState.emailInput).subscribeBy(onSuccess = {
                    process(EmailVeriffIntent.EmailUpdated(it))
                }, onError = {
                    process(EmailVeriffIntent.EmailUpdateFailed)
                })
            }
            else -> null
        }
}
