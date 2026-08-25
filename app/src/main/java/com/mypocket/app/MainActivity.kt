package com.mypocket.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mypocket.app.data.Movement
import com.mypocket.app.data.MovementType
import com.mypocket.app.data.PocketRepository
import com.mypocket.app.ui.theme.MyPocketTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MyPocketApp(remember { PocketRepository(applicationContext) }) }
    }
}

private val nativeLib = NativeLib()
private fun formatMoney(amount: Double) = nativeLib.formatCurrencyFallback(amount)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPocketApp(repository: PocketRepository) {
    val settings = repository.settings
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    var showSettings by remember { mutableStateOf(false) }

    MyPocketTheme(darkTheme = settings.darkTheme) {
        CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, settings.fontScale)) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("MyPocket", fontWeight = FontWeight.Bold) },
                        actions = { IconButton(onClick = { showSettings = true }) { Text("⚙", fontSize = 24.sp) } }
                    )
                },
                bottomBar = {
                    NavigationBar {
                        val tabs = listOf("Registro" to "＋", "Gráficos" to "◔", "Historial" to "≡")
                        tabs.forEachIndexed { index, (label, icon) ->
                            NavigationBarItem(
                                selected = pagerState.currentPage == index,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                                icon = { Text(icon, fontSize = 20.sp) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            ) { pad ->
                HorizontalPager(state = pagerState, modifier = Modifier.padding(pad).fillMaxSize()) { page ->
                    when (page) {
                        0 -> RegisterScreen(repository)
                        1 -> ChartScreen(repository.movements)
                        2 -> HistoryScreen(repository.movements, repository::deleteMovement)
                    }
                }
            }

            if (showSettings) {
                ModalBottomSheet(onDismissRequest = { showSettings = false }) {
                    SettingsPanel(settings.darkTheme, settings.fontScale, 
                        onTheme = { repository.updateSettings(darkTheme = it) },
                        onScale = { repository.updateSettings(fontScale = it) })
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(repository: PocketRepository) {
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(MovementType.INCOME) }
    var category by remember { mutableStateOf("General") }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Nuevo Movimiento", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = amount, onValueChange = { amount = it },
            label = { Text("Cantidad") }, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(MovementType.INCOME to "Ingreso", MovementType.EXPENSE to "Gasto").forEach { (t, label) ->
                Button(
                    onClick = { type = t },
                    colors = if (type == t) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
                    modifier = Modifier.weight(1f)
                ) { Text(label) }
            }
        }
        OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Tipo/Categoría") }, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                amount.toDoubleOrNull()?.let { repository.addMovement(it, type, category) }
                amount = ""
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) { Text("Guardar") }
    }
}

@Composable
fun ChartScreen(movements: List<Movement>) {
    val income = movements.filter { it.type == MovementType.INCOME }.sumOf { it.amount }
    val expense = movements.filter { it.type == MovementType.EXPENSE }.sumOf { it.amount }
    
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Resumen Financiero", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(32.dp))
        Canvas(Modifier.size(200.dp)) {
            val total = (income + expense).toFloat()
            if (total > 0) {
                drawArc(Color(0xFF4CAF50), -90f, (income.toFloat() / total) * 360f, true)
                drawArc(Color(0xFFF44336), -90f + (income.toFloat() / total) * 360f, (expense.toFloat() / total) * 360f, true)
            } else {
                drawCircle(Color.LightGray)
            }
        }
        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ingresos", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                Text(formatMoney(income))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Gastos", color = Color(0xFFF44336), fontWeight = FontWeight.Bold)
                Text(formatMoney(expense))
            }
        }
    }
}

@Composable
fun HistoryScreen(movements: List<Movement>, onDelete: (Long) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item { Text("Movimientos", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(8.dp)) }
        items(movements) { m ->
            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m.category, fontWeight = FontWeight.Bold)
                        Text(if (m.type == MovementType.INCOME) "Ingreso" else "Gasto", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        formatMoney(m.amount),
                        color = if (m.type == MovementType.INCOME) Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.Black
                    )
                    IconButton(onClick = { onDelete(m.id) }) { Text("✕") }
                }
            }
        }
    }
}

@Composable
fun SettingsPanel(dark: Boolean, scale: Float, onTheme: (Boolean) -> Unit, onScale: (Float) -> Unit) {
    Column(Modifier.padding(24.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Configuración", style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Tema Oscuro", Modifier.weight(1f))
            Switch(checked = dark, onCheckedChange = onTheme)
        }
        Column {
            Text("Tamaño de Fuente: ${String.format("%.2f", scale)}")
            Slider(value = scale, onValueChange = onScale, valueRange = 0.8f..1.4f)
        }
        Spacer(Modifier.height(32.dp))
    }
}
