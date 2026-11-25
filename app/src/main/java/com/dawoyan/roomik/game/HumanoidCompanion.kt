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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dawoyan.roomik.AppViewModel
import com.dawoyan.roomik.R
import com.dawoyan.roomik.core.PointKind
import com.dawoyan.roomik.core.allFloors
import com.dawoyan.roomik.core.YardKind
import java.util.Locale

private data class HomeProgress(val bedrooms: Int, val bed: Boolean, val toilet: Boolean, val kitchen: Boolean, val lights: Int, val yard: Boolean) {
    val checks get() = listOf("Спальня" to (bedrooms > 0), "Кровать" to bed, "Туалет" to toilet, "Еда: холодильник и плита" to kitchen, "Свет" to (lights > 0), "Двор" to yard)
    val score: Int get() = checks.count { it.second } * 100 / checks.size
    val complete: Boolean get() = checks.all { it.second }
    fun next(): String = when {
        bedrooms < 1 -> "Мне нужна спальня. Добавьте комнату и выберите тип «Спальня». / Ինձ ննջասենյակ է պետք։"
        !bed -> "Мне нужна кровать, чтобы спать. / Ինձ մահճակալ է պետք։"
        !toilet -> "Я не вижу туалет. Добавьте его в разделе «Интерьер → Ванная»."
        !kitchen -> "Нужна кухня: поставьте холодильник и плиту."
        lights < 1 -> "Дома темно. Интерьер → 💡 Лампа → нажмите внутри комнаты. / Տանը մութ է։ Ինտերիերում ավելացրեք լամպ։"
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
    var careOpen by remember { mutableStateOf(false) }
    val progress = remember(vm.root) { homeProgress(vm) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    androidx.compose.runtime.LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(60000); now = System.currentTimeMillis() } }
    val dryTree = vm.root?.allFloors.orEmpty().flatMap { it.models }.firstOrNull { tree ->
        val key = "tree_${vm.root?.id}_${tree.id}"
        tree.yard?.kind in setOf(YardKind.TREE, YardKind.POPLAR, YardKind.CYPRESS) && prefs.contains(key) && now - prefs.getLong(key, now) >= 14 * 3600000L
    }
    val concern = when {
        !progress.complete -> progress.next()
        prefs.getBoolean("dog", false) && now - prefs.getLong("dog_fed", now) > 12 * 3600000L -> "Собака голодна, давай её покормим. / Շունը սոված է, կերակրենք նրան։"
        dryTree != null -> "Дерево ${dryTree.name} засыхает, ему нужна вода. / Ծառը ջրի կարիք ունի։"
        now - prefs.getLong("cleaned", 0L) > 24 * 3600000L -> "Дома пора пропылесосить. / Ժամանակն է փոշեկուլով մաքրել տունը։"
        else -> "Спасибо, дома хорошо! / Շնորհակալ եմ, տանը լավ է։"
    }
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

    Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(portrait), contentDescription = name, modifier = Modifier.height(64.dp).clickable { dialog = true })
                Text("$face $name · $happiness%", modifier = Modifier.weight(1f).clickable { dialog = true }, style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { careOpen = true }) { Text("🌱 🐕") }
                TextButton(onClick = { dialog = true }) { Text("💬") }
            }
            Text("💬 $concern", modifier = Modifier.clickable { dialog = true }, style = MaterialTheme.typography.labelSmall, maxLines = 2)
            Text(progress.checks.joinToString("  ") { (label, done) -> "${if (done) "✓" else "○"} $label" }, style = MaterialTheme.typography.labelSmall, maxLines = 2)
        }
    }
    if (dialog && selected.isEmpty()) CharacterChoice(
        onPick = { choice, chosenName, age ->
            selected = choice
            residentName = chosenName
            residentAge = age
            prefs.edit().putString("character", choice).putString("name", chosenName).putInt("age", age).apply()
            speak("Привет! Я $chosenName, мне $age. Я хочу уютный дом.")
        }
    )
    if (dialog && selected.isNotEmpty()) ResidentDialog(name, residentAge, portrait, face, progress, concern, pulse, place, speak, ::go) { dialog = false }
    if (careOpen) {
        val root = vm.root
        val trees = root?.allFloors.orEmpty().flatMap { it.models }
            .filter { it.yard?.kind in setOf(YardKind.TREE, YardKind.POPLAR, YardKind.CYPRESS) }
        var tick by remember { mutableStateOf(System.currentTimeMillis()) }
        var dog by remember { mutableStateOf(prefs.getBoolean("dog", false)) }
        var fed by remember { mutableStateOf(prefs.getLong("dog_fed", 0L)) }
        var robot by remember { mutableStateOf(prefs.getBoolean("robot_vacuum", false)) }
        var cleaned by remember { mutableStateOf(prefs.getLong("cleaned", 0L)) }
        androidx.compose.runtime.LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(1000); tick = System.currentTimeMillis() } }
        AlertDialog(onDismissRequest = { careOpen = false; now = System.currentTimeMillis() }, title = { Text("Уход за домом") }, text = {
            Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                Text(if (cleaned == 0L || tick - cleaned > 24 * 3600000L) "🏠 Пора пропылесосить" else "✨ Пол чистый")
                TextButton(onClick = { cleaned = tick; prefs.edit().putLong("cleaned", tick).apply(); speak("Я пропылесосила дом.") }) { Text("🧹 Попросить пропылесосить") }
                if (!robot) TextButton(onClick = { robot = true; prefs.edit().putBoolean("robot_vacuum", true).apply() }) { Text("🤖 Получить робот-пылесос (бесплатно)") }
                else TextButton(onClick = { cleaned = tick; prefs.edit().putLong("cleaned", tick).apply() }) { Text("🤖 Включить робот-пылесос") }
                if (trees.isEmpty()) Text("Посадите дерево во дворе: 🌿 → 🌱 Посадить дерево → нажмите на землю.")
                trees.forEach { tree ->
                    val key = "tree_${root?.id}_${tree.id}"
                    val last = prefs.getLong(key, tick).also { if (!prefs.contains(key)) prefs.edit().putLong(key, it).apply() }
                    val dryness = (((tick - last).coerceAtLeast(0L) / 3600000L) * 5).coerceIn(0, 100).toInt()
                    Text("${tree.name}: сухость $dryness% ${if (dryness >= 70 && (tick / 500) % 2L == 0L) "🔴" else "🌱"}", color = if (dryness >= 70) Color.Red else Color.Unspecified)
                    TextButton(onClick = { prefs.edit().putLong(key, System.currentTimeMillis()).apply(); tick = System.currentTimeMillis() }) { Text("💧 Полить") }
                }
                if (!dog) TextButton(onClick = { dog = true; fed = tick; prefs.edit().putBoolean("dog", true).putLong("dog_fed", tick).apply() }) { Text("🐕 Приютить собаку") }
                else {
                    Text(if (tick - fed > 12 * 3600000L) "🐕 Собака голодна" else "🐕 Собака сыта")
                    TextButton(onClick = { fed = tick; prefs.edit().putLong("dog_fed", tick).apply() }) { Text("🍖 Покормить собаку") }
                }
            }
        }, confirmButton = { TextButton(onClick = { careOpen = false; now = System.currentTimeMillis() }) { Text("Готово") } })
    }
}

