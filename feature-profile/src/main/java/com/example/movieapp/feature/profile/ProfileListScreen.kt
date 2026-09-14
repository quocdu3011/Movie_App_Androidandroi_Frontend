package com.example.movieapp.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.movieapp.core.ui.component.ErrorView
import com.example.movieapp.core.ui.component.LoadingIndicator
import com.example.movieapp.core.ui.theme.DarkBackground
import com.example.movieapp.core.ui.theme.DarkSurface
import com.example.movieapp.core.ui.theme.PrimaryCoral
import com.example.movieapp.core.ui.theme.SecondaryGold
import com.example.movieapp.core.ui.theme.TextPrimaryDark
import com.example.movieapp.core.ui.theme.TextSecondaryDark
import com.example.movieapp.domain.model.Profile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    onProfileSelected: (String) -> Unit,
    onCreateProfileClick: () -> Unit,
    onEditProfileClick: (String) -> Unit,
    viewModel: ProfileListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var profileToDelete by remember { mutableStateOf<Profile?>(null) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Danh sách hồ sơ",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        floatingActionButton = {
            if (uiState is ProfileListUiState.Success) {
                val profiles = (uiState as ProfileListUiState.Success).profiles
                if (profiles.size < 5) {
                    FloatingActionButton(
                        onClick = onCreateProfileClick,
                        containerColor = PrimaryCoral,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Thêm hồ sơ")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground)
        ) {
            when (val state = uiState) {
                is ProfileListUiState.Loading -> {
                    LoadingIndicator(modifier = Modifier.fillMaxSize())
                }

                is ProfileListUiState.Success -> {
                    if (state.profiles.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Chưa có hồ sơ nào", style = MaterialTheme.typography.bodyLarge, color = TextSecondaryDark)
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = onCreateProfileClick) {
                                Text("Tạo hồ sơ ngay", color = PrimaryCoral, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.profiles, key = { it.id }) { profile ->
                                ProfileCard(
                                    profile = profile,
                                    isActive = profile.id == state.activeProfileId,
                                    onSelect = {
                                        viewModel.selectProfile(profile.id)
                                        onProfileSelected(profile.id)
                                    },
                                    onEdit = { onEditProfileClick(profile.id) },
                                    onDelete = { profileToDelete = profile }
                                )
                            }
                        }
                    }
                }

                is ProfileListUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        requestId = state.requestId,
                        onRetry = viewModel::loadProfiles
                    )
                }
            }
        }
    }

    profileToDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            containerColor = DarkSurface,
            title = { Text("Xác nhận xóa", color = TextPrimaryDark, fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa hồ sơ \"${profile.name}\" không?", color = TextSecondaryDark) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProfile(profile.id)
                        profileToDelete = null
                    }
                ) {
                    Text("Xóa", color = PrimaryCoral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text("Hủy", color = TextSecondaryDark)
                }
            }
        )
    }
}

@Composable
private fun ProfileCard(
    profile: Profile,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Large rounded square profile avatar (not round circle)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isActive) PrimaryCoral else DarkBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = if (isActive) Color.White else TextSecondaryDark
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        if (isActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Đang chọn",
                                tint = PrimaryCoral,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (profile.isKids) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Trẻ em (Kids)",
                            fontSize = 13.sp,
                            color = SecondaryGold,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Chỉnh sửa", tint = TextSecondaryDark)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = PrimaryCoral.copy(alpha = 0.8f))
                }
            }
        }
    }
}

