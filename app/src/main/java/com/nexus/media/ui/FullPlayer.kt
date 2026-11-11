package com.nexus.media.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.nexus.media.data.local.entities.MediaEntity
import com.nexus.media.di.AppContainer
import com.nexus.media.player.audio.LyricLine
import com.nexus.media.player.audio.loadSidecarLyrics
import com.nexus.media.player.audio.parseLyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayer(c: AppContainer, onClose: () -> Unit) {
    val st by c.player.state.collectAsState()
    val sleepEnds by c.player.sleepEndsAt.collectAsState()
    val item = st.current
    var mode by remember { mutableStateOf(c.prefs.playerMode) }
    var sheet by remember { mutableStateOf<String?>(null) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var seeking by remember { mutableStateOf(false) }
    var seekPos by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            if (!seeking) pos = c.player.player.currentPosition
            dur = c.player.player.duration.coerceAtLeast(0L)
            delay(300)
        }
    }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF14285C), Color.Black)))
            .pointerInput(Unit) {}
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.KeyboardArrowDown, "Tutup") }
                Text("Sedang diputar", Modifier.weight(1f), textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(48.dp))
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("artwork" to "Cover", "vinyl" to "Vinyl", "lyrics" to "Lirik", "compact" to "Compact").forEach { (k, label) ->
                    FilterChip(selected = mode == k, onClick = { mode = k; c.prefs.playerMode = k }, label = { Text(label) })
                }
            }
            if (item == null) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Belum ada lagu diputar") }
            } else {
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    val s = minOf(maxWidth, maxHeight)
                    when (mode) {
                        "vinyl" -> Vinyl(item, st.isPlaying, s)
                        "lyrics" -> LyricsView(item, pos, c, Modifier.fillMaxSize())
                        "compact" -> Thumb(item, Modifier.size(minOf(s, 120.dp)), RoundedCornerShape(16.dp))
                        else -> Thumb(item, Modifier.size(s), RoundedCornerShape(24.dp))
                    }
                }
                Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = if (seeking) seekPos else pos.toFloat().coerceAtMost(maxOf(dur, 1L).toFloat()),
                    onValueChange = { seeking = true; seekPos = it },
                    onValueChangeFinished = { c.player.seekTo(seekPos.toLong()); pos = seekPos.toLong(); seeking = false },
                    valueRange = 0f..maxOf(dur, 1L).toFloat()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(fmtTime(if (seeking) seekPos.toLong() else pos), style = MaterialTheme.typography.bodySmall)
                    Text(fmtTime(dur), style = MaterialTheme.typography.bodySmall)
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { c.player.setShuffle(!st.shuffle) }) {
                        Icon(Icons.Rounded.Shuffle, "Acak", tint = if (st.shuffle) MaterialTheme.colorScheme.primary else Color.White)
                    }
                    IconButton(onClick = { c.player.previous() }) { Icon(Icons.Rounded.SkipPrevious, "Sebelumnya", Modifier.size(36.dp)) }
                    FilledIconButton(onClick = { c.player.toggle() }, modifier = Modifier.size(68.dp)) {
                        Icon(if (st.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Putar/Jeda", Modifier.size(38.dp))
                    }
                    IconButton(onClick = { c.player.next() }) { Icon(Icons.Rounded.SkipNext, "Berikutnya", Modifier.size(36.dp)) }
                    IconButton(onClick = { c.player.cycleRepeat() }) {
                        Icon(
                            if (st.repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Ulangi",
                            tint = if (st.repeat != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconButton(onClick = { sheet = "queue" }) { Icon(Icons.Rounded.QueueMusic, "Antrian") }
                    IconButton(onClick = { sheet = "sleep" }) {
                        Icon(Icons.Rounded.Bedtime, "Sleep timer", tint = if (sleepEnds != null) MaterialTheme.colorScheme.primary else Color.White)
                    }
                    IconButton(onClick = { sheet = "eq" }) { Icon(Icons.Rounded.Equalizer, "Equalizer") }
                }
            }
        }
    }

    when (sheet) {
        "queue" -> ModalBottomSheet(onDismissRequest = { sheet = null }, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Text("Antrian (${st.queue.size})", Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.titleMedium)
            val ls = rememberLazyListState(initialFirstVisibleItemIndex = st.currentIndex.coerceAtLeast(0))
            LazyColumn(Modifier.heightIn(max = 480.dp), state = ls) {
                itemsIndexed(st.queue) { i, m ->
                    Row(Modifier.fillMaxWidth().clickable { c.player.jumpTo(i) }.padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(m.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (i == st.currentIndex) MaterialTheme.colorScheme.primary else Color.White)
                            Text(m.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { c.player.removeAt(i) }) { Icon(Icons.Rounded.Close, "Hapus") }
                    }
                }
            }
        }
        "eq" -> EqualizerSheet(c) { sheet = null }
        "sleep" -> AlertDialog(
            onDismissRequest = { sheet = null },
            title = { Text("Sleep timer") },
            text = {
                Column {
                    listOf(15, 30, 45, 60, 90).forEach { m ->
                        Text("$m menit", Modifier.fillMaxWidth().clickable { c.player.startSleepTimer(m); sheet = null }.padding(vertical = 12.dp))
                    }
                }
            },
            confirmButton = {
                if (sleepEnds != null) TextButton(onClick = { c.player.cancelSleepTimer(); sheet = null }) { Text("Matikan timer") }
            },
            dismissButton = { TextButton(onClick = { sheet = null }) { Text("Tutup") } }
        )
    }
}

@Composable
private fun Vinyl(item: MediaEntity, playing: Boolean, size: Dp) {
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playing) {
        if (playing) {
            var last = androidx.compose.runtime.withFrameNanos { it }
            while (true) {
                val now = androidx.compose.runtime.withFrameNanos { it }
                angle = (angle + (now - last) / 1_000_000_000f * 40f) % 360f
                last = now
            }
        }
    }
    Box(
        Modifier.size(size).rotate(angle).clip(CircleShape).background(Color(0xFF0D0D0D)).border(2.dp, Color(0xFF262626), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.fillMaxSize(0.8f).border(1.dp, Color(0xFF1C1C1C), CircleShape))
        Box(Modifier.fillMaxSize(0.65f).border(1.dp, Color(0xFF1C1C1C), CircleShape))
        Thumb(item, Modifier.fillMaxSize(0.5f), CircleShape)
        Box(Modifier.size(14.dp).clip(CircleShape).background(Color.Black))
    }
}

