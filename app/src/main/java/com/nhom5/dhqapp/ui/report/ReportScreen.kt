package com.nhom5.dhqapp.ui.report

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nhom5.dhqapp.data.AppDatabase
import com.nhom5.dhqapp.ui.theme.BluePrimary
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

private val ScreenBg = Color(0xFFF6F7FB)
private val LavenderCardBg = Color(0xFFECE6F0)
private val SelectedChipBg = Color(0xFFEADDFF)
private val OrangeBannerBg = Color(0xFFFFF3E0)
private val OrangeBannerBorder = Color(0xFFFFB74D)
private val OrangeStarBg = Color(0xFFFFE0B2)
private val OrangeStarTint = Color(0xFFF57C00)
private val ReportGreen = Color(0xFF2E7D32)
private val ReportRed = Color(0xFFE53935)
private val DonutBlue = Color(0xFF1E88E5)
private val DonutGreen = Color(0xFF43A047)

private val donutPalette = listOf(
    DonutBlue,
    DonutGreen,
    Color(0xFFFF9800),
    Color(0xFF8E24AA),
    Color(0xFFE53935),
    Color(0xFF00ACC1)
)

private fun fmtVnd(v: Double): String =
    NumberFormat.getNumberInstance(Locale("vi", "VN")).format(v.toLong()) + " đ"

