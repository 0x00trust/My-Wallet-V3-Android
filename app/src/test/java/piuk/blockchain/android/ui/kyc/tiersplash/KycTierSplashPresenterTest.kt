package piuk.blockchain.android.ui.kyc.tiersplash

import androidx.navigation.NavDirections
import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.models.responses.nabu.KycTierLevel
import com.blockchain.nabu.models.responses.nabu.KycTierState
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.models.responses.nabu.Limits
import com.blockchain.nabu.models.responses.nabu.Tier
import com.blockchain.nabu.models.responses.nabu.Tiers
import com.blockchain.nabu.service.TierService
import com.blockchain.nabu.service.TierUpdater
import com.blockchain.testutils.usd
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.KycNavXmlDirections
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.kyc.reentry.KycNavigator

class KycTierSplashPresenterTest {

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
    }

    @Test
    fun `on tier1 selected`() {
        val view: KycTierSplashView = mock()
        val tierUpdater: TierUpdater = givenTierUpdater()
        KycTierSplashPresenter(tierUpdater, givenTiers(), givenRedirect(email()))
            .also {
                it.initView(view)
                it.onViewResumed()
            }
            .tier1Selected()
        verify(view).navigateTo(email(), 1)
        verify(tierUpdater).setUserTier(1)
    }

    @Test
    fun `on tier1 selected - error setting tier`() {
        val view: KycTierSplashView = mock()
        val tierUpdater: TierUpdater = givenUnableToSetTier()
        KycTierSplashPresenter(tierUpdater, givenTiers(), givenRedirect(email()))
            .also {
                it.initView(view)
                it.onViewResumed()
            }
            .tier1Selected()
        verify(tierUpdater).setUserTier(1)
        verify(view, never()).navigateTo(any(), any())
        verify(view).showError(R.string.kyc_non_specific_server_error)
    }

    @Test
    fun `on tier1 selected but tier 1 is verified`() {
        val view: KycTierSplashView = mock()
        val tierUpdater: TierUpdater = givenTierUpdater()
        KycTierSplashPresenter(
            tierUpdater,
            givenTiers(
                tiers(
                    KycTierState.Verified to 1000.usd(),
                    KycTierState.None to 25000.usd()
                )
            ),
            givenRedirect(mobile())
        ).also {
            it.initView(view)
            it.onViewResumed()
        }.tier1Selected()
        verify(view, never()).navigateTo(any(), any())
        verify(tierUpdater, never()).setUserTier(any())
    }

    @Test
    fun `on tier2 selected`() {
        val view: KycTierSplashView = mock()
        val tierUpdater: TierUpdater = givenTierUpdater()
        KycTierSplashPresenter(tierUpdater, givenTiers(), givenRedirect(veriff()))
            .also {
                it.initView(view)
                it.onViewResumed()
            }
            .tier2Selected()
        verify(view).navigateTo(veriff(), 2)
        verify(tierUpdater).setUserTier(2)
    }

    @Test
    fun `on tier2 selected - error setting tier`() {
        val view: KycTierSplashView = mock()
        val tierUpdater: TierUpdater = givenUnableToSetTier()
        KycTierSplashPresenter(tierUpdater, givenTiers(), givenRedirect(veriff()))
            .also {
                it.initView(view)
                it.onViewResumed()
            }
            .tier2Selected()
        verify(tierUpdater).setUserTier(2)
        verify(view, never()).navigateTo(any(), any())
        verify(view).showError(R.string.kyc_non_specific_server_error)
    }

    @Test
    fun `on tier2 selected but tier 2 is verified`() {
        val view: KycTierSplashView = mock()
        val tierUpdater: TierUpdater = givenTierUpdater()
        KycTierSplashPresenter(
            tierUpdater,
            givenTiers(
                tiers(
                    KycTierState.None to 1000.usd(),
                    KycTierState.Verified to 25000.usd()
                )
            ),
            mock()
        ).also {
            it.initView(view)
            it.onViewResumed()
        }.tier2Selected()
        verify(view, never()).navigateTo(any(), any())
        verify(tierUpdater, never()).setUserTier(any())
    }

    private fun givenTierUpdater(): TierUpdater =
        mock {
            on { setUserTier(any()) }.thenReturn(Completable.complete())
        }

    private fun givenUnableToSetTier(): TierUpdater =
        mock {
            on { setUserTier(any()) }.thenReturn(Completable.error(Throwable()))
        }
}

private fun givenTiers(tiers: KycTiers? = null): TierService =
    mock {
        on { tiers() }.thenReturn(
            Single.just(
                tiers ?: tiers(
                    KycTierState.None to 1000.usd(),
                    KycTierState.None to 25000.usd()
                )
            )
        )
    }

private fun tiers(tier1: Pair<KycTierState, FiatValue>, tier2: Pair<KycTierState, FiatValue>) =
    KycTiers(
        Tiers(
            mapOf(
                KycTierLevel.BRONZE to
                    Tier(
                        KycTierState.Verified,
                        Limits(null, null)
                    ),
                KycTierLevel.SILVER to
                    Tier(
                        tier1.first,
                        Limits(
                            null,
                            Money.fromMinor(tier1.second.currency, tier1.second.toBigInteger())
                        )
                    ),
                KycTierLevel.GOLD to
                    Tier(
                        tier2.first,
                        Limits(
                            null,
                            Money.fromMinor(tier2.second.currency, tier2.second.toBigInteger())
                        )
                    )
            )
        )
    )

private fun email(): NavDirections = KycNavXmlDirections.actionStartEmailVerification(true)
private fun mobile(): NavDirections = KycNavXmlDirections.actionStartMobileVerification("DE")
private fun veriff(): NavDirections = KycNavXmlDirections.actionStartVeriff("DE")

private fun givenRedirect(email: NavDirections): KycNavigator =
    mock {
        on {
            findNextStep()
        }.thenReturn(Single.just(email))
    }
