package piuk.blockchain.android.ui.settings

import com.blockchain.android.testutils.rxInit
import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.nabu.datamanagers.Bank
import com.blockchain.nabu.datamanagers.BankState
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.EligiblePaymentMethodType
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.responses.nabu.KycTierState
import com.blockchain.nabu.models.responses.nabu.NabuApiException.Companion.fromResponseBody
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.RatingPrefs
import com.blockchain.remoteconfig.FeatureFlag
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.settings.SettingsManager
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import junit.framework.Assert.assertFalse
import junit.framework.Assert.assertTrue
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import piuk.blockchain.android.R
import piuk.blockchain.android.data.biometrics.BiometricsController
import piuk.blockchain.android.scan.QrScanResultProcessor
import piuk.blockchain.android.ui.auth.newlogin.SecureChannelManager
import piuk.blockchain.android.ui.kyc.settings.KycStatusHelper
import piuk.blockchain.android.ui.tiers
import piuk.blockchain.androidcore.data.access.PinRepository
import piuk.blockchain.androidcore.data.auth.AuthDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.Email
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import retrofit2.HttpException
import retrofit2.Response.error
import thepit.PitLinking
import thepit.PitLinkingState

class SettingsPresenterTest {

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private lateinit var subject: SettingsPresenter

    private val activity: SettingsView = mock()

    private val biometricsController: BiometricsController = mock()
    private val authDataManager: AuthDataManager = mock()

    private val settingsDataManager: SettingsDataManager = mock()

    private val payloadManager: PayloadManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()

    private val prefsUtil: PersistentPrefs = mock()
    private val pinRepository: PinRepository = mock()

    private val notificationTokenManager: NotificationTokenManager = mock()
    private val exchangeRates: ExchangeRatesDataManager = mock()
    private val kycStatusHelper: KycStatusHelper = mock()
    private val emailSyncUpdater: EmailSyncUpdater = mock()
    private val pitLinking: PitLinking = mock()
    private val pitLinkState: PitLinkingState = mock()
    private val ratingPrefs: RatingPrefs = mock()
    private val qrProcessor: QrScanResultProcessor = mock()
    private val secureChannelManager: SecureChannelManager = mock()

    private val featureFlag: FeatureFlag = mock()

    private val analytics: Analytics = mock()
    private val custodialWalletManager: CustodialWalletManager = mock()
    private val cardsFeatureFlag: FeatureFlag = mock()
    private val fundsFeatureFlag: FeatureFlag = mock()

    @Before
    fun setUp() {
        subject = SettingsPresenter(
            authDataManager = authDataManager,
            settingsDataManager = settingsDataManager,
            emailUpdater = emailSyncUpdater,
            payloadManager = payloadManager,
            payloadDataManager = payloadDataManager,
            prefs = prefsUtil,
            pinRepository = pinRepository,
            custodialWalletManager = custodialWalletManager,
            notificationTokenManager = notificationTokenManager,
            exchangeRates = exchangeRates,
            kycStatusHelper = kycStatusHelper,
            pitLinking = pitLinking,
            analytics = analytics,
            biometricsController = biometricsController,
            ratingPrefs = ratingPrefs,
            qrProcessor = qrProcessor,
            secureChannelManager = secureChannelManager
        )
        subject.initView(activity)
        whenever(prefsUtil.selectedFiatCurrency).thenReturn(USD)
        whenever(prefsUtil.arePushNotificationsEnabled).thenReturn(false)
        whenever(biometricsController.isHardwareDetected).thenReturn(false)
        whenever(prefsUtil.getValue(any(), any<Boolean>())).thenReturn(false)
        whenever(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete())
        whenever(payloadDataManager.syncPayloadAndPublicKeys()).thenReturn(Completable.complete())
    }

