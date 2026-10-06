package com.example.myapplication.data.remote

import com.example.myapplication.data.remote.dto.ProductResponseDto
import retrofit2.http.GET

interface ProductApi {
    @GET("training/mock-product-responses/algolia-example-payload.json")
    suspend fun getProducts(): ProductResponseDto
}
