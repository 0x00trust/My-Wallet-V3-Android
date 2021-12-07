package piuk.blockchain.android.ui.kyc.email.entry

import piuk.blockchain.android.ui.base.mvi.MviIntent
import piuk.blockchain.androidcore.data.settings.Email

sealed class EmailVeriffIntent : MviIntent<EmailVeriffState> {

    class EmailUpdated(private val mail: Email) : EmailVeriffIntent() {
        override fun reduce(oldState: EmailVeriffState): EmailVeriffState =
            oldState.copy(
                email = mail,
                isLoading = false,
                emailChanged = oldState.email.address != mail.address &&
                    oldState.email.address.isNotEmpty() &&
                    mail.address.isNotEmpty()
            )
    }

    class UpdateRedesignState(private val redesignEnabled: Boolean) : EmailVeriffIntent() {
        override fun reduce(oldState: EmailVeriffState): EmailVeriffState =
            oldState.copy(isRedesignEnabled = redesignEnabled)
    }

    class UpdateEmailInput(private val emailInput: String) : EmailVeriffIntent() {
        override fun reduce(oldState: EmailVeriffState): EmailVeriffState = oldState.copy(emailInput = emailInput)
    }

    object ErrorEmailVerification : EmailVeriffIntent() {
        override fun reduce(oldState: EmailVeriffState): EmailVeriffState = oldState.copy(hasError = true)
    }

    object FetchEmail : EmailVeriffIntent() {
        override fun reduce(oldState: EmailVeriffState): EmailVeriffState = oldState.copy(isLoading = true)
    }

    object CancelEmailVerification : EmailVeriffIntent() {
        override fun reduce(oldState: EmailVeriffState): EmailVeriffState = oldState
    }

    object EmailUpdateFailed : EmailVeriffIntent() {
        override fun reduce(oldState: EmailVeriffState): EmailVeriffState = oldState.copy(isLoading = false)
    }

    object StartEmailVerification : EmailVeriffIntent() {
        override fun reduce(oldState: EmailVeriffState): EmailVeriffState = oldState
    }

    object UpdateEmail : EmailVeriffIntent() {
        override fun reduce(oldState: EmailVeriffState): EmailVeriffState = oldState.copy(isLoading = true)
    }

    object ResendEmail : EmailVeriffIntent() {
        override fun reduce(oldState: EmailVeriffState): EmailVeriffState = oldState.copy(isLoading = true)
    }
}
