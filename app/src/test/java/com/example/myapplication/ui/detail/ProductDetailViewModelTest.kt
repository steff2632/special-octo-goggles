package com.example.myapplication.ui.detail

import androidx.lifecycle.SavedStateHandle
import com.example.myapplication.domain.ProductNotFoundException
import com.example.myapplication.domain.ProductRepository
import com.example.myapplication.testutil.MainDispatcherRule
import com.example.myapplication.testutil.testProduct
import com.example.myapplication.ui.common.ErrorReason
import com.example.myapplication.ui.navigation.ProductDetailRoute
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class ProductDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: ProductRepository = mockk()

    private fun viewModel(id: Long? = 1L) = ProductDetailViewModel(
        SavedStateHandle(if (id == null) emptyMap() else mapOf(ProductDetailRoute.ARG_PRODUCT_ID to id)),
        repository,
    )

    @Test
    fun `loads product and prepares display data`() = runTest {
        coEvery { repository.getProduct(1L) } returns
            Result.success(testProduct(id = 1L, price = 800, compareAtPrice = 1000))

        val state = viewModel().uiState.value as ProductDetailUiState.Success

        assertEquals(1L, state.detail.product.id)
        assertEquals("£8.00", state.detail.price)
        assertEquals("£10.00", state.detail.originalPrice)
        assertEquals("<p>Hello</p>", state.detail.descriptionHtml)
    }

    @Test
    fun `no original price when not discounted`() = runTest {
        coEvery { repository.getProduct(1L) } returns
            Result.success(testProduct(price = 1000, compareAtPrice = 1000))

        val state = viewModel().uiState.value as ProductDetailUiState.Success
        assertNull(state.detail.originalPrice)
    }

    @Test
    fun `unknown product shows not found`() = runTest {
        coEvery { repository.getProduct(1L) } returns Result.failure(ProductNotFoundException(1L))
        assertEquals(ProductDetailUiState.Error(ErrorReason.NOT_FOUND), viewModel().uiState.value)
    }

    @Test
    fun `missing id argument shows not found without calling repository`() = runTest {
        assertEquals(ProductDetailUiState.Error(ErrorReason.NOT_FOUND), viewModel(id = null).uiState.value)
        coVerify(exactly = 0) { repository.getProduct(any()) }
    }

    @Test
    fun `retry recovers after network error`() = runTest {
        coEvery { repository.getProduct(1L) } returnsMany listOf(
            Result.failure(IOException()),
            Result.success(testProduct()),
        )

        val viewModel = viewModel()
        assertEquals(ProductDetailUiState.Error(ErrorReason.NETWORK), viewModel.uiState.value)

        viewModel.retry()
        assertTrue(viewModel.uiState.value is ProductDetailUiState.Success)
    }
}
