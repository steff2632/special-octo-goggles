package com.example.myapplication.data

import com.example.myapplication.data.remote.ProductApi
import com.example.myapplication.data.remote.dto.ProductDto
import com.example.myapplication.di.IoDispatcher
import com.example.myapplication.domain.ProductNotFoundException
import com.example.myapplication.domain.ProductRepository
import com.example.myapplication.domain.model.Product
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val api: ProductApi,
    private val json: Json,
    private val mapper: ProductMapper,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ProductRepository {

    private val mutex = Mutex()
    private var cache: List<Product>? = null

    override suspend fun getProducts(forceRefresh: Boolean): Result<List<Product>> =
        mutex.withLock {
            val cached = cache
            if (!forceRefresh && cached != null) return@withLock Result.success(cached)
            safeCall { fetch() }.onSuccess { cache = it }
        }

    override suspend fun getProduct(id: Long): Result<Product> =
        getProducts().mapCatching { products ->
            products.firstOrNull { it.id == id } ?: throw ProductNotFoundException(id)
        }

    private suspend fun fetch(): List<Product> = withContext(ioDispatcher) {
        api.getProducts().hits.orEmpty()
            .mapNotNull { element ->
                // Skip individual malformed products rather than failing the whole list.
                runCatching { json.decodeFromJsonElement(ProductDto.serializer(), element) }
                    .getOrNull()
                    ?.let(mapper::map)
            }
            .distinctBy { it.id }
    }

    private inline fun <T> safeCall(block: () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
