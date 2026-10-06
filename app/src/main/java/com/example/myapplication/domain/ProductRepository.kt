package com.example.myapplication.domain

import com.example.myapplication.domain.model.Product

interface ProductRepository {
    suspend fun getProducts(forceRefresh: Boolean = false): Result<List<Product>>

    suspend fun getProduct(id: Long): Result<Product>
}

class ProductNotFoundException(id: Long) : NoSuchElementException("Product $id not found")