    @Test
    fun onViewReadySuccess() {
        // Arrange
        val mockSettings: Settings = mock {
            on { isNotificationsOn }.thenReturn(true)
            on { notificationsType }.thenReturn(listOf(1, 32))
            on { smsNumber }.thenReturn("sms")
            on { email }.thenReturn("email")
        }

        whenever(settingsDataManager.fetchSettings()).thenReturn(Observable.just(mockSettings))
        whenever(prefsUtil.selectedFiatCurrency).thenReturn(USD)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))
        whenever(kycStatusHelper.getSettingsKycStateTier())
            .thenReturn(Single.just(tiers(KycTierState.None, KycTierState.None)))
        whenever(pitLinkState.isLinked).thenReturn(false)
        whenever(custodialWalletManager.fetchUnawareLimitsCards(ArgumentMatchers.anyList()))
            .thenReturn(Single.just(emptyList()))
        whenever(pitLinking.state).thenReturn(Observable.just(pitLinkState))

        whenever(featureFlag.enabled).thenReturn(Single.just(true))
        whenever(cardsFeatureFlag.enabled).thenReturn(Single.just(true))
        whenever(fundsFeatureFlag.enabled).thenReturn(Single.just(true))

        arrangeEligiblePaymentMethodTypes(USD, listOf(EligiblePaymentMethodType(PaymentMethodType.PAYMENT_CARD, USD)))
        whenever(custodialWalletManager.canTransactWithBankMethods(any())).thenReturn(Single.just(false))
        whenever(custodialWalletManager.updateSupportedCardTypes(ArgumentMatchers.anyString())).thenReturn(
            Completable.complete()
        )
        arrangeBanks(emptyList())
        arrangeEligiblePaymentMethodTypes(any(), emptyList())
        // Act
        subject.onViewReady()
        // Assert
        verify(activity).showProgress()
        verify(activity).hideProgress()
        verify(activity).setUpUi()
        verify(activity).setPitLinkingState(false)
        verify(activity, Mockito.times(2)).updateCards(emptyList())
    }

    @Test
    fun onViewReadyFailed() {
        // Arrange
        whenever(
            settingsDataManager.fetchSettings()
        ).thenReturn(Observable.error(Throwable()))
        whenever(pitLinkState.isLinked).thenReturn(false)
        whenever(kycStatusHelper.getSettingsKycStateTier())
            .thenReturn(Single.just(tiers(KycTierState.Verified, KycTierState.Verified)))
        whenever(pitLinking.state).thenReturn(Observable.just(pitLinkState))
        whenever(featureFlag.enabled).thenReturn(Single.just(false))
        whenever(prefsUtil.selectedFiatCurrency).thenReturn(USD)
        whenever(cardsFeatureFlag.enabled).thenReturn(Single.just(false))
        whenever(fundsFeatureFlag.enabled).thenReturn(Single.just(false))

        whenever(custodialWalletManager.canTransactWithBankMethods(any())).thenReturn(Single.just(false))
        arrangeEligiblePaymentMethodTypes(USD, listOf(EligiblePaymentMethodType(PaymentMethodType.PAYMENT_CARD, USD)))
        whenever(custodialWalletManager.updateSupportedCardTypes(ArgumentMatchers.anyString())).thenReturn(
            Completable.complete()
        )
        whenever(custodialWalletManager.fetchUnawareLimitsCards(ArgumentMatchers.anyList()))
            .thenReturn(Single.just(emptyList()))
        arrangeBanks(emptyList())
        arrangeEligiblePaymentMethodTypes(any(), emptyList())

        // Act
        subject.onViewReady()

        // Assert
        verify(activity).showProgress()
        verify(activity).hideProgress()
        verify(activity).setUpUi()
        verify(activity, times(2)).updateCards(emptyList())
    }

    @Test
    fun onKycStatusClicked_should_launch_homebrew_tier1() {
        assertClickLaunchesKyc(KycTierState.Verified, KycTierState.None)
    }

    @Test
    fun onKycStatusClicked_should_launch_homebrew_tier2() {
        assertClickLaunchesKyc(KycTierState.Verified, KycTierState.Verified)
    }

    @Test
    fun onKycStatusClicked_should_launch_kyc_flow_locked() {
        assertClickLaunchesKyc(KycTierState.None, KycTierState.None)
    }

    @Test
    fun onKycStatusClicked_should_launch_kyc_status_tier1_review() {
        assertClickLaunchesKyc(KycTierState.Pending, KycTierState.None)
    }

    @Test
    fun onKycStatusClicked_should_launch_kyc_status_tier2_review() {
        assertClickLaunchesKyc(KycTierState.Verified, KycTierState.Pending)
    }

    @Test
    fun onKycStatusClicked_should_launch_kyc_status_tier1_rejected() {
        assertClickLaunchesKyc(KycTierState.Rejected, KycTierState.None)
    }

    @Test
    fun onKycStatusClicked_should_launch_kyc_status_tier2_rejected() {
        assertClickLaunchesKyc(KycTierState.Verified, KycTierState.Rejected)
    }

    @Test
    fun updateEmailSuccess() {
        // Arrange
        val notifications: List<Int> = listOf(SettingsManager.NOTIFICATION_TYPE_EMAIL)

        val mockSettings = Settings().copy(notificationsType = notifications)

        val email = "EMAIL"
        whenever(emailSyncUpdater.updateEmailAndSync(email)).thenReturn(Single.just(Email(email, false)))
        whenever(settingsDataManager.fetchSettings()).thenReturn(Observable.just(mockSettings))
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))
        whenever(settingsDataManager.disableNotification(Settings.NOTIFICATION_TYPE_EMAIL, notifications))
            .thenReturn(Observable.just(mockSettings))

        // Act
        subject.updateEmail(email)

        // Assert
        verify(emailSyncUpdater).updateEmailAndSync(email)
        verify(settingsDataManager).disableNotification(Settings.NOTIFICATION_TYPE_EMAIL, notifications)
        verify(activity).showDialogEmailVerification()
    }

    @Test
    fun updateEmailFailed() {
        // Arrange
        val email = "EMAIL"
        whenever(emailSyncUpdater.updateEmailAndSync(email)).thenReturn(Single.error(Throwable()))

        // Act
        subject.updateEmail(email)

        // Assert
        verify(emailSyncUpdater).updateEmailAndSync(email)
        verify(activity).showError(R.string.update_failed)
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun updateSmsInvalid() {
        // Arrange
        subject.updateSms("")
        // Assert
        verify(activity).setSmsUnknown()
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun updateSmsSuccess() {
        // Arrange
        val notifications: List<Int> = listOf(SettingsManager.NOTIFICATION_TYPE_SMS)

        val mockSettings = Settings().copy(notificationsType = notifications)
        val phoneNumber = "PHONE_NUMBER"
        whenever(settingsDataManager.updateSms(phoneNumber)).thenReturn(Observable.just(mockSettings))
        whenever(settingsDataManager.disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications))
            .thenReturn(Observable.just(mockSettings))
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))
        whenever(kycStatusHelper.syncPhoneNumberWithNabu()).thenReturn(Completable.complete())

        // Act
        subject.updateSms(phoneNumber)

        // Assert
        verify(settingsDataManager).updateSms(phoneNumber)
        verify(settingsDataManager).disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications)
        verify(activity).showDialogVerifySms()
    }

    @Test
    fun updateSmsSuccess_despiteNumberAlreadyRegistered() {
        // Arrange
        val notifications: List<Int> = listOf(SettingsManager.NOTIFICATION_TYPE_SMS)

        val mockSettings = Settings().copy(notificationsType = notifications)
        val phoneNumber = "PHONE_NUMBER"
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))
        whenever(
            settingsDataManager.updateSms(phoneNumber)
        ).thenReturn(Observable.just(mockSettings))
        whenever(
            settingsDataManager.disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications)
        )
            .thenReturn(Observable.just(mockSettings))
        val responseBody = ResponseBody.create("application/json".toMediaTypeOrNull(), "{}")
        val error = fromResponseBody(HttpException(error<Any>(409, responseBody)))
        whenever(kycStatusHelper.syncPhoneNumberWithNabu()).thenReturn(Completable.error(error))

        // Act
        subject.updateSms(phoneNumber)

        // Assert
        verify(settingsDataManager).updateSms(phoneNumber)
        verify(settingsDataManager).disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications)
        verify(activity).showDialogVerifySms()
    }

    @Test
    fun updateSmsFailed() {
        // Arrange
        val phoneNumber = "PHONE_NUMBER"
        whenever(settingsDataManager.updateSms(phoneNumber)).thenReturn(Observable.error(Throwable()))

        // Act
        subject.updateSms(phoneNumber)

        // Assert
        verify(settingsDataManager).updateSms(phoneNumber)
        verifyNoMoreInteractions(settingsDataManager)
        verify(activity).showError(R.string.update_failed)
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun verifySmsSuccess() {
        // Arrange
        val verificationCode = "VERIFICATION_CODE"
        val mockSettings = Settings()
        whenever(settingsDataManager.verifySms(verificationCode)).thenReturn(Observable.just(mockSettings))
        whenever(kycStatusHelper.syncPhoneNumberWithNabu()).thenReturn(Completable.complete())

        // Act
        subject.verifySms(verificationCode)

        // Assert
        verify(settingsDataManager).verifySms(verificationCode)
        verifyNoMoreInteractions(settingsDataManager)
        verify(activity).showProgress()
        verify(activity).hideProgress()
        verify(activity).showDialogSmsVerified()
    }

    @Test
    fun verifySmsFailed() {
        // Arrange
        val verificationCode = "VERIFICATION_CODE"
        whenever(settingsDataManager.verifySms(ArgumentMatchers.anyString())).thenReturn(Observable.error(Throwable()))

        // Act
        subject.verifySms(verificationCode)
        // Assert
        verify(settingsDataManager).verifySms(verificationCode)
        verifyNoMoreInteractions(settingsDataManager)
        verify(activity).showProgress()
        verify(activity).hideProgress()
        verify(activity).showWarningDialog(R.string.verify_sms_failed)
    }

    @Test
    fun updateTorSuccess() {
        // Arrange
        val mockSettings = Settings().copy(
            blockTorIps = 1
        )
        whenever(settingsDataManager.updateTor(true)).thenReturn(Observable.just(mockSettings))

        // Act
        subject.updateTor(true)
        // Assert
        verify(settingsDataManager).updateTor(true)
        verify(activity).setTorBlocked(true)
    }

    @Test
    fun updateTorFailed() {
        // Arrange
        Mockito.`when`(settingsDataManager.updateTor(true)).thenReturn(Observable.error(Throwable()))
        // Act
        subject.updateTor(true)
        // Assert
        verify(settingsDataManager).updateTor(true)
        verify(activity).showError(R.string.update_failed)
    }

    @Test
    fun update2FaSuccess() {
        // Arrange
        val mockSettings = Settings()
        val authType = SettingsManager.AUTH_TYPE_YUBI_KEY
        Mockito.`when`(
            settingsDataManager.updateTwoFactor(authType)
        ).thenReturn(Observable.just(mockSettings))
        // Act
        subject.updateTwoFa(authType)
        // Assert
        verify(settingsDataManager).updateTwoFactor(authType)
    }

    @Test
    fun update2FaFailed() {
        // Arrange
        val authType = SettingsManager.AUTH_TYPE_YUBI_KEY
        whenever(
            settingsDataManager.updateTwoFactor(authType)
        ).thenReturn(Observable.error(Throwable()))

        // Act
        subject.updateTwoFa(authType)
        // Assert
        verify(settingsDataManager).updateTwoFactor(authType)
        verify(activity).showError(R.string.update_failed)
    }

    @Test
    fun enableNotificationSuccess() {
        // Arrange
        val mockSettingsResponse = Settings()
        val mockSettings = Settings().copy(
            notificationsType = listOf(
                SettingsManager.NOTIFICATION_TYPE_NONE
            )
        )

        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))

        whenever(
            settingsDataManager.enableNotification(
                SettingsManager.NOTIFICATION_TYPE_EMAIL,
                listOf(
                    SettingsManager.NOTIFICATION_TYPE_NONE
                )
            )
        )
            .thenReturn(Observable.just(mockSettingsResponse))
        // Act
        subject.updateEmailNotification(true)
        // Assert
        verify(settingsDataManager)
            .enableNotification(
                SettingsManager.NOTIFICATION_TYPE_EMAIL,
                listOf(
                    SettingsManager.NOTIFICATION_TYPE_NONE
                )
            )
        verify(payloadDataManager).syncPayloadAndPublicKeys()
        verify(activity).setEmailNotificationPref(true)
    }

    @Test
    fun disableNotificationSuccess() {
        // Arrange

        val mockSettingsResponse = Settings()
        val mockSettings = Settings().copy(
            notificationsType = listOf(SettingsManager.NOTIFICATION_TYPE_EMAIL)
        )

        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))

        whenever(
            settingsDataManager.disableNotification(
                SettingsManager.NOTIFICATION_TYPE_EMAIL,
                listOf(SettingsManager.NOTIFICATION_TYPE_EMAIL)
            )
        ).thenReturn(Observable.just(mockSettingsResponse))
        // Act
        subject.updateEmailNotification(false)
        // Assert
        verify(settingsDataManager)
            .disableNotification(
                SettingsManager.NOTIFICATION_TYPE_EMAIL,
                listOf(SettingsManager.NOTIFICATION_TYPE_EMAIL)
            )

        verify(payloadDataManager).syncPayloadWithServer()
        verify(activity).setEmailNotificationPref(ArgumentMatchers.anyBoolean())
    }

    @Test
    fun enableNotificationAlreadyEnabled() {
        // Arrange
        val mockSettings = Settings().copy(
            notificationsType = listOf(SettingsManager.NOTIFICATION_TYPE_EMAIL),
            notificationsOn = 1
        )
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))

        // Act
        subject.updateEmailNotification(true)

        // Assert
        verify(settingsDataManager).getSettings()
        verifyNoMoreInteractions(settingsDataManager)
        verify(activity).setEmailNotificationPref(true)
    }

    @Test
    fun disableNotificationAlreadyDisabled() {
        // Assert
        val mockSettings = Settings().copy(
            notificationsType = listOf(SettingsManager.NOTIFICATION_TYPE_NONE),
            notificationsOn = 1
        )
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))

        // Act
        subject.updateEmailNotification(false)

        // Assert
        verify(settingsDataManager).getSettings()
        verifyNoMoreInteractions(settingsDataManager)
        verify(activity).setEmailNotificationPref(false)
    }

    @Test
    fun enableNotificationFailed() {
        // Arrange
        val mockSettings = Settings().copy(
            notificationsType = listOf(SettingsManager.NOTIFICATION_TYPE_NONE)
        )
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))
        whenever(
            settingsDataManager.enableNotification(
                SettingsManager.NOTIFICATION_TYPE_EMAIL,
                listOf(SettingsManager.NOTIFICATION_TYPE_NONE)
            )
        ).thenReturn(Observable.error(Throwable()))

        // Act
        subject.updateEmailNotification(true)

        // Assert
        verify(settingsDataManager).enableNotification(
            SettingsManager.NOTIFICATION_TYPE_EMAIL,
            listOf(SettingsManager.NOTIFICATION_TYPE_NONE)
        )
        verify(activity).showError(R.string.update_failed)
    }

    @Test
    fun pinCodeValidatedForChange() {
        // Arrange

        // Act
        subject.pinCodeValidatedForChange()
        // Assert
        verify(prefsUtil).pinFails = 0
        verify(prefsUtil).pinId = ""
        verify(activity).goToPinEntryPage()
        verifyNoMoreInteractions(activity)
    }

    @Test
    fun updatePasswordSuccess() {
        // Arrange
        val newPassword = "NEW_PASSWORD"
        val oldPassword = "OLD_PASSWORD"
        val pin = "PIN"
        whenever(pinRepository.pin).thenReturn(pin)
        whenever(authDataManager.createPin(newPassword, pin)).thenReturn(Completable.complete())
        whenever(authDataManager.verifyCloudBackup()).thenReturn(Completable.complete())
        whenever(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete())

        // Act
        subject.updatePassword(newPassword, oldPassword)

        // Assert
        verify(pinRepository).pin
        verify(authDataManager).createPin(newPassword, pin)
        verify(payloadDataManager).syncPayloadWithServer()
        verify(activity).showProgress()
        verify(activity).hideProgress()
        verify(activity).showError(R.string.password_changed)
    }

    @Test
    fun updatePasswordFailed() {
        // Arrange
        val newPassword = "NEW_PASSWORD"
        val oldPassword = "OLD_PASSWORD"
        val pin = "PIN"
        whenever(pinRepository.pin).thenReturn(pin)
        whenever(authDataManager.createPin(newPassword, pin))
            .thenReturn(Completable.error(Throwable()))
        whenever(authDataManager.verifyCloudBackup()).thenReturn(Completable.complete())
        whenever(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete())

        // Act
        subject.updatePassword(newPassword, oldPassword)

        // Assert
        verify(pinRepository).pin
        verify(authDataManager).createPin(newPassword, pin)
        verify(payloadDataManager).syncPayloadWithServer()
        verify(payloadManager).tempPassword = newPassword
        verify(payloadManager).tempPassword = oldPassword
        verify(activity).showProgress()
        verify(activity).hideProgress()
        verify(activity).showError(R.string.remote_save_failed)
        verify(activity).showError(R.string.password_unchanged)
    }

    @Test
    fun enablePushNotifications() {
        // Arrange
        whenever(notificationTokenManager.enableNotifications()).thenReturn(Completable.complete())

        // Act
        subject.enablePushNotifications()

        // Assert
        verify(activity).setPushNotificationPref(true)
        verify(notificationTokenManager).enableNotifications()
        verifyNoMoreInteractions(notificationTokenManager)
    }

    @Test
    fun disablePushNotifications() {
        // Arrange
        whenever(notificationTokenManager.disableNotifications()).thenReturn(Completable.complete())

        // Act
        subject.disablePushNotifications()

        // Assert
        verify(activity).setPushNotificationPref(false)
        verify(notificationTokenManager).disableNotifications()
        verifyNoMoreInteractions(notificationTokenManager)
    }

    @Test
    fun updateEligibleLinkedBanks() {
        // Arrange
        whenever(prefsUtil.selectedFiatCurrency).thenReturn(USD)
        arrangeEligiblePaymentMethodTypes(
            USD,
            listOf(
                EligiblePaymentMethodType(PaymentMethodType.BANK_TRANSFER, USD),
                EligiblePaymentMethodType(PaymentMethodType.BANK_ACCOUNT, "EUR")
            )
        )
        arrangeBanks(
            listOf(
                Bank("", "", "", BankState.ACTIVE, "", "", PaymentMethodType.BANK_TRANSFER, ""),
                Bank("", "", "", BankState.ACTIVE, "", "", PaymentMethodType.BANK_ACCOUNT, "")
            )
        )

        // Act
        subject.updateBanks()

        // Assert
        argumentCaptor<Set<Bank>>().apply {
            verify(activity).updateLinkedBanks(capture())

            assertTrue(firstValue.first { it.paymentMethodType == PaymentMethodType.BANK_TRANSFER }.canBeUsedToTransact)
            assertFalse(firstValue.first { it.paymentMethodType == PaymentMethodType.BANK_ACCOUNT }.canBeUsedToTransact)
        }
    }

    @Test
    fun `updateEligibleLinkedBanks - no linkable banks`() {
        // Arrange
        whenever(prefsUtil.selectedFiatCurrency).thenReturn(USD)
        arrangeEligiblePaymentMethodTypes(
            USD,
            listOf(
                EligiblePaymentMethodType(PaymentMethodType.BANK_TRANSFER, "GBP"),
                EligiblePaymentMethodType(PaymentMethodType.BANK_ACCOUNT, "EUR")
            )
        )
        arrangeBanks(
            listOf(
                Bank("", "", "", BankState.ACTIVE, "", "", PaymentMethodType.BANK_TRANSFER, ""),
                Bank("", "", "", BankState.ACTIVE, "", "", PaymentMethodType.BANK_ACCOUNT, "")
            )
        )

        // Act
        subject.updateBanks()

        // Assert
        argumentCaptor<Set<Bank>>().apply {
            verify(activity).updateLinkedBanks(capture())

            assertFalse(
                firstValue.first { it.paymentMethodType == PaymentMethodType.BANK_TRANSFER }.canBeUsedToTransact
            )
            assertFalse(firstValue.first { it.paymentMethodType == PaymentMethodType.BANK_ACCOUNT }.canBeUsedToTransact)
        }
    }

    private fun arrangeEligiblePaymentMethodTypes(
        currency: String,
        eligiblePaymentMethodTypes: List<EligiblePaymentMethodType>
    ) {
        whenever(custodialWalletManager.getEligiblePaymentMethodTypes(currency)).thenReturn(
            Single.just(eligiblePaymentMethodTypes)
        )
    }

    private fun arrangeBanks(banks: List<Bank>) {
        whenever(custodialWalletManager.getBanks()).thenReturn(
            Single.just(banks)
        )
    }

    private fun assertClickLaunchesKyc(status1: KycTierState, status2: KycTierState) {
        // Arrange
        whenever(kycStatusHelper.getKycTierStatus())
            .thenReturn(Single.just(tiers(status1, status2)))

        // Act
        subject.onKycStatusClicked()

        // Assert
        verify(activity).launchKycFlow()
    }

    // companion object
    private companion object {
        const val USD = "USD"
    }
}
