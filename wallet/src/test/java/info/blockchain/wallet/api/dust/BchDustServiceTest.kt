package info.blockchain.wallet.api.dust

import com.blockchain.testutils.rxInit
import com.blockchain.testutils.waitForCompletionWithoutErrors
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import info.blockchain.wallet.api.dust.data.DustInput
import io.reactivex.rxjava3.core.Single
import org.junit.Rule
import org.junit.Test

class BchDustServiceTest {

    @get:Rule
    val initRx = rxInit {
        ioTrampoline()
    }

    private val dustApi: DustApi = mock()
    private val apiCode = "123"
    private val subject: DustService = BchDustService(dustApi, apiCode)

    @Test
    fun `get dust returns input`() {
        val expectedDustInput: DustInput = mock()

        whenever(
            dustApi.getDust("bch", apiCode)
        ).thenReturn(
            Single.just(expectedDustInput)
        )

        subject.getDust()
            .test()
            .waitForCompletionWithoutErrors()
            .assertValue {
                it == expectedDustInput
            }
    }
}
