package com.example.docscanner

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey

@Serializable data class Sign(val filePath: String) : NavKey

