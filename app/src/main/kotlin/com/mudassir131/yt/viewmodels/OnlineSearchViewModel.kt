/*
 * Nocturne - by Mudassir
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mudassir131.yt.innertube.YouTube
import com.mudassir131.yt.innertube.models.filterExplicit
import com.mudassir131.yt.innertube.models.filterVideo
import com.mudassir131.yt.innertube.pages.SearchSummaryPage
import com.mudassir131.yt.innertube.pages.SearchSummary
import com.mudassir131.yt.constants.HideExplicitKey
import com.mudassir131.yt.constants.HideVideoKey
import com.mudassir131.yt.constants.ContentFilterMode
import com.mudassir131.yt.constants.ContentFilterModeKey
import com.mudassir131.yt.utils.filterYTItemsByContentMode
import com.mudassir131.yt.extensions.toEnum
import com.mudassir131.yt.models.ItemsPage
import com.mudassir131.yt.utils.dataStore
import com.mudassir131.yt.utils.get
import com.mudassir131.yt.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val query = savedStateHandle.get<String>("query")!!
    val filter = MutableStateFlow<YouTube.SearchFilter?>(null)
    
    // Raw (unfiltered) data cache
    private var rawSummaryPage: SearchSummaryPage? = null
    private val rawViewStateMap = mutableMapOf<String, ItemsPage>()

    var summaryPage by mutableStateOf<SearchSummaryPage?>(null)
        private set
    val viewStateMap = mutableStateMapOf<String, ItemsPage?>()

    init {
        viewModelScope.launch {
            filter.collect { filter ->
                if (filter == null) {
                    if (rawSummaryPage == null) {
                        YouTube
                            .searchSummary(query)
                            .onSuccess {
                                rawSummaryPage = it.filterExplicit(context.dataStore.get(HideExplicitKey, false)).filterVideo(context.dataStore.get(HideVideoKey, false))
                                applyFilters()
                            }.onFailure {
                                reportException(it)
                            }
                    }
                } else {
                    if (rawViewStateMap[filter.value] == null) {
                        YouTube
                            .search(query, filter)
                            .onSuccess { result ->
                                rawViewStateMap[filter.value] =
                                    ItemsPage(
                                        result.items
                                            .distinctBy { it.id }
                                            .filterExplicit(
                                                context.dataStore.get(
                                                    HideExplicitKey,
                                                    false
                                                )
                                            ).filterVideo(context.dataStore.get(HideVideoKey, false)),
                                        result.continuation,
                                    )
                                applyFilters()
                            }.onFailure {
                                reportException(it)
                            }
                    }
                }
            }
        }

        viewModelScope.launch {
            context.dataStore.data.map {
                it[ContentFilterModeKey].toEnum(ContentFilterMode.GLOBAL)
            }.distinctUntilChanged().collect { mode ->
                applyFilters(mode)
            }
        }
    }

    private suspend fun applyFilters(mode: ContentFilterMode? = null) {
        val activeMode = mode ?: context.dataStore.data.map {
            it[ContentFilterModeKey].toEnum(ContentFilterMode.GLOBAL)
        }.first()

        rawSummaryPage?.let { page ->
            summaryPage = SearchSummaryPage(
                summaries = page.summaries.mapNotNull { summary ->
                    val filteredItems = summary.items.filterYTItemsByContentMode(activeMode)
                    if (filteredItems.isNotEmpty() || activeMode == ContentFilterMode.GLOBAL) {
                        SearchSummary(title = summary.title, items = filteredItems)
                    } else {
                        null
                    }
                }
            )
        }

        rawViewStateMap.forEach { (key, page) ->
            viewStateMap[key] = ItemsPage(
                items = page.items.filterYTItemsByContentMode(activeMode),
                continuation = page.continuation
            )
        }
    }

    fun loadMore() {
        val filter = filter.value?.value
        viewModelScope.launch {
            if (filter == null) return@launch
            val rawViewState = rawViewStateMap[filter] ?: return@launch
            val continuation = rawViewState.continuation
            if (continuation != null) {
                val searchResult =
                    YouTube.searchContinuation(continuation).getOrNull() ?: return@launch
                rawViewStateMap[filter] = ItemsPage(
                    (rawViewState.items + searchResult.items).distinctBy { it.id },
                    searchResult.continuation
                )
                applyFilters()
            }
        }
    }
}
