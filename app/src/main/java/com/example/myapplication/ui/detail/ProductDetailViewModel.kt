package com.example.myapplication.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.ProductRepository
import com.example.myapplication.domain.model.Product
import com.example.myapplication.domain.util.HtmlSanitizer
import com.example.myapplication.domain.util.PriceFormatter
import com.example.myapplication.ui.common.ErrorReason
import com.example.myapplication.ui.common.toErrorReason
import com.example.myapplication.ui.navigation.ProductDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductDetail(
    val product: Product,
    val price: String?,
    val originalPrice: String?,
    val descriptionHtml: String,
)

sealed interface ProductDetailUiState {
    data object Loading : ProductDetailUiState
    data class Error(val reason: ErrorReason) : ProductDetailUiState
    data class Success(val detail: ProductDetail) : ProductDetailUiState
}

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ProductRepository,
) : ViewModel() {

    private val productId: Long? = savedStateHandle.get<Long>(ProductDetailRoute.ARG_PRODUCT_ID)

    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        val id = productId
        if (id == null) {
            _uiState.value = ProductDetailUiState.Error(ErrorReason.NOT_FOUND)
            return
        }
        loadJob?.cancel()
        _uiState.value = ProductDetailUiState.Loading
        loadJob = viewModelScope.launch {
            _uiState.value = repository.getProduct(id).fold(
                onSuccess = { ProductDetailUiState.Success(it.toDetail()) },
                onFailure = { ProductDetailUiState.Error(it.toErrorReason()) },
            )
        }
    }

    private fun Product.toDetail() = ProductDetail(
        product = this,
        price = PriceFormatter.format(priceInPence),
        originalPrice = if (isDiscounted) PriceFormatter.format(compareAtPriceInPence) else null,
        descriptionHtml = HtmlSanitizer.sanitize(descriptionHtml),
    )
}
