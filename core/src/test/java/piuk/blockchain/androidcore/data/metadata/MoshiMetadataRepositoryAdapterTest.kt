package piuk.blockchain.androidcore.data.metadata

import com.blockchain.android.testutils.rxInit
import com.blockchain.serialization.BigDecimalAdapter
import com.blockchain.serialization.JsonSerializable
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.squareup.moshi.Moshi
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Maybe
import java.math.BigDecimal
import org.amshove.kluent.`should be equal to`
import org.junit.Rule
import org.junit.Test

class MoshiMetadataRepositoryAdapterTest {

    @get:Rule
    val initSchedulers = rxInit {
        ioTrampoline()
    }

    data class ExampleClass(
        val field1: String,
        val field2: BigDecimal
    ) : JsonSerializable

    private val moshi: Moshi = Moshi.Builder().add(BigDecimalAdapter()).build()

    @Test
    fun `can save json`() {
        val metadataManager = mock<MetadataManager> {
            on { saveToMetadata(any(), any()) }.thenReturn(Completable.complete())
        }
        MoshiMetadataRepositoryAdapter(metadataManager, moshi)
            .saveMetadata(ExampleClass("ABC", 123.toBigDecimal()), ExampleClass::class.java, 100)
            .test()
            .assertComplete()

        verify(metadataManager).saveToMetadata("""{"field1":"ABC","field2":"123"}""", 100)
    }

    @Test
    fun `can load json`() {
        val metadataManager = mock<MetadataManager> {
            on { fetchMetadata(199) }.thenReturn(
                Maybe.just(
                    """{"field1":"DEF","field2":"456"}"""
                )
            )
        }
        MoshiMetadataRepositoryAdapter(metadataManager, moshi)
            .loadMetadata(199, ExampleClass::class.java)
            .test()
            .assertComplete()
            .values() `should be equal to` listOf(ExampleClass("DEF", 456.toBigDecimal()))
    }

    @Test
    fun `can load missing json`() {
        val metadataManager = mock<MetadataManager> {
            on { fetchMetadata(199) }.thenReturn(Maybe.empty())
        }
        MoshiMetadataRepositoryAdapter(metadataManager, moshi)
            .loadMetadata(199, ExampleClass::class.java)
            .test()
            .assertComplete()
            .values() `should be equal to` listOf()
    }

    @Test
    fun `bad json is an error`() {
        val metadataManager = mock<MetadataManager> {
            on { fetchMetadata(199) }.thenReturn(
                Maybe.just(
                    """{"field1":"DEF","fie..."""
                )
            )
        }
        MoshiMetadataRepositoryAdapter(metadataManager, moshi)
            .loadMetadata(199, ExampleClass::class.java)
            .test()
            .assertError(Exception::class.java)
    }
}
