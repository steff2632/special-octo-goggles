package com.example.myapplication.ui.common

import com.example.myapplication.domain.ProductNotFoundException
import kotlinx.serialization.SerializationException
import java.io.IOException

enum class ErrorReason { NETWORK, DATA, NOT_FOUND, UNKNOWN }

fun Throwable.toErrorReason(): ErrorReason = when (this) {
    is ProductNotFoundException -> ErrorReason.NOT_FOUND
    is IOException -> ErrorReason.NETWORK
    is SerializationException, is IllegalArgumentException -> ErrorReason.DATA
    else -> ErrorReason.UNKNOWN
}
