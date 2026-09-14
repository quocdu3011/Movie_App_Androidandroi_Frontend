package com.example.movieapp.feature.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.movieapp.core.ui.component.ErrorView
import com.example.movieapp.core.ui.component.LoadingIndicator
import com.example.movieapp.core.ui.component.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is SettingsUiState.LoggedOut) {
            onLoggedOut()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt tài khoản") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is SettingsUiState.Loading -> {
                    LoadingIndicator(modifier = Modifier.fillMaxSize())
                }

                is SettingsUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Thông tin tài khoản",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                state.session.fullName?.let {
                                    Text("Họ và tên: $it", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                state.session.email?.let {
                                    Text("Email: $it", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                state.session.role?.let {
                                    Text("Vai trò: $it", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                state.session.userId?.let {
                                    Text("Mã người dùng: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        PrimaryButton(
                            text = "Đăng xuất",
                            onClick = viewModel::logout,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                is SettingsUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        requestId = state.requestId,
                        onRetry = viewModel::loadSession
                    )
                }

                is SettingsUiState.LoggedOut -> {}
            }
        }
    }
}
