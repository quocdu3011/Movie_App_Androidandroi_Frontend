package com.example.movieapp.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.movieapp.core.ui.component.ErrorView
import com.example.movieapp.core.ui.component.LoadingIndicator
import com.example.movieapp.core.ui.component.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileFormScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProfileFormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var name by remember { mutableStateOf("") }
    var isKids by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ProfileFormUiState.Loaded -> {
                name = state.initialName
                isKids = state.initialIsKids
            }
            is ProfileFormUiState.SaveSuccess -> {
                onNavigateBack()
            }
            else -> {}
        }
    }

    val isEditMode = (uiState as? ProfileFormUiState.Loaded)?.isEditMode == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Chỉnh sửa hồ sơ" else "Tạo hồ sơ mới") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (val state = uiState) {
                is ProfileFormUiState.Loading -> {
                    LoadingIndicator(modifier = Modifier.fillMaxSize())
                }

                is ProfileFormUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        requestId = state.requestId,
                        onRetry = { viewModel.loadProfileForForm(null) }
                    )
                }

                else -> {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên hồ sơ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Hồ sơ dành cho trẻ em", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Chỉ hiển thị nội dung phù hợp với độ tuổi trẻ em (Kids Safe)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isKids,
                            onCheckedChange = { isKids = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    PrimaryButton(
                        text = if (isEditMode) "Lưu thay đổi" else "Tạo hồ sơ",
                        onClick = { viewModel.saveProfile(name, null, isKids) },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
