package com.example.myapplication.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.ProductRepository
import com.example.myapplication.ui.common.toErrorReason
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val repository: ProductRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductListUiState>(ProductListUiState.Loading)
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load(forceRefresh = false)
    }

    fun retry() = load(forceRefresh = true)

    /** Pull-to-refresh: keeps current content visible while reloading. */
    fun refresh() {
        val current = _uiState.value
        if (current is ProductListUiState.Success) {
            _uiState.value = current.copy(isRefreshing = true)
            load(forceRefresh = true, showLoading = false)
        } else {
            retry()
        }
    }

    fun onRefreshErrorShown() {
        _uiState.update { state ->
            if (state is ProductListUiState.Success) state.copy(refreshError = null) else state
        }
    }

    private fun load(forceRefresh: Boolean, showLoading: Boolean = true) {
        loadJob?.cancel()
        if (showLoading) _uiState.value = ProductListUiState.Loading
        loadJob = viewModelScope.launch {
            repository.getProducts(forceRefresh)
                .onSuccess { products ->
                    _uiState.value = if (products.isEmpty()) {
                        ProductListUiState.Empty
                    } else {
                        ProductListUiState.Success(products.map { it.toListItem() })
                    }
                }
                .onFailure { error ->
                    _uiState.update { previous ->
                        // On a failed refresh keep the existing content rather than wiping it.
                        if (previous is ProductListUiState.Success) {
                            previous.copy(
                                isRefreshing = false,
                                refreshError = error.toErrorReason(),
                            )
                        } else {
                            ProductListUiState.Error(error.toErrorReason())
                        }
                    }
                }
        }
    }
}
