package com.remotemanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.remotemanager.R
import com.remotemanager.data.model.ConnectionType
import com.remotemanager.data.model.Server
import com.remotemanager.ui.theme.NeonBlue
import com.remotemanager.ui.theme.NeonCyan
import com.remotemanager.ui.theme.NeonGreen
import com.remotemanager.ui.theme.NeonPink
import com.remotemanager.ui.theme.NeonPurple
import com.remotemanager.ui.theme.TechBorder
import com.remotemanager.ui.theme.TechPanel
import com.remotemanager.ui.theme.TechSurface
import com.remotemanager.ui.theme.TextSecondary
import com.remotemanager.ui.viewmodel.ServerListViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerListScreen(
    selectedServerId: Long = 0L,
    onServerClick: (Long) -> Unit,
    onAddServer: () -> Unit,
    viewModel: ServerListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name).uppercase(),
                        color = NeonCyan
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = TechPanel,
                    titleContentColor = NeonCyan
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddServer,
                containerColor = NeonCyan,
                contentColor = Color.Black,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_server))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChanged,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            FilterChips(
                selectedType = uiState.selectedType,
                onTypeSelected = viewModel::onTypeSelected,
                selectedGroup = uiState.selectedGroup,
                groups = uiState.groups,
                onGroupSelected = viewModel::onGroupSelected,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            if (uiState.servers.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.servers, key = { it.id }) { server ->
                        ServerCard(
                            server = server,
                            isSelected = server.id == selectedServerId,
                            onClick = { onServerClick(server.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, TechBorder, RoundedCornerShape(12.dp)),
        placeholder = { Text("搜索服务器…", color = TextSecondary) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = TechSurface,
            unfocusedContainerColor = TechSurface,
            disabledContainerColor = TechSurface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun FilterChips(
    selectedType: ConnectionType?,
    onTypeSelected: (ConnectionType?) -> Unit,
    selectedGroup: String?,
    groups: List<String>,
    onGroupSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TechFilterChip(
                selected = selectedType == null,
                onClick = { onTypeSelected(null) },
                label = "全部",
                activeColor = NeonCyan
            )
            TechFilterChip(
                selected = selectedType == ConnectionType.RDP,
                onClick = { onTypeSelected(ConnectionType.RDP) },
                label = stringResource(R.string.rdp),
                activeColor = NeonPink
            )
            TechFilterChip(
                selected = selectedType == ConnectionType.SSH,
                onClick = { onTypeSelected(ConnectionType.SSH) },
                label = stringResource(R.string.ssh),
                activeColor = NeonGreen
            )
        }

        if (groups.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TechFilterChip(
                    selected = selectedGroup == null,
                    onClick = { onGroupSelected(null) },
                    label = "所有分组",
                    activeColor = NeonCyan
                )
                groups.forEach { group ->
                    TechFilterChip(
                        selected = selectedGroup == group,
                        onClick = { onGroupSelected(group) },
                        label = group,
                        activeColor = NeonPurple
                    )
                }
            }
        }
    }
}

@Composable
private fun TechFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    activeColor: Color
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = activeColor.copy(alpha = 0.15f),
            selectedLabelColor = activeColor,
            containerColor = TechSurface,
            labelColor = TextSecondary
        ),
        border = if (selected) {
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = true,
                borderColor = activeColor.copy(alpha = 0.50f),
                selectedBorderColor = activeColor.copy(alpha = 0.50f)
            )
        } else {
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = false,
                borderColor = TechBorder
            )
        }
    )
}

@Composable
private fun ServerCard(
    server: Server,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val typeTint = when (server.type) {
        ConnectionType.RDP -> NeonPink
        ConnectionType.SSH -> NeonGreen
    }
    val borderColor = if (isSelected) typeTint.copy(alpha = 0.60f) else TechBorder

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = TechSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            typeTint.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(typeTint.copy(alpha = 0.12f))
                    .border(1.dp, typeTint.copy(alpha = 0.30f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (server.type == ConnectionType.RDP) Icons.Default.Computer else Icons.Default.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = typeTint
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${server.username}@${server.host}${server.displayPort}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!server.group.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = server.group,
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonPurple,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonPurple.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Computer,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = NeonCyan.copy(alpha = 0.3f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_servers),
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
    }
}
