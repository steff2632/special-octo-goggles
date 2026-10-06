package com.example.myapplication.data

import com.example.myapplication.data.remote.ProductApi
import com.example.myapplication.di.NetworkModule
import com.example.myapplication.domain.ProductNotFoundException
import com.example.myapplication.testutil.readResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException

/** Exercises Retrofit + kotlinx.serialization + repository against a local MockWebServer. */
class ProductRepositoryImplTest {

    private val server = MockWebServer()
    private val json: Json = NetworkModule.provideJson()
    private lateinit var repository: ProductRepositoryImpl

    @Before
    fun setUp() {
        server.start()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ProductApi::class.java)
        repository = ProductRepositoryImpl(api, json, ProductMapper(), Dispatchers.IO)
    }

    @After
    fun tearDown() = server.close()

    private fun enqueueJson(body: String, code: Int = 200) =
        server.enqueue(MockResponse.Builder().code(code).body(body).build())

    @Test
    fun `requests the expected endpoint path`() = runTest {
        enqueueJson(readResource("products.json"))
        repository.getProducts()

        assertEquals(
            "/training/mock-product-responses/algolia-example-payload.json",
            server.takeRequest().url.encodedPath,
        )
    }

    @Test
    fun `parses valid products and skips malformed or incomplete ones`() = runTest {
        enqueueJson(readResource("products.json"))

        val products = repository.getProducts().getOrThrow()

        assertEquals(listOf(1L, 2L, 3L), products.map { it.id })
        assertEquals(listOf("going-fast", "new"), products[0].labels.map { it.key })
        assertEquals(2, products[0].images.size)
        assertNull("missing images handled", products[1].primaryImage)
        assertEquals("https://cdn.example.com/c.jpg", products[2].primaryImage?.url)
    }

    @Test
    fun `caches results and only refetches when forced`() = runTest {
        enqueueJson(readResource("products.json"))
        enqueueJson("""{"hits":[]}""")

        repository.getProducts()
        repository.getProducts()
        assertEquals(1, server.requestCount)

        val refreshed = repository.getProducts(forceRefresh = true).getOrThrow()
        assertEquals(2, server.requestCount)
        assertTrue(refreshed.isEmpty())
    }

    @Test
    fun `getProduct returns cached product by id or not found`() = runTest {
        enqueueJson(readResource("products.json"))

        assertEquals("No Image Product", repository.getProduct(2).getOrThrow().title)
        assertTrue(repository.getProduct(999).exceptionOrNull() is ProductNotFoundException)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `missing hits yields empty list`() = runTest {
        enqueueJson("{}")
        assertTrue(repository.getProducts().getOrThrow().isEmpty())
    }

    @Test
    fun `http error is returned as failure and not cached`() = runTest {
        enqueueJson("oops", code = 500)
        enqueueJson(readResource("products.json"))

        assertTrue(repository.getProducts().isFailure)
        assertEquals(3, repository.getProducts().getOrThrow().size)
    }

    @Test
    fun `invalid json body is returned as failure`() = runTest {
        enqueueJson("not json")
        assertTrue(repository.getProducts().isFailure)
    }

    @Test
    fun `network failure is returned as IOException`() = runTest {
        server.close()
        assertTrue(repository.getProducts().exceptionOrNull() is IOException)
    }
}
