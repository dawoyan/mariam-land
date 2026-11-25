package com.dawoyan.roomik.game

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dawoyan.roomik.AppViewModel
import com.dawoyan.roomik.R
import com.dawoyan.roomik.core.PointKind
import com.dawoyan.roomik.core.allFloors
import java.util.Locale

private data class HomeProgress(val bedrooms: Int, val toilet: Boolean, val kitchen: Boolean, val lights: Int, val yard: Boolean) {
    val score: Int get() = (bedrooms.coerceAtMost(3) * 18) + (if (toilet) 18 else 0) + (if (kitchen) 22 else 0) + (lights.coerceAtMost(3) * 2) + (if (yard) 6 else 0)
    val complete: Boolean get() = bedrooms >= 3 && toilet && kitchen && lights >= 3 && yard
    fun next(): String = when {
        bedrooms < 3 -> "Нужны спальни: $bedrooms из 3. Добавьте комнату и выберите тип «Спальня»."
        !toilet -> "Я не вижу туалет. Добавьте его в разделе «Интерьер → Ванная»."
        !kitchen -> "Нужна кухня: поставьте холодильник и плиту."
        lights < 3 -> "Дома темно. Добавьте ещё светильники: $lights из 3."
        !yard -> "Хочу двор! Добавьте участок / двор в инструментах участка."
        else -> "Дом готов — он удобный, светлый и мне очень нравится!"
    }
}

/** The resident is both a visual companion and a live checklist for the room plan. */
@Composable
fun HumanoidCompanion(vm: AppViewModel) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mariam_companion", Context.MODE_PRIVATE) }
    var selected by remember { mutableStateOf(prefs.getString("character", "") ?: "") }
    var residentName by remember { mutableStateOf(prefs.getString("name", "") ?: "") }
    var residentAge by remember { mutableStateOf(prefs.getInt("age", 0)) }
    var place by remember { mutableStateOf(prefs.getString("place", "Дома") ?: "Дома") }
    var happiness by remember { mutableStateOf(prefs.getInt("happiness", 50)) }
    var dialog by remember { mutableStateOf(selected.isEmpty()) }
    val progress = remember(vm.root) { homeProgress(vm) }
    val speak = rememberRussianSpeech(context)
    val pulse by rememberInfiniteTransition(label = "resident")
        .animateFloat(0f, 5f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "resident-breath")
    val name = residentName.ifBlank { if (selected == "boy") "Максим" else "Марьям" }
    val portrait = if (selected == "boy") R.drawable.mariam_guide_boy else R.drawable.mariam_guide
    val face = when { happiness < 35 -> "😟"; happiness >= 70 -> "😁"; happiness >= 55 -> "😊"; else -> "🙂" }
    fun go(destination: String) {
        place = destination
        happiness = (happiness + when (destination) { "Работа" -> -6; "Магазин" -> 8; "Спортзал" -> 6; else -> 2 }).coerceIn(0, 100)
        prefs.edit().putString("place", place).putInt("happiness", happiness).apply()
    }

    Box(Modifier.fillMaxSize().padding(end = 12.dp, bottom = 18.dp), contentAlignment = Alignment.BottomEnd) {
        Card(modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                Image(painterResource(portrait), null, Modifier.height(58.dp).graphicsLayer { translationX = pulse })
                Column(Modifier.padding(start = 5.dp)) {
                    Text("$face $name · $happiness%")
                    Text("📍 $place", style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
                    LinearProgressIndicator(progress = { progress.score / 100f }, modifier = Modifier.size(width = 74.dp, height = 5.dp))
                }
            }
        }
        FloatingActionButton(onClick = { dialog = true }, containerColor = Color(0xFFFFC857)) {
            Icon(Icons.Filled.Chat, "Поговорить с жителем")
        }
    }
    if (dialog && selected.isEmpty()) CharacterChoice(
        onPick = { choice, chosenName, age ->
            selected = choice
            residentName = chosenName
            residentAge = age
            prefs.edit().putString("character", choice).putString("name", chosenName).putInt("age", age).apply()
            speak("Привет! Я $chosenName, мне $age. Я хочу дом с тремя спальнями, кухней, туалетом, светом и двором.")
        }
    )
    if (dialog && selected.isNotEmpty()) ResidentDialog(name, residentAge, portrait, face, progress, pulse, place, speak, ::go) { dialog = false }
}