@Composable
private fun LyricsView(item: MediaEntity, posMs: Long, c: AppContainer, modifier: Modifier) {
    val ctx = LocalContext.current
    var uriStr by remember(item.id) { mutableStateOf(c.prefs.lyricsUri(item.id)) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u: Uri? ->
        if (u != null) {
            runCatching { ctx.contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            c.prefs.setLyricsUri(item.id, u.toString())
            uriStr = u.toString()
        }
    }
    val lines by produceState<List<LyricLine>>(emptyList(), uriStr, item.id) {
        val s = uriStr
        value = withContext(Dispatchers.IO) {
            val raw = if (s != null) {
                runCatching { ctx.contentResolver.openInputStream(Uri.parse(s))!!.bufferedReader().use { it.readText() } }.getOrNull()
            } else {
                loadSidecarLyrics(ctx, Uri.parse(item.uri))   // auto: <lagu>.lrc / .txt di folder yang sama
            }
            if (raw == null) emptyList() else parseLyrics(raw)
        }
    }
    if (lines.isEmpty()) {
        Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("Lirik belum ada", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(onClick = { picker.launch(arrayOf("*/*")) }) { Text("Pilih file lirik (.lrc / .txt)") }
        }
    } else {
        val synced = lines.first().timeMs >= 0
        val cur = if (synced) lines.indexOfLast { it.timeMs <= posMs }.coerceAtLeast(0) else -1
        val ls = rememberLazyListState()
        LaunchedEffect(cur) { if (cur >= 0) ls.animateScrollToItem(maxOf(cur - 3, 0)) }
        LazyColumn(modifier, state = ls, horizontalAlignment = Alignment.CenterHorizontally) {
            itemsIndexed(lines) { i, l ->
                Text(
                    l.text, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium,
                    color = if (i == cur || !synced) Color.White else Color(0x88FFFFFF),
                    fontWeight = if (i == cur) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
            item { TextButton(onClick = { picker.launch(arrayOf("*/*")) }) { Text("Ganti file lirik") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EqualizerSheet(c: AppContainer, onDismiss: () -> Unit) {
    val eq by c.equalizer.state.collectAsState()
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState())) {
            Text("Equalizer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (!eq.available) {
                Text("Equalizer aktif setelah lagu diputar.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
            } else {
                Text("Preset", Modifier.padding(top = 12.dp, bottom = 4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(eq.presets) { i, n -> AssistChip(onClick = { c.equalizer.usePreset(i) }, label = { Text(n) }) }
                }
                eq.levels.forEachIndexed { i, l ->
                    val hz = eq.freqsHz.getOrElse(i) { 0 }
                    Text(if (hz >= 1000) "${hz / 1000} kHz" else "$hz Hz", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                    Slider(
                        value = l.toFloat(), onValueChange = { c.equalizer.setBand(i, it.toInt()) },
                        valueRange = eq.minLevel.toFloat()..eq.maxLevel.toFloat()
                    )
                }
                Text("Bass boost")
                Slider(value = eq.bass.toFloat(), onValueChange = { c.equalizer.setBass(it.toInt()) }, valueRange = 0f..1000f)
                Text("Surround (virtualizer)")
                Slider(value = eq.virt.toFloat(), onValueChange = { c.equalizer.setVirt(it.toInt()) }, valueRange = 0f..1000f)
                Text("Profil kustom", Modifier.padding(top = 8.dp, bottom = 4.dp))
                var name by remember { mutableStateOf("") }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, singleLine = true, label = { Text("Nama profil") }, modifier = Modifier.weight(1f))
                    Button(onClick = { if (name.isNotBlank()) { c.equalizer.saveProfile(name.trim()); name = "" } }) { Text("Simpan") }
                }
                LazyRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(eq.profiles) { p ->
                        InputChip(
                            selected = false, onClick = { c.equalizer.loadProfile(p) }, label = { Text(p) },
                            trailingIcon = { Icon(Icons.Rounded.Close, "Hapus", Modifier.size(16.dp).clickable { c.equalizer.deleteProfile(p) }) }
                        )
                    }
                }
            }
        }
    }
}
