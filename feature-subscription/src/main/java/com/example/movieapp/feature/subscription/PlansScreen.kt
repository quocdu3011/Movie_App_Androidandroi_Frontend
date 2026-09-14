package com.example.movieapp.feature.subscription

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.movieapp.core.ui.component.ErrorView
import com.example.movieapp.core.ui.component.LoadingIndicator
import com.example.movieapp.core.ui.component.PrimaryButton
import com.example.movieapp.core.ui.theme.DarkBackground
import com.example.movieapp.core.ui.theme.DarkSurface
import com.example.movieapp.core.ui.theme.PrimaryCoral
import com.example.movieapp.core.ui.theme.SecondaryGold
import com.example.movieapp.core.ui.theme.TextPrimaryDark
import com.example.movieapp.core.ui.theme.TextSecondaryDark
import com.example.movieapp.domain.model.Plan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    onSubscriptionSuccess: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground),
                title = {
                    Text(
                        text = "Đăng ký gói Premium",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(DarkBackground)
        ) {
            when (val state = uiState) {
                is SubscriptionUiState.Loading -> {
                    LoadingIndicator(modifier = Modifier.fillMaxSize())
                }

                is SubscriptionUiState.PlansLoaded -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        state.currentSubscription?.let { sub ->
                            if (sub.status == "active") {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = SecondaryGold
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                "Gói hiện tại: ${sub.plan?.name ?: "Đang hoạt động"}",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimaryDark
                                            )
                                            Text(
                                                "Hạn sử dụng: ${sub.endAt}",
                                                fontSize = 13.sp,
                                                color = TextSecondaryDark
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Chọn gói dịch vụ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.plans, key = { it.id }) { plan ->
                                PlanCard(
                                    plan = plan,
                                    isSelected = plan.id == state.selectedPlanId,
                                    onSelect = { viewModel.selectPlan(plan.id) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Phương thức thanh toán",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val paymentMethods = listOf(
                            "card" to "Thẻ ngân hàng (Card)",
                            "wallet" to "Ví điện tử (Wallet)",
                            "bank_transfer" to "Chuyển khoản (Bank Transfer)"
                        )

                        paymentMethods.forEach { (code, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.selectPaymentMethod(code) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = state.selectedPaymentMethod == code,
                                    onClick = { viewModel.selectPaymentMethod(code) },
                                    colors = RadioButtonDefaults.colors(selectedColor = PrimaryCoral, unselectedColor = TextSecondaryDark)
                                )
                                Text(label, fontSize = 14.sp, color = TextPrimaryDark)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        PrimaryButton(
                            text = "Đăng ký ngay",
                            onClick = viewModel::subscribe,
                            enabled = state.selectedPlanId != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                is SubscriptionUiState.AwaitingPayment -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.height(64.dp), color = PrimaryCoral)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Đang xử lý thanh toán...",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Vui lòng chờ trong giây lát (${state.attemptCount}/24)",
                            fontSize = 13.sp,
                            color = TextSecondaryDark
                        )
                    }
                }

                is SubscriptionUiState.ActiveSuccess -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SecondaryGold,
                            modifier = Modifier.height(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Đăng ký gói thành công!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        PrimaryButton(
                            text = "Hoàn tất",
                            onClick = onSubscriptionSuccess,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                is SubscriptionUiState.PaymentPendingTimeout -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.message,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryCoral
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        PrimaryButton(
                            text = "Quay lại danh sách gói",
                            onClick = viewModel::loadPlans,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                is SubscriptionUiState.Error -> {
                    ErrorView(
                        message = state.message,
                        requestId = state.requestId,
                        onRetry = viewModel::loadPlans
                    )
                }
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: Plan,
    isSelected: Boolean,
    onSelect: () -> Unit
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
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = if (isSelected) SecondaryGold else TextSecondaryDark
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = plan.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                    Text(
                        text = "${plan.durationDays} ngày • Tối đa ${plan.maxConcurrentStreams} luồng • ${plan.maxResolution}",
                        fontSize = 13.sp,
                        color = TextSecondaryDark
                    )
                }
            }
            Text(
                text = "${plan.price} ${plan.currency}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryCoral
            )
        }
    }
}

