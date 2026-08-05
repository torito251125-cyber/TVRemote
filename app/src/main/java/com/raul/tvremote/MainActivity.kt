package com.raul.tvremote

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { Remote() } }
    }
}

private val BG = Color(0xFF0B0D12)
private val CARD = Color(0xFF161A23)
private val ACC = Color(0xFF2B4570)
private val RED = Color(0xFF3A1218)
private val TXT = Color(0xFFE9ECF3)

@Composable
fun Remote() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var ip by remember { mutableStateOf(TvAdb.lastIp(ctx)) }
    var status by remember { mutableStateOf("Escribe la IP de tu TV o pulsa Buscar") }
    var apps by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var txt by remember { mutableStateOf("") }

    fun tap(code: Int) {
        vibrate(ctx)
        scope.launch {
            TvAdb.key(code).onFailure { status = "Error: " + it.message }
        }
    }

    Column(
        Modifier.fillMaxSize().background(BG).verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("TV Remote", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TXT)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = ip,
                onValueChange = { ip = it },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    status = "Buscando en la red..."
                    scope.launch {
                        val found = TvAdb.scan(myIp(ctx))
                        status = if (found.isEmpty()) "No se encontro ningun dispositivo"
                        else { ip = found[0]; "Encontrado: " + found.joinToString() }
                    }
                },
                modifier = Modifier.height(56.dp)
            ) { Text("Buscar") }
        }

        Button(
            onClick = {
                status = "Conectando... acepta el aviso en la TV"
                scope.launch {
                    TvAdb.connect(ctx, ip.trim())
                        .onSuccess { status = "Conectado a " + it }
                        .onFailure { status = "Error: " + it.message }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ACC),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Conectar") }

        Text(status, fontSize = 12.sp, color = Color(0xFF8B93A7))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("Power", Modifier.weight(1f), RED) { tap(K.POWER) }
            Btn("Fuente", Modifier.weight(1f)) { tap(K.SOURCE) }
            Btn("Inicio", Modifier.weight(1f)) { tap(K.HOME) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("Vol +", Modifier.fillMaxWidth()) { tap(K.VOL_UP) }
                Btn("Mute", Modifier.fillMaxWidth()) { tap(K.MUTE) }
                Btn("Vol -", Modifier.fillMaxWidth()) { tap(K.VOL_DOWN) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("Canal +", Modifier.fillMaxWidth()) { tap(K.CH_UP) }
                Btn("Guia", Modifier.fillMaxWidth()) { tap(K.GUIDE) }
                Btn("Canal -", Modifier.fillMaxWidth()) { tap(K.CH_DOWN) }
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Btn("Arriba", Modifier.width(110.dp)) { tap(K.UP) }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Btn("Izq", Modifier.width(96.dp)) { tap(K.LEFT) }
                Surface(color = ACC, shape = CircleShape, modifier = Modifier.size(84.dp)) {
                    Box(
                        Modifier.fillMaxSize().clickable { tap(K.OK) },
                        contentAlignment = Alignment.Center
                    ) { Text("OK", color = Color.White, fontWeight = FontWeight.Bold) }
                }
                Btn("Der", Modifier.width(96.dp)) { tap(K.RIGHT) }
            }
            Btn("Abajo", Modifier.width(110.dp)) { tap(K.DOWN) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("Atras", Modifier.weight(1f)) { tap(K.BACK) }
            Btn("Menu", Modifier.weight(1f)) { tap(K.MENU) }
            Btn("Apps", Modifier.weight(1f)) { tap(K.RECENTS) }
            Btn("Ajuste", Modifier.weight(1f)) { tap(K.SETTINGS) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("<<", Modifier.weight(1f)) { tap(K.REW) }
            Btn("Play", Modifier.weight(1f)) { tap(K.PLAY_PAUSE) }
            Btn("Stop", Modifier.weight(1f)) { tap(K.STOP) }
            Btn(">>", Modifier.weight(1f)) { tap(K.FFW) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("Buscar", Modifier.weight(1f)) { tap(K.SEARCH) }
            Btn("Voz", Modifier.weight(1f)) { tap(K.ASSIST) }
            Btn("CC", Modifier.weight(1f)) { tap(K.CAPTIONS) }
            Btn("Info", Modifier.weight(1f)) { tap(K.INFO) }
        }

        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { n -> Btn(n.toString(), Modifier.weight(1f)) { tap(K.num(n)) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn("Enter", Modifier.weight(1f)) { tap(K.ENTER) }
            Btn("0", Modifier.weight(1f)) { tap(K.num(0)) }
            Btn("Borrar", Modifier.weight(1f)) { tap(K.DEL) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = txt,
                onValueChange = { txt = it },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { scope.launch { TvAdb.text(txt); txt = "" } },
                modifier = Modifier.height(56.dp)
            ) { Text("Enviar") }
        }

        Btn("Cargar apps", Modifier.fillMaxWidth()) {
            scope.launch {
                apps = TvAdb.apps()
                status = apps.size.toString() + " apps"
            }
        }

        apps.chunked(3).forEach { fila ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { par ->
                    Btn(par.second, Modifier.weight(1f)) {
                        scope.launch { TvAdb.launch(par.first) }
                    }
                }
                repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun Btn(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = CARD,
    onClick: () -> Unit
) {
    Surface(color = color, shape = RoundedCornerShape(12.dp), modifier = modifier.height(54.dp)) {
        Box(
            Modifier.fillMaxSize().clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { Text(label, color = TXT, fontSize = 14.sp, maxLines = 1) }
    }
}

private fun myIp(ctx: Context): String {
    val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    @Suppress("DEPRECATION") val i = wm.connectionInfo.ipAddress
    return "" + (i and 0xff) + "." + (i shr 8 and 0xff) + "." +
            (i shr 16 and 0xff) + "." + (i shr 24 and 0xff)
}

private fun vibrate(ctx: Context) {
    val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(15, 60))
    else @Suppress("DEPRECATION") v.vibrate(15)
}
