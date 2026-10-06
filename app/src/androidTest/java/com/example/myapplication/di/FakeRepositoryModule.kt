package com.example.myapplication.di

import com.example.myapplication.domain.ProductNotFoundException
import com.example.myapplication.domain.ProductRepository
import com.example.myapplication.domain.model.Product
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import java.io.IOException
import javax.inject.Singleton

/** In-memory repository controlled by tests; no network access. */
class FakeProductRepository : ProductRepository {
    @Volatile
    var products: List<Product> = emptyList()

    /** When set, the next N product-list loads fail with this error. */
    @Volatile
    var failuresRemaining: Int = 0

    @Volatile
    var listLoads: Int = 0

    override suspend fun getProducts(forceRefresh: Boolean): Result<List<Product>> {
        listLoads++
        if (failuresRemaining > 0) {
            failuresRemaining--
            return Result.failure(IOException("offline"))
        }
        return Result.success(products)
    }

    override suspend fun getProduct(id: Long): Result<Product> =
        products.firstOrNull { it.id == id }?.let { Result.success(it) }
            ?: Result.failure(ProductNotFoundException(id))
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [RepositoryModule::class])
object FakeRepositoryModule {
    @Provides
    @Singleton
    fun provideFakeRepository(): FakeProductRepository = FakeProductRepository()

    @Provides
    fun provideRepository(fake: FakeProductRepository): ProductRepository = fake
}
