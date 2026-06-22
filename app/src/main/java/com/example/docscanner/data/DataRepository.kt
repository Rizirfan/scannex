package com.example.docscanner.data

import com.example.docscanner.DocumentManager
import com.example.docscanner.ScannedDoc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface DataRepository {
    val data: Flow<List<ScannedDoc>>
    fun refresh()
}

class DefaultDataRepository(private val documentManager: DocumentManager) : DataRepository {
    private val _data = MutableStateFlow<List<ScannedDoc>>(emptyList())
    override val data: Flow<List<ScannedDoc>> = _data.asStateFlow()

    init {
        refresh()
    }

    override fun refresh() {
        CoroutineScope(Dispatchers.IO).launch {
            val docs = documentManager.getDocuments()
            _data.value = docs
        }
    }
}