private fun fmtShortK(v: Double): String {
    if (v <= 0.0) return "0"
    val k = v / 1000.0
    return if (k >= 1000.0) {
        val m = k / 1000.0
        if (m % 1.0 == 0.0) "${m.toInt()}tr" else String.format(Locale.US, "%.1ftr", m)
    } else {
        if (k % 1.0 == 0.0) "${k.toInt()}k" else String.format(Locale.US, "%.1fk", k)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(userId: Int, db: AppDatabase) {
    val viewModel: ReportViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ReportViewModel(userId, db) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.MONTH) }

    // Tab chuyển đổi độc lập cho 2 biểu đồ
    var donutType by remember { mutableStateOf("CHI") } // mặc định "Chi" giống Figma
    var barType by remember { mutableStateOf("THU") }   // mặc định "Thu" giống Figma

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.load(selectedPeriod)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = ScreenBg,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Báo cáo thống kê",
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1F2937)
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(10.dp))

            // 1. Thanh 4 nút chọn kỳ: Ngày | Tuần | Tháng | Năm
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportPeriod.entries.forEach { period ->
                    val isSelected = selectedPeriod == period
                    Surface(
                        onClick = {
                            selectedPeriod = period
                            viewModel.load(period)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) SelectedChipBg else Color.White,
                        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF9CA3AF))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = period.label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = Color(0xFF1F2937)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            when (val state = uiState) {
                is ReportUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is ReportUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(state.message)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { viewModel.load(selectedPeriod) }) {
                                Text("Thử lại")
                            }
                        }
                    }
                }

                is ReportUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 2. Hai thẻ Tổng thu & Tổng chi
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = LavenderCardBg)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        "Tổng thu",
                                        fontSize = 12.sp,
                                        color = Color(0xFF4B5563)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = fmtVnd(state.totalIncome),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ReportGreen
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = LavenderCardBg)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(
                                        "Tổng chi",
                                        fontSize = 12.sp,
                                        color = Color(0xFF4B5563)
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = fmtVnd(state.totalExpense),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ReportRed
                                    )
                                }
                            }
                        }

                        // 3. Banner "Danh mục chi nhiều nhất"
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = OrangeBannerBg,
                            border = BorderStroke(1.dp, OrangeBannerBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(OrangeStarBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = OrangeStarTint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Danh mục chi nhiều nhất",
                                        fontSize = 11.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    val top = state.topExpenseCategory
                                    Text(
                                        text = if (top != null) {
                                            val pct = String.format(Locale.US, "%.1f", top.percent * 100f)
                                            "${top.name} · ${fmtVnd(top.amount)} ($pct%)"
                                        } else {
                                            "Chưa có khoản chi trong kỳ này"
                                        },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F2937)
                                    )
                                }
                            }
                        }

                        // 4. Thẻ "Cơ cấu theo danh mục" (Biểu đồ Donut)
                        CategoryDonutCard(
                            selectedType = donutType,
                            onTypeChange = { donutType = it },
                            breakdown = if (donutType == "CHI") state.expenseBreakdown else state.incomeBreakdown,
                            totalAmount = if (donutType == "CHI") state.totalExpense else state.totalIncome
                        )

                        // 5. Thẻ "Biểu đồ cột theo [Kỳ]"
                        PeriodBarChartCard(
                            period = state.period,
                            selectedType = barType,
                            onTypeChange = { barType = it },
                            points = if (barType == "THU") state.incomeBars else state.expenseBars
                        )

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// THẺ BIỂU ĐỒ DONUT: CƠ CẤU THEO DANH MỤC
@Composable
private fun CategoryDonutCard(
    selectedType: String,
    onTypeChange: (String) -> Unit,
    breakdown: List<CategoryStat>,
    totalAmount: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cơ cấu theo danh mục",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )

                // Nút chuyển Thu / Chi
                SegmentedTypeToggle(
                    selectedType = selectedType,
                    onTypeChange = onTypeChange,
                    activeColor = BluePrimary
                )
            }

            Spacer(Modifier.height(18.dp))

            if (breakdown.isEmpty() || totalAmount <= 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedType == "CHI") "Chưa có dữ liệu khoản chi" else "Chưa có dữ liệu khoản thu",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Vẽ biểu đồ Donut bằng Canvas
                    Box(
                        modifier = Modifier.size(145.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 28.dp.toPx()
                            val diameter = size.minDimension - strokeWidth
                            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
                            var startAngle = -90f

                            breakdown.forEachIndexed { index, stat ->
                                val sweep = stat.percent * 360f
                                drawArc(
                                    color = donutPalette[index % donutPalette.size],
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = Size(diameter, diameter),
                                    style = Stroke(width = strokeWidth)
                                )
                                startAngle += sweep
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (selectedType == "CHI") "Tổng chi" else "Tổng thu",
                                fontSize = 11.sp,
                                color = Color(0xFF6B7280)
                            )
                            Text(
                                text = fmtVnd(totalAmount),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1F2937)
                            )
                        }
                    }

                    Spacer(Modifier.width(18.dp))

                    // Danh sách chú thích danh mục bên phải
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        breakdown.take(4).forEachIndexed { index, stat ->
                            val pct = String.format(Locale.US, "%.1f", stat.percent * 100f)
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 3.dp)
                                        .size(12.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(donutPalette[index % donutPalette.size])
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = stat.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1F2937),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${fmtVnd(stat.amount)} ($pct%)",
                                        fontSize = 12.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Dòng gợi ý ở cuối thẻ
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ReportGreen)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (selectedType == "CHI") {
                        "Chuyển sang tab \"Thu\" để xem tỷ lệ các danh mục thu nhập đã thêm (Lương, Thu khác...)"
                    } else {
                        "Chuyển sang tab \"Chi\" để xem tỷ lệ các danh mục chi tiêu đã thêm (Ăn uống, Giải trí...)"
                    },
                    fontSize = 12.sp,
                    color = Color(0xFF4B5563),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// THẺ BIỂU ĐỒ CỘT THEO KỲ (NGÀY / TUẦN / THÁNG / NĂM)