private fun homeProgress(vm: AppViewModel): HomeProgress {
    val floors = vm.root?.allFloors.orEmpty()
    val items = floors.flatMap { it.items }
    return HomeProgress(
        bedrooms = floors.sumOf { floor -> floor.rooms.count { it.type?.name == "BEDROOM" } },
        bed = items.any { it.kind == "bed" || it.kind == "bed1" },
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
private fun ResidentDialog(name: String, age: Int, portrait: Int, face: String, progress: HomeProgress, message: String, bob: Float, place: String, speak: (String) -> Unit, go: (String) -> Unit, close: () -> Unit) {
    AlertDialog(
        onDismissRequest = close,
        icon = { Icon(Icons.Filled.Person, null) },
        title = { Text("$face $name") },
        text = { Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            var question by remember { mutableStateOf("") }
            var reply by remember(message, place) { mutableStateOf(message) }
            Image(painterResource(portrait), null, Modifier.height(150.dp).graphicsLayer { translationY = bob })
            Text("Вы: Какой дом ты хочешь?")
            Text("$name${if (age > 0) ", $age" else ""}: Я хочу уютный дом.")
            progress.checks.forEach { (label, done) -> Text("${if (done) "✓" else "○"} $label") }
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
            IconButton(onClick = { speak(message.substringBefore(" / ")) }) { Icon(Icons.Filled.Campaign, "Озвучить совет") }
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