private fun homeProgress(vm: AppViewModel): HomeProgress {
    val floors = vm.root?.allFloors.orEmpty()
    val items = floors.flatMap { it.items }
    return HomeProgress(
        bedrooms = floors.sumOf { floor -> floor.rooms.count { it.type?.name == "BEDROOM" } },
        toilet = items.any { it.kind == "toilet" },
        kitchen = items.any { it.kind == "fridge" } && items.any { it.kind == "stove" },
        lights = floors.sumOf { floor -> floor.points.count { it.kind == PointKind.LAMP } },
        yard = vm.root?.site != null
    )
}

@Composable
private fun CharacterChoice(onPick: (String, String, Int) -> Unit) {
    var gender by remember { mutableStateOf("girl") }
    var name by remember { mutableStateOf("Марьям") }
    var ageText by remember { mutableStateOf("18") }
    val age = ageText.toIntOrNull()
    AlertDialog(
        onDismissRequest = {},
        icon = { Icon(Icons.Filled.Person, null) },
        title = { Text("Создайте жителя") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Этот персонаж будет жить в доме, который вы строите.")
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Имя") }, singleLine = true)
            OutlinedTextField(value = ageText, onValueChange = { ageText = it.filter(Char::isDigit).take(2) }, label = { Text("Возраст") }, singleLine = true, isError = age != null && age !in 4..99)
            Text("Пол")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = { gender = "girl"; if (name == "Максим") name = "Марьям" }, label = { Text(if (gender == "girl") "✓ Девочка" else "Девочка") })
                AssistChip(onClick = { gender = "boy"; if (name == "Марьям") name = "Максим" }, label = { Text(if (gender == "boy") "✓ Мальчик" else "Мальчик") })
            }
        } },
        confirmButton = { TextButton(enabled = name.isNotBlank() && age in 4..99, onClick = { onPick(gender, name.trim(), age!!) }) { Text("Начать строить") } }
    )
}

@Composable
private fun ResidentDialog(name: String, age: Int, portrait: Int, face: String, progress: HomeProgress, bob: Float, place: String, speak: (String) -> Unit, go: (String) -> Unit, close: () -> Unit) {
    val message = progress.next()
    AlertDialog(
        onDismissRequest = close,
        icon = { Icon(Icons.Filled.Person, null) },
        title = { Text("$face $name") },
        text = { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            var question by remember { mutableStateOf("") }
            var reply by remember(progress, place) { mutableStateOf(message) }
            Image(painterResource(portrait), null, Modifier.height(150.dp).graphicsLayer { translationY = bob })
            Text("Вы: Какой дом ты хочешь?")
            Text("$name${if (age > 0) ", $age" else ""}: Я хочу дом с тремя спальнями, двором, кухней, туалетом и светом.")
            Text("$name: $reply")
            Text("Настроение зависит от поездок: работа снижает его, магазин и спортзал повышают.")
            OutlinedTextField(question, { question = it }, label = { Text("Спросите жителя") }, singleLine = true)
            TextButton(onClick = {
                reply = when {
                    question.contains("настро", ignoreCase = true) || question.contains("почему", ignoreCase = true) -> "После работы я устаю. Магазин и спортзал улучшают моё настроение. / Աշխատանքից հոգնում եմ, իսկ խանութն ու մարզասրահը բարձրացնում են տրամադրությունս։"
                    else -> message
                }
                speak(reply.substringBefore(" / "))
            }) { Text("Спросить / Հարցնել") }
            Text("Сейчас: $place")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AssistChip(onClick = { go("Дома") }, label = { Text("🏠") })
                AssistChip(onClick = { go("Работа") }, label = { Text("💼") })
                AssistChip(onClick = { go("Магазин") }, label = { Text("🛍️") })
                AssistChip(onClick = { go("Спортзал") }, label = { Text("🏋️") })
            }
        } },
        confirmButton = { Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { speak(message) }) { Icon(Icons.Filled.Campaign, "Озвучить совет") }
            TextButton(onClick = close) { Text("Строим") }
        } }
    )
}

@Composable
private fun rememberRussianSpeech(context: Context): (String) -> Unit {
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val engine = TextToSpeech(context) { }
        tts = engine
        onDispose { engine.stop(); engine.shutdown() }
    }
    return { text -> tts?.let { it.language = Locale("ru", "RU"); it.speak(text, TextToSpeech.QUEUE_FLUSH, null, "mariam") } }
}
