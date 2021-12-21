package piuk.blockchain.android.ui.settings

import android.annotation.SuppressLint
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.extensions.exhaustive
import com.blockchain.nabu.datamanagers.Bank
import com.blockchain.nabu.datamanagers.BankState
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.CardStatus
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.responses.nabu.NabuApiException
import com.blockchain.nabu.models.responses.nabu.NabuErrorStatusCodes
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.RatingPrefs
import info.blockchain.balance.FiatCurrency
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.settings.SettingsManager
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.kotlin.zipWith
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.Serializable
import piuk.blockchain.android.R
import piuk.blockchain.android.data.biometrics.BiometricsController
import piuk.blockchain.android.scan.QrScanError
import piuk.blockchain.android.scan.QrScanResultProcessor
import piuk.blockchain.android.scan.ScanResult
import piuk.blockchain.android.simplebuy.SimpleBuyModel
import piuk.blockchain.android.ui.auth.newlogin.SecureChannelManager
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalyticsAccountType
import piuk.blockchain.android.util.AndroidUtils
import piuk.blockchain.androidcore.data.access.PinRepository
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.extensions.thenSingle
import thepit.PitLinking
import thepit.PitLinkingState
import timber.log.Timber

class SettingsPresenter(
    private val authDataManager: AuthDataManager,
    private val settingsDataManager: SettingsDataManager,
    private val emailUpdater: EmailSyncUpdater,
    private val payloadManager: PayloadManager,
    private val payloadDataManager: PayloadDataManager,
    private val prefs: PersistentPrefs,
    private val pinRepository: PinRepository,
    private val custodialWalletManager: CustodialWalletManager,
    private val notificationTokenManager: NotificationTokenManager,
    private val exchangeRates: ExchangeRatesDataManager,
    private val kycStatusHelper: KycStatusHelper,
    private val pitLinking: PitLinking,
    private val analytics: Analytics,
    private val biometricsController: BiometricsController,
    private val ratingPrefs: RatingPrefs,
    private val qrProcessor: QrScanResultProcessor,
    private val secureChannelManager: SecureChannelManager
) : BasePresenter<SettingsView>() {

    private val fiatCurrency: FiatCurrency
        get() = prefs.selectedFiatCurrency as FiatCurrency

    private var pitClickedListener = {}

    override fun onViewReady() {
        view?.showProgress()
        compositeDisposable += settingsDataManager.fetchSettings()
            .singleOrError()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { settings ->
                    handleUpdate(settings)
                },
                onError = {
                    handleUpdate(Settings())
                    view?.showError(R.string.settings_error_updating)
                }
            )

        compositeDisposable += pitLinking.state
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { state -> onPitStateUpdated(state) },
                onError = { Timber.e(it) }
            )
        updateCards()
        updateBanks()
    }

    private fun updateCards() {
        compositeDisposable +=
            custodialWalletManager.getEligiblePaymentMethodTypes(fiatCurrency).map { eligiblePaymentMethods ->
                eligiblePaymentMethods.firstOrNull { it.paymentMethodType == PaymentMethodType.PAYMENT_CARD } != null
            }
                .doOnSuccess { isCardEligible ->
                    view?.cardsEnabled(isCardEligible)
                }
                .flatMap { isCardEligible ->
                    if (isCardEligible) {
                        custodialWalletManager.updateSupportedCardTypes(fiatCurrency)
                            .thenSingle {
                                custodialWalletManager.fetchUnawareLimitsCards(
                                    listOf(CardStatus.ACTIVE, CardStatus.EXPIRED)
                                )
                            }
                    } else {
                        Single.just(emptyList())
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    view?.cardsEnabled(false)
                    onCardsUpdated(emptyList())
                }
                .subscribeBy(
                    onSuccess = { cards ->
                        onCardsUpdated(cards)
                    },
                    onError = {
                        Timber.i(it)
                    }
                )
    }

    fun checkShouldDisplayRateUs() {
        if (ratingPrefs.preRatingActionCompletedTimes >= SimpleBuyModel.COMPLETED_ORDERS_BEFORE_SHOWING_APP_RATING) {
            view?.showRateUsPreference()
        }
    }

    fun updateBanks() {
        compositeDisposable +=
            eligibleBankPaymentMethods(fiatCurrency).zipWith(linkedBanks().onErrorReturn { emptySet() })
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    view?.banksEnabled(false)
                }
                .subscribeBy(
                    onSuccess = { (linkableBanks, linkedBanks) ->
                        view?.banksEnabled(linkedBanks.isNotEmpty() or linkableBanks.isNotEmpty())
                        view?.updateLinkedBanks(linkedBanks.getEligibleLinkedBanks(linkableBanks))
                        view?.updateLinkableBanks(linkableBanks, linkedBanks.size)
                    },
                    onError = {
                        Timber.e(it)
                    }
                )
    }

    private fun Set<Bank>.getEligibleLinkedBanks(linkableBanks: Set<LinkablePaymentMethods>): Set<Bank> {
        val paymentMethod = linkableBanks.filter { it.currency == fiatCurrency }.distinct().firstOrNull()
        val eligibleLinkedBanks = this.filter { paymentMethod?.linkMethods?.contains(it.paymentMethodType) ?: false }

        return this.map {
            it.copy(
                canBeUsedToTransact = eligibleLinkedBanks.contains(it)
            )
        }.toSet()
    }

    private fun onCardsUpdated(cards: List<PaymentMethod.Card>) {
        view?.updateCards(cards)
    }

    private fun onPitStateUpdated(state: PitLinkingState) {
        view?.let {
            it.setPitLinkingState(state.isLinked)
            pitClickedListener = if (state.isLinked) {
                { it.launchThePit() }
            } else {
                { it.launchThePitLandingActivity() }
            }
        }
    }

    private fun eligibleBankPaymentMethods(fiat: FiatCurrency): Single<Set<LinkablePaymentMethods>> =
        custodialWalletManager.getEligiblePaymentMethodTypes(fiat).map { methods ->
            val bankPaymentMethods = methods.filter {
                it.paymentMethodType == PaymentMethodType.BANK_TRANSFER ||
                    it.paymentMethodType == PaymentMethodType.BANK_ACCOUNT
            }

            bankPaymentMethods.map { method ->
                LinkablePaymentMethods(
                    method.currency,
                    bankPaymentMethods.filter { it.currency == method.currency }.map { it.paymentMethodType }.distinct()
                )
            }.toSet()
        }

    private fun linkedBanks(): Single<Set<Bank>> =
        custodialWalletManager.getBanks().map { banks ->
            banks.filter { it.state == BankState.ACTIVE }
        }.map { banks ->
            banks.toSet()
        }

    private fun handleUpdate(settings: Settings) {
        view?.hideProgress()
        view?.setUpUi()
        updateUi(settings)
    }

    private fun updateUi(settings: Settings) =
        view?.apply {
            setGuidSummary(settings.guid)
            setEmailSummary(settings.email, settings.isEmailVerified)
            setSmsSummary(settings.smsNumber, settings.isSmsVerified)
            setFiatSummary(fiatCurrency.displayTicker)
            setEmailNotificationsVisibility(settings.isEmailVerified)

            setEmailNotificationPref(false)
            setPushNotificationPref(arePushNotificationEnabled())

            val emailNotificationsEnabled = settings.notificationsType.any {
                it == Settings.NOTIFICATION_TYPE_EMAIL ||
                    it == Settings.NOTIFICATION_TYPE_ALL
            }
            setEmailNotificationPref(emailNotificationsEnabled)
            setFingerprintVisibility(isFingerprintHardwareAvailable)
            updateFingerprintPreferenceStatus()
            setTwoFaPreference(settings.authType != Settings.AUTH_TYPE_OFF)
            setTorBlocked(settings.isBlockTorIps)
            setScreenshotsEnabled(prefs.getValue(PersistentPrefs.KEY_SCREENSHOTS_ENABLED, false))
            setLauncherShortcutVisibility(AndroidUtils.is25orHigher())
        }

    /**
     * @return true if the device has usable fingerprint hardware
     */
    private val isFingerprintHardwareAvailable: Boolean
        get() = biometricsController.isHardwareDetected

    /**
     * @return true if the user has previously enabled fingerprint login
     */
    val isFingerprintUnlockEnabled: Boolean
        get() = biometricsController.isBiometricUnlockEnabled

    /**
     * Sets fingerprint unlock enabled and clears the encrypted PIN if {@param enabled} is false
     *
     * @param enabled Whether or not the fingerprint unlock feature is set up
     */
    fun setFingerprintUnlockDisabled() {
        biometricsController.setBiometricUnlockDisabled()
    }

    /**
     * Handle fingerprint preference toggle
     */
    fun onFingerprintClicked() {
        if (isFingerprintUnlockEnabled) {
            // Show dialog "are you sure you want to disable fingerprint login?
            view?.showDisableFingerprintDialog()
        } else if (!biometricsController.areBiometricsEnrolled) {
            // No fingerprints enrolled, prompt user to add some
            view?.showNoFingerprintsAddedDialog()
        } else {
            val pin = pinRepository.pin
            if (pin.isNotEmpty()) {
                view?.showFingerprintDialog(pin)
            } else {
                throw IllegalStateException("PIN code not found in AccessState")
            }
        }
    }

    private fun String?.isInvalid(): Boolean =
        this.isNullOrEmpty() || this.length >= 256

    private val cachedSettings: Single<Settings>
        get() = settingsDataManager.getSettings().first(Settings())

    /**
     * @return the temporary password from the Payload Manager
     */
    val tempPassword: String
        get() = payloadManager.tempPassword

    private val authType: Single<Int>
        get() = cachedSettings.map { it.authType }

    /**
     * @return is the user's phone number is verified
     */
    private val isSmsVerified: Single<Boolean>
        get() = cachedSettings.map { it.isSmsVerified }

    /**
     * Write key/value to [android.content.SharedPreferences]
     *
     * @param key The key under which to store the data
     * @param value The value to be stored as a boolean
     */
    fun updatePreferences(key: String, value: Boolean) {
        prefs.setValue(key, value)
    }

    /**
     * Updates the user's email, prompts user to check their email for verification after success
     *
     * @param email The email address to be saved
     */
    fun updateEmail(email: String) {
        if (email.isInvalid()) {
            view?.setEmailUnknown()
            return
        }
        compositeDisposable += emailUpdater.updateEmailAndSync(email)
            .flatMap {
                settingsDataManager.fetchSettings()
                    .singleOrError()
                    .flatMap {
                        updateNotification(Settings.NOTIFICATION_TYPE_EMAIL, false)
                    }
            }.observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    updateUi(it)
                    view?.showDialogEmailVerification()
                },
                onError = {
                    view?.showError(R.string.update_failed)
                }
            )
    }

    /**
     * Updates the user's phone number, prompts user to verify their number after success
     *
     * @param sms The phone number to be saved
     */
    fun updateSms(sms: String) {
        if (sms.isInvalid()) {
            view?.setSmsUnknown()
            return
        }
        compositeDisposable += settingsDataManager.updateSms(sms)
            .firstOrError()
            .flatMap {
                syncPhoneNumberWithNabu().thenSingle {
                    updateNotification(Settings.NOTIFICATION_TYPE_SMS, false)
                }
            }.observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    updateUi(it)
                    view?.showDialogVerifySms()
                },
                onError = {
                    view?.showError(R.string.update_failed)
                }
            )
    }

    /**
     * Verifies a user's number, shows verified dialog after success
     *
     * @param code The verification code which has been sent to the user
     */
    fun verifySms(code: String) {
        compositeDisposable += settingsDataManager.verifySms(code)
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { updateUi(it) }
            .doOnSubscribe { view?.showProgress() }
            .flatMapCompletable { syncPhoneNumberWithNabu() }
            .observeOn(AndroidSchedulers.mainThread())
            .doAfterTerminate {
                view?.hideProgress()
            }
            .subscribeBy(
                onComplete = { view?.showDialogSmsVerified() },
                onError = { view?.showWarningDialog(R.string.verify_sms_failed) }
            )
    }

    private fun syncPhoneNumberWithNabu(): Completable {
        return kycStatusHelper.syncPhoneNumberWithNabu()
            .subscribeOn(Schedulers.io())
            .onErrorResumeNext { throwable: Throwable? ->
                if (throwable is NabuApiException && throwable.getErrorStatusCode() ==
                    NabuErrorStatusCodes.Conflict
                )
                    Completable.complete()
                else Completable.error(throwable)
            }
    }

    /**
     * Updates the user's Tor blocking preference
     *
     * @param blocked Whether or not to block Tor requests
     */
    fun updateTor(blocked: Boolean) {
        compositeDisposable += settingsDataManager.updateTor(blocked)
            .subscribeBy(
                onNext = { updateUi(it) },
                onError = { view?.showError(R.string.update_failed) }
            )
    }

    /**
     * Sets the auth type used for 2FA. Pass in [Settings.AUTH_TYPE_OFF] to disable 2FA
     *
     * @param type The auth type used for 2FA
     * @see Settings
     */
    fun updateTwoFa(type: Int) {
        compositeDisposable += settingsDataManager.updateTwoFactor(type)
            .subscribeBy(
                onNext = { updateUi(it) },
                onError = { view?.showError(R.string.update_failed) }
            )
    }

    fun updateEmailNotification(enabled: Boolean) {
        compositeDisposable += updateNotification(Settings.NOTIFICATION_TYPE_EMAIL, enabled)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = {
                    view?.setEmailNotificationPref(enabled)
                },
                onError = {
                    view?.showError(R.string.update_failed)
                }
            )
    }

    /**
     * Updates the user's notification preferences. Will not make any web requests if not necessary.
     *
     * @param type The notification type to be updated
     * @param enable Whether or not to enable the notification type
     * @see Settings
     */

    private fun updateNotification(type: Int, enable: Boolean): Single<Settings> {
        return cachedSettings.flatMap {
            if (enable && it.isNotificationTypeEnabled(type)) {
                // No need to change
                return@flatMap Single.just(it)
            } else if (!enable && it.isNotificationTypeDisabled(type)) {
                // No need to change
                return@flatMap Single.just(it)
            }
            val notificationsUpdate =
                if (enable) settingsDataManager.enableNotification(type, it.notificationsType)
                else settingsDataManager.disableNotification(type, it.notificationsType)
            notificationsUpdate.flatMapCompletable {
                if (enable) {
                    payloadDataManager.syncPayloadAndPublicKeys()
                } else {
                    payloadDataManager.syncPayloadWithServer()
                }
            }.thenSingle {
                cachedSettings
            }
        }
    }

    private fun Settings.isNotificationTypeEnabled(type: Int): Boolean {
        return isNotificationsOn && (
            notificationsType.contains(type) ||
                notificationsType.contains(SettingsManager.NOTIFICATION_TYPE_ALL)
            )
    }

    private fun Settings.isNotificationTypeDisabled(type: Int): Boolean {
        return notificationsType.contains(SettingsManager.NOTIFICATION_TYPE_NONE) ||
            (
                !notificationsType.contains(SettingsManager.NOTIFICATION_TYPE_ALL) &&
                    !notificationsType.contains(type)
                )
    }

    /**
     * PIN code validated, take user to PIN change page
     */
    fun pinCodeValidatedForChange() {
        prefs.pinFails = 0
        prefs.pinId = ""
        view?.goToPinEntryPage()
    }

    /**
     * Updates the user's password
     *
     * @param password The requested new password as a String
     * @param fallbackPassword The user's current password as a fallback
     */
    @SuppressLint("CheckResult")
    fun updatePassword(password: String, fallbackPassword: String) {
        payloadManager.tempPassword = password
        compositeDisposable += authDataManager.createPin(password, pinRepository.pin)
            .doOnSubscribe { view?.showProgress() }
            .doOnTerminate { view?.hideProgress() }
            .andThen(authDataManager.verifyCloudBackup())
            .andThen(payloadDataManager.syncPayloadWithServer())
            .subscribeBy(
                onComplete = {
                    view?.showError(R.string.password_changed)

                    analytics.logEvent(SettingsAnalytics.PasswordChanged_Old)
                    analytics.logEvent(SettingsAnalytics.PasswordChanged(TxFlowAnalyticsAccountType.USERKEY))
                },
                onError = { showUpdatePasswordFailed(fallbackPassword) }
            )
    }

    private fun showUpdatePasswordFailed(fallbackPassword: String) {
        payloadManager.tempPassword = fallbackPassword
        view?.showError(R.string.remote_save_failed)
        view?.showError(R.string.password_unchanged)
    }

    /**
     * Updates the user's fiat unit preference
     */
    fun updateFiatUnit(fiatUnit: FiatCurrency) {
        compositeDisposable += settingsDataManager.updateFiatUnit(fiatUnit)
            .map { settings ->
                prefs.clearBuyState()
                analytics.logEvent(SettingsAnalytics.CurrencyChanged)
                settings
            }.observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { updateUi(it) },
                onError = { view?.showError(R.string.update_failed) }
            )
    }

    fun updateCloudData(newValue: Boolean) {
        prefs.backupEnabled = newValue
    }

    private fun arePushNotificationEnabled(): Boolean =
        prefs.arePushNotificationsEnabled

    fun enablePushNotifications() {
        compositeDisposable += notificationTokenManager.enableNotifications()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = { view?.setPushNotificationPref(true) },
                onError = { Timber.e(it) }
            )
    }

    fun disablePushNotifications() {
        compositeDisposable += notificationTokenManager.disableNotifications()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = { view?.setPushNotificationPref(false) },
                onError = { Timber.e(it) }
            )
    }

    val currencyLabels: List<FiatCurrency>
        get() = exchangeRates.fiatAvailableForRates

    fun onThePitClicked() {
        pitClickedListener()
    }

    fun processScanResult(scanData: String) {
        compositeDisposable += qrProcessor.processScan(scanData)
            .subscribeBy(
                onSuccess = {
                    when (it) {
                        is ScanResult.SecuredChannelLogin -> secureChannelManager.sendHandshake(it.handshake)
                        else -> {
                        }
                    }.exhaustive
                },
                onError = {
                    when (it) {
                        is QrScanError -> view?.showScanTargetError(it)
                        else -> {
                            Timber.d("Scan failed")
                        }
                    }
                }
            )
    }

    fun onTwoStepVerificationRequested() {
        compositeDisposable += authType.zipWith(
            isSmsVerified
        ).subscribeBy { (auth, smsVerified) ->
            view?.showDialogTwoFA(auth, smsVerified)
        }
    }

    fun resendSms() {
        compositeDisposable += cachedSettings.subscribeBy {
            updateSms(it.smsNumber)
        }
    }

    fun onVerifySmsRequested() {
        compositeDisposable += cachedSettings.subscribeBy {
            view?.showDialogMobile(it.authType, it.isSmsVerified, it.smsNumber ?: "")
        }
    }

    fun onEmailShowRequested() {
        compositeDisposable += cachedSettings.subscribeBy {
            view?.showEmailDialog(it.email, it.isEmailVerified)
        }
    }

    fun linkBank(currency: FiatCurrency) {
        compositeDisposable += custodialWalletManager.linkToABank(currency)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onSuccess = {
                view?.linkBankWithPartner(it)
            }, onError = {
                view?.showError(R.string.failed_to_link_bank)
            })
    }
}

data class LinkablePaymentMethods(
    val currency: FiatCurrency,
    val linkMethods: List<PaymentMethodType>
) : Serializable
