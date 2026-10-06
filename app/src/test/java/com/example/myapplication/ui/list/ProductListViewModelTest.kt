package com.example.myapplication.ui.list

import app.cash.turbine.test
import com.example.myapplication.domain.ProductRepository
import com.example.myapplication.domain.model.Product
import com.example.myapplication.testutil.MainDispatcherRule
import com.example.myapplication.testutil.testProduct
import com.example.myapplication.ui.common.ErrorReason
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ProductListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: ProductRepository = mockk()

    @Test
    fun `emits Loading then Success with mapped items`() = runTest {
        val deferred = CompletableDeferred<Result<List<Product>>>()
        coEvery { repository.getProducts(any()) } coAnswers { deferred.await() }

        val viewModel = ProductListViewModel(repository)
        viewModel.uiState.test {
            assertEquals(ProductListUiState.Loading, awaitItem())
            deferred.complete(
                Result.success(
                    listOf(
                        testProduct(id = 1, compareAtPrice = 1500),
                        testProduct(id = 2, images = emptyList()),
                    )
                )
            )
            val success = awaitItem() as ProductListUiState.Success
            assertEquals(listOf(1L, 2L), success.products.map { it.id })
            assertEquals("£10.00", success.products[0].price)
            assertEquals("£15.00", success.products[0].originalPrice)
            assertEquals(listOf("new"), success.products[0].labels.map { it.key })
            assertNull("missing image is null, UI shows placeholder", success.products[1].imageUrl)
        }
    }

    @Test
    fun `empty list emits Empty`() = runTest {
        coEvery { repository.getProducts(any()) } returns Result.success(emptyList())
        assertEquals(ProductListUiState.Empty, ProductListViewModel(repository).uiState.value)
    }

    @Test
    fun `failures map to error reasons`() = runTest {
        coEvery { repository.getProducts(any()) } returns Result.failure(IOException())
        assertEquals(
            ProductListUiState.Error(ErrorReason.NETWORK),
            ProductListViewModel(repository).uiState.value,
        )

        coEvery { repository.getProducts(any()) } returns Result.failure(SerializationException())
        assertEquals(
            ProductListUiState.Error(ErrorReason.DATA),
            ProductListViewModel(repository).uiState.value,
        )
    }

    @Test
    fun `retry forces refresh and recovers from error`() = runTest {
        coEvery { repository.getProducts(false) } returns Result.failure(IOException())
        coEvery { repository.getProducts(true) } returns Result.success(listOf(testProduct()))

        val viewModel = ProductListViewModel(repository)
        assertTrue(viewModel.uiState.value is ProductListUiState.Error)

        viewModel.retry()

        assertTrue(viewModel.uiState.value is ProductListUiState.Success)
        coVerify(exactly = 1) { repository.getProducts(true) }
    }

    @Test
    fun `failed refresh keeps content and reports error once`() = runTest {
        coEvery { repository.getProducts(false) } returns Result.success(listOf(testProduct()))
        coEvery { repository.getProducts(true) } returns Result.failure(IOException())

        val viewModel = ProductListViewModel(repository)
        viewModel.refresh()

        val state = viewModel.uiState.value as ProductListUiState.Success
        assertEquals(1, state.products.size)
        assertEquals(false, state.isRefreshing)
        assertEquals(ErrorReason.NETWORK, state.refreshError)

        viewModel.onRefreshErrorShown()
        assertNull((viewModel.uiState.value as ProductListUiState.Success).refreshError)
    }

    @Test
    fun `successful refresh replaces content`() = runTest {
        coEvery { repository.getProducts(false) } returns Result.success(listOf(testProduct(id = 1)))
        coEvery { repository.getProducts(true) } returns Result.success(listOf(testProduct(id = 7)))

        val viewModel = ProductListViewModel(repository)
        viewModel.refresh()

        val state = viewModel.uiState.value as ProductListUiState.Success
        assertEquals(listOf(7L), state.products.map { it.id })
        assertEquals(false, state.isRefreshing)
    }
}