@Composable
private fun PeriodBarChartCard(
    period: ReportPeriod,
    selectedType: String,
    onTypeChange: (String) -> Unit,
    points: List<BarPoint>
) {
    val activeBarColor = if (selectedType == "THU") ReportGreen else ReportRed
    val softBarColor = if (selectedType == "THU") Color(0xFFA5D6A7) else Color(0xFFEF9A9A)

    val subtitle = when (period) {
        ReportPeriod.DAY -> "Số tiền ${if (selectedType == "THU") "Thu" else "Chi"} theo các khung giờ trong ngày (Đơn vị: nghìn đ)"
        ReportPeriod.WEEK -> "Số tiền ${if (selectedType == "THU") "Thu" else "Chi"} theo các ngày trong tuần (Đơn vị: nghìn đ)"
        ReportPeriod.MONTH -> "Số tiền ${if (selectedType == "THU") "Thu" else "Chi"} theo các tuần trong tháng (Đơn vị: nghìn đ)"
        ReportPeriod.YEAR -> "Số tiền ${if (selectedType == "THU") "Thu" else "Chi"} theo các quý trong năm (Đơn vị: nghìn đ)"
    }

    val rawMax = points.maxOfOrNull { it.amount } ?: 0.0
    val step = when {
        rawMax <= 0.0 -> 40_000.0
        rawMax <= 120_000.0 -> 40_000.0
        else -> ceil((rawMax / 3.0) / 10_000.0) * 10_000.0
    }
    val maxScale = step * 3.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Biểu đồ cột theo ${period.label}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )

                SegmentedTypeToggle(
                    selectedType = selectedType,
                    onTypeChange = onTypeChange,
                    activeColor = activeBarColor
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )

            Spacer(Modifier.height(16.dp))

            // Vùng vẽ trục tung + lưới nét đứt + các cột
            val chartHeight = 140.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight + 28.dp)
            ) {
                // Nhãn trục Y bên trái (maxScale -> 0)
                Column(
                    modifier = Modifier
                        .height(chartHeight)
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(fmtShortK(maxScale), fontSize = 11.sp, color = Color(0xFF9CA3AF))
                    Text(fmtShortK(step * 2), fontSize = 11.sp, color = Color(0xFF9CA3AF))
                    Text(fmtShortK(step), fontSize = 11.sp, color = Color(0xFF9CA3AF))
                    Text("0", fontSize = 11.sp, color = Color(0xFF9CA3AF))
                }

                // Khung lưới và các cột
                Box(modifier = Modifier.weight(1f)) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(chartHeight)
                    ) {
                        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        for (i in 0..2) {
                            val y = size.height * (i / 3f)
                            drawLine(
                                color = Color(0xFFE5E7EB),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = dash
                            )
                        }
                        // Đường trục đáy liền nét
                        drawLine(
                            color = Color(0xFF9CA3AF),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val maxPointVal = points.maxOfOrNull { it.amount } ?: 0.0
                        points.forEach { pt ->
                            val isHighest = pt.amount > 0.0 && pt.amount == maxPointVal
                            val ratio = if (maxScale > 0) (pt.amount / maxScale).toFloat().coerceIn(0f, 1f) else 0f
                            val barHeight = if (pt.amount <= 0.0) 4.dp else max(12f, chartHeight.value * ratio * 0.82f).dp

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.height(chartHeight),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = fmtShortK(pt.amount),
                                            fontSize = 11.sp,
                                            fontWeight = if (isHighest) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isHighest) activeBarColor else Color(0xFF9CA3AF)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(if (points.size > 5) 20.dp else 32.dp)
                                                .height(barHeight)
                                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                                .background(
                                                    when {
                                                        pt.amount <= 0.0 -> Color(0xFFE5E7EB)
                                                        isHighest -> activeBarColor
                                                        else -> softBarColor
                                                    }
                                                )
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = pt.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isHighest) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isHighest) Color(0xFF1F2937) else Color(0xFF6B7280)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = if (selectedType == "THU") {
                    "Chọn \"Chi\" ở góc phải thẻ để xem biểu đồ cột khoản chi tương tự."
                } else {
                    "Chọn \"Thu\" ở góc phải thẻ để xem biểu đồ cột khoản thu tương tự."
                },
                fontSize = 12.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}

// NÚT CHUYỂN THU / CHI DẠNG SEGMENTED PILL Ở GÓC PHẢI THẺ
@Composable
private fun SegmentedTypeToggle(
    selectedType: String,
    onTypeChange: (String) -> Unit,
    activeColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFF1F3F6))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("THU" to "Thu", "CHI" to "Chi").forEach { (code, label) ->
            val selected = selectedType == code
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) activeColor else Color.Transparent)
                    .clickable { onTypeChange(code) }
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) Color.White else Color(0xFF4B5563)
                )
            }
        }
    }
}