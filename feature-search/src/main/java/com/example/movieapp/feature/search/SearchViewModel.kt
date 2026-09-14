package com.example.movieapp.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.movieapp.core.common.Result
import com.example.movieapp.domain.model.Movie
import com.example.movieapp.domain.store.CurrentProfileStore
import com.example.movieapp.domain.usecase.BrowseMoviesUseCase
import com.example.movieapp.domain.usecase.SearchMoviesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMoviesUseCase: SearchMoviesUseCase,
    private val browseMoviesUseCase: BrowseMoviesUseCase,
    private val currentProfileStore: CurrentProfileStore,
    private val searchHistoryStorage: SearchHistoryStorage
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow(SearchFilter())
    val filter: StateFlow<SearchFilter> = _filter.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    val searchHistory: StateFlow<List<String>> = searchHistoryStorage.getHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: Job? = null

    init {
        _query
            .debounce(300)
            .distinctUntilChanged()
            .onEach { q ->
                if (q.isNotBlank()) {
                    performSearch(q, page = 1)
                } else if (!_filter.value.isActive) {
                    _uiState.value = SearchUiState.History(searchHistory.value)
                }
            }
            .launchIn(viewModelScope)

        searchHistory.onEach { history ->
            if (_query.value.isBlank() && !_filter.value.isActive) {
                _uiState.value = SearchUiState.History(history)
            }
        }.launchIn(viewModelScope)
    }

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        if (newQuery.isBlank() && !_filter.value.isActive) {
            _uiState.value = SearchUiState.History(searchHistory.value)
        }
    }

    fun onFilterChanged(newFilter: SearchFilter) {
        _filter.value = newFilter
        if (newFilter.isActive) {
            performBrowse(page = 1)
        } else if (_query.value.isNotBlank()) {
            performSearch(_query.value, page = 1)
        } else {
            _uiState.value = SearchUiState.History(searchHistory.value)
        }
    }

    fun onHistoryItemClick(historyQuery: String) {
        _query.value = historyQuery
        performSearch(historyQuery, page = 1)
    }

    fun onRemoveHistoryItem(historyQuery: String) {
        viewModelScope.launch {
            searchHistoryStorage.removeQuery(historyQuery)
        }
    }

    fun onClearHistory() {
        viewModelScope.launch {
            searchHistoryStorage.clearHistory()
        }
    }

    fun loadMore() {
        val currentState = _uiState.value
        if (currentState !is SearchUiState.Success || !currentState.canLoadMore || currentState.isLoadingMore) {
            return
        }

        val nextPage = currentState.page + 1
        _uiState.value = currentState.copy(isLoadingMore = true)

        viewModelScope.launch {
            val profileId = currentProfileStore.currentProfileId.value
            val result = if (currentState.isBrowseMode || _filter.value.isActive) {
                val f = _filter.value
                browseMoviesUseCase(
                    page = nextPage,
                    pageSize = 24,
                    genre = f.genre,
                    country = f.country,
                    year = f.year,
                    type = f.type,
                    contentKind = f.contentKind,
                    sourceType = f.sourceType,
                    provider = f.provider,
                    sort = f.sort,
                    profileId = profileId
                )
            } else {
                searchMoviesUseCase(
                    q = _query.value,
                    page = nextPage,
                    pageSize = 24,
                    profileId = profileId
                )
            }

            when (result) {
                is Result.Success -> {
                    val newItems = result.data
                    val updatedMovies = currentState.movies + newItems
                    _uiState.value = currentState.copy(
                        movies = updatedMovies,
                        page = nextPage,
                        canLoadMore = newItems.size >= 24,
                        isLoadingMore = false
                    )
                }
                is Result.Error -> {
                    _uiState.value = currentState.copy(isLoadingMore = false)
                }
            }
        }
    }

    private fun performSearch(q: String, page: Int) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            searchHistoryStorage.addQuery(q)
            val profileId = currentProfileStore.currentProfileId.value
            when (val result = searchMoviesUseCase(q = q, page = page, pageSize = 24, profileId = profileId)) {
                is Result.Success -> {
                    _uiState.value = SearchUiState.Success(
                        movies = result.data,
                        page = page,
                        canLoadMore = result.data.size >= 24,
                        isBrowseMode = false
                    )
                }
                is Result.Error -> {
                    _uiState.value = SearchUiState.Error(
                        message = result.message,
                        code = result.code,
                        requestId = result.requestId
                    )
                }
            }
        }
    }

    private fun performBrowse(page: Int) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value = SearchUiState.Loading
            val profileId = currentProfileStore.currentProfileId.value
            val f = _filter.value
            when (val result = browseMoviesUseCase(
                page = page,
                pageSize = 24,
                genre = f.genre,
                country = f.country,
                year = f.year,
                type = f.type,
                contentKind = f.contentKind,
                sourceType = f.sourceType,
                provider = f.provider,
                sort = f.sort,
                profileId = profileId
            )) {
                is Result.Success -> {
                    _uiState.value = SearchUiState.Success(
                        movies = result.data,
                        page = page,
                        canLoadMore = result.data.size >= 24,
                        isBrowseMode = true
                    )
                }
                is Result.Error -> {
                    _uiState.value = SearchUiState.Error(
                        message = result.message,
                        code = result.code,
                        requestId = result.requestId
                    )
                }
            }
        }
    }
}
