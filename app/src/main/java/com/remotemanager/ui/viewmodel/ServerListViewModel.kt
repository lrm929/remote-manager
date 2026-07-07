package com.remotemanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.remotemanager.data.model.ConnectionType
import com.remotemanager.data.model.Server
import com.remotemanager.data.repository.ServerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ServerListUiState(
    val servers: List<Server> = emptyList(),
    val groups: List<String> = emptyList(),
    val selectedType: ConnectionType? = null,
    val selectedGroup: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class ServerListViewModel(
    private val repository: ServerRepository
) : ViewModel() {

    private val _allServers = MutableStateFlow<List<Server>>(emptyList())
    private val _groups = MutableStateFlow<List<String>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedType = MutableStateFlow<ConnectionType?>(null)
    private val _selectedGroup = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ServerListUiState> = combine(
        _allServers,
        _groups,
        _selectedType,
        _selectedGroup,
        _searchQuery,
        _isLoading,
        _error
    ) { allServers, groups, type, group, query, loading, error ->
        ServerListUiState(
            servers = filterServers(allServers, type, group, query),
            groups = groups,
            selectedType = type,
            selectedGroup = group,
            searchQuery = query,
            isLoading = loading,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ServerListUiState()
    )

    init {
        loadData()
    }

    private fun loadData() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                repository.getAllServers().collect { _allServers.value = it }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
        viewModelScope.launch {
            try {
                repository.getGroups().collect { _groups.value = it.filterNotNull() }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
        _isLoading.value = false
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onTypeSelected(type: ConnectionType?) {
        _selectedType.value = type
    }

    fun onGroupSelected(group: String?) {
        _selectedGroup.value = group
    }

    fun deleteServer(server: Server) {
        viewModelScope.launch {
            try {
                repository.deleteServer(server)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    private fun filterServers(
        servers: List<Server>,
        type: ConnectionType?,
        group: String?,
        query: String
    ): List<Server> {
        val q = query.trim().lowercase()
        return servers.filter { server ->
            val matchesType = type == null || server.type == type
            val matchesGroup = group == null || server.group == group
            val matchesSearch = q.isBlank() ||
                server.name.lowercase().contains(q) ||
                server.host.lowercase().contains(q) ||
                server.username.lowercase().contains(q)
            matchesType && matchesGroup && matchesSearch
        }
    }
}
