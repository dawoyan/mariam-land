package com.dawoyan.roomik.game

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ShoppingCart
import kotlin.math.max

/** A small, offline-first life-simulation.  State is saved locally between upgrades. */
class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("mariam_land_life", 0)
    var name by mutableStateOf(prefs.getString("name", "Մարիամ") ?: "Մարիամ"); private set
    var pronoun by mutableStateOf(prefs.getString("pronoun", "նա") ?: "նա"); private set
    var happiness by mutableIntStateOf(prefs.getInt("happiness", 72)); private set
    var wealth by mutableIntStateOf(prefs.getInt("wealth", 320)); private set
    var health by mutableIntStateOf(prefs.getInt("health", 68)); private set
    var lightsOn by mutableStateOf(prefs.getBoolean("lights", true)); private set
    var waterFlowing by mutableStateOf(prefs.getBoolean("water", false)); private set
    var pet by mutableStateOf(prefs.getString("pet", "") ?: ""); private set
    var furniture by mutableStateOf(prefs.getStringSet("furniture", setOf("Բազմոց", "Սառնարան", "Մահճակալ")) ?: emptySet()); private set
    var playerFacts by mutableStateOf(prefs.getString("facts", "") ?: ""); private set
    var concern by mutableStateOf("Мне хочется поесть. / Ես ուզում եմ ուտել։")

    private fun save() = prefs.edit()
        .putString("name", name).putString("pronoun", pronoun)
        .putInt("happiness", happiness).putInt("wealth", wealth).putInt("health", health)
        .putBoolean("lights", lightsOn).putBoolean("water", waterFlowing)
        .putString("pet", pet).putStringSet("furniture", furniture).putString("facts", playerFacts).apply()

    fun customise(newName: String, newPronoun: String) { name = newName.ifBlank { name }; pronoun = newPronoun; save() }
    fun toggleLights() { lightsOn = !lightsOn; save() }
    fun toggleWater() { waterFlowing = !waterFlowing; save() }
    fun adopt(kind: String) { pet = kind; happiness = (happiness + 8).coerceAtMost(100); concern = if (kind == "Շուն") "Я хочу поиграть с собакой! / Ես ուզում եմ խաղալ շան հետ։" else "Кот дома — мне спокойнее. / Կատուն տանն է՝ ես հանգիստ եմ։"; save() }
    fun addFurniture(item: String) { furniture = furniture + item; save() }
    fun go(place: String) {
        when (place) {
            "gym" -> { health = (health + 14).coerceAtMost(100); happiness = (happiness + 3).coerceAtMost(100); concern = "Я бодрее после зала! / Մարզասրահից հետո ավելի առույգ եմ։" }
            "shop" -> { happiness = (happiness + 13).coerceAtMost(100); wealth = max(0, wealth - 35); concern = "Спасибо за продукты! / Շնորհակալ եմ մթերքի համար։" }
        }
        save()
    }
    fun earnedFromWork() { wealth += 120; happiness = max(0, happiness - 6); health = max(0, health - 4); concern = "Я поработал и заработал! / Աշխատեցի ու գումար վաստակեցի։"; save() }
    fun rememberFacts(facts: String) { playerFacts = facts.trim(); save() }
    fun reply(question: String): String {
        val q = question.lowercase()
        return when {
            "туал" in q || "զուգ" in q -> "Мне нужен туалет. / Ինձ զուգարան է պետք։"
            "еда" in q || "холод" in q || "սն" in q || "սառնար" in q -> "Мне нужна еда из холодильника. / Սառնարանից ուտելիք եմ ուզում։"
            "что" in q || "ուզ" in q -> concern
            playerFacts.isNotBlank() -> "Я помню: $playerFacts. Сейчас мне важно: $concern"
            else -> "Расскажи мне о себе — я запомню это. / Պատմիր քո մասին, ես կհիշեմ։"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(vm: GameViewModel) {
    var tab by remember { mutableStateOf("տուն") }
    var showPairFinder by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }
    var showCustomize by remember { mutableStateOf(false) }
    var showCatalog by remember { mutableStateOf(false) }
    var showPets by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF7F4EE)) {
        Column {
            TopAppBar(
                title = { Text("Մարիամի աշխարհ", fontWeight = FontWeight.Bold) },
                actions = { Text("💾 պահպանվում է", modifier = Modifier.padding(end = 12.dp), fontSize = 12.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFE9B0))
            )
            NeedsRow(vm)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                items(listOf("տուն", "3D տեսք", "կահույք", "զրույց")) { label ->
                    FilterChip(selected = tab == label, onClick = {
                        tab = label
                        if (label == "կահույք") showCatalog = true
                        if (label == "զրույց") showChat = true
                    }, label = { Text(label) })
                }
            }
            if (tab == "3D տեսք") HouseScene(vm, true) else HouseScene(vm, false)
            ActionRow(
                onWork = { showPairFinder = true },
                onGym = { vm.go("gym") },
                onShop = { vm.go("shop") }
            )
            Card(modifier = Modifier.padding(12.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(14.dp)) {
                    Text("${vm.name}-ի մտքերը", fontWeight = FontWeight.Bold)
                    Text(vm.concern, modifier = Modifier.padding(top = 4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                        AssistChip(onClick = { showChat = true }, label = { Text("💬 խոսել") })
                        AssistChip(onClick = { showCustomize = true }, label = { Text("🧍 կերպար") })
                        AssistChip(onClick = { showPets = true }, label = { Text("🐾 ընտանի կենդանի") })
                    }
                }
            }
            Text("Տունը՝ ${vm.furniture.joinToString(" · ")}", modifier = Modifier.padding(horizontal = 16.dp), fontSize = 13.sp, color = Color(0xFF5B5B5B))
        }
    }
    if (showPairFinder) PairFinder(vm) { showPairFinder = false }
    if (showChat) ChatSheet(vm) { showChat = false }
    if (showCustomize) CustomiseSheet(vm) { showCustomize = false }
    if (showCatalog) CatalogSheet(vm) { showCatalog = false }
    if (showPets) PetSheet(vm) { showPets = false }
}

@Composable private fun NeedsRow(vm: GameViewModel) = Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
    Need("երջանկություն", vm.happiness, Color(0xFFFFA000), Icons.Filled.Star)
    Need("բարեկեցություն", vm.wealth.coerceAtMost(1000) / 10, Color(0xFF2E7D32), Icons.Filled.ShoppingCart, "${vm.wealth} դրամ")
    Need("առողջություն", vm.health, Color(0xFFD84355), Icons.Filled.Favorite)
}

@Composable private fun Need(label: String, value: Int, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, suffix: String = "$value%") = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp)); Text(label, fontSize = 10.sp)
    Text(suffix, fontWeight = FontWeight.Bold, color = color, fontSize = 12.sp)
}

@Composable private fun HouseScene(vm: GameViewModel, threeD: Boolean) = Card(Modifier.padding(horizontal = 12.dp).fillMaxWidth().height(220.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = if (vm.lightsOn) Color(0xFFBBE4FF) else Color(0xFF283444))) {
    Box(Modifier.fillMaxSize()) {
        Text(if (threeD) "3D · ${if (vm.lightsOn) "լուսավոր տուն" else "գիշերային տուն"}" else "Տան տեսարան", modifier = Modifier.padding(16.dp), color = if (vm.lightsOn) Color(0xFF183B56) else Color.White, fontWeight = FontWeight.Bold)
        Text("🏡", fontSize = 112.sp, modifier = Modifier.align(Alignment.Center))
        Text("${if (vm.lightsOn) "💡" else "🌙"}  ${if (vm.waterFlowing) "💧 ջուրը հոսում է" else "🚰 ջուրը փակ է"}", modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp), fontSize = 17.sp)
        Text("🧍 ${vm.name}\n${if (vm.pet.isNotBlank()) if (vm.pet == "Շուն") "🐕 վազում ու խաղում է" else "🐈 կողքին է" else ""}", modifier = Modifier.align(Alignment.CenterStart).padding(start = 20.dp, top = 54.dp), fontSize = 14.sp)
        Row(Modifier.align(Alignment.BottomEnd).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            AssistChip(onClick = vm::toggleLights, label = { Text(if (vm.lightsOn) "Անջատել լույսը" else "Միացնել լույսը") })
            AssistChip(onClick = vm::toggleWater, label = { Text(if (vm.waterFlowing) "Փակել ջուրը" else "Բացել ջուրը") })
        }
    }
}

@Composable private fun ActionRow(onWork: () -> Unit, onGym: () -> Unit, onShop: () -> Unit) = Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    Button(onClick = onWork, modifier = Modifier.weight(1f)) { Text("💼\nԱշխատանք", textAlign = TextAlign.Center) }
    Button(onClick = onGym, modifier = Modifier.weight(1f)) { Text("🏋️\nՄարզասրահ", textAlign = TextAlign.Center) }
    Button(onClick = onShop, modifier = Modifier.weight(1f)) { Text("🛒\nՍուպերմարկետ", textAlign = TextAlign.Center) }
}

@Composable private fun DialogFrame(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.material3.AlertDialog(onDismissRequest = onClose, confirmButton = { Button(onClick = onClose) { Text("Պատրաստ է") } }, title = { Text(title) }, text = { Column(content = content) })
}

@Composable private fun CustomiseSheet(vm: GameViewModel, close: () -> Unit) {
    var name by remember { mutableStateOf(vm.name) }; var pronoun by remember { mutableStateOf(vm.pronoun) }
    DialogFrame("Կերպարի հարմարեցում", { vm.customise(name, pronoun); close() }) {
        OutlinedTextField(name, { name = it }, label = { Text("Անուն") })
        OutlinedTextField(pronoun, { pronoun = it }, label = { Text("Նա / նա") }, modifier = Modifier.padding(top = 8.dp))
        Text("Փորձն ու կարիքները պահվում են սարքում՝ նաև հավելվածի թարմացումից հետո։", modifier = Modifier.padding(top = 12.dp), fontSize = 12.sp)
    }
}

@Composable private fun PetSheet(vm: GameViewModel, close: () -> Unit) = DialogFrame("Ընտրիր ընտանի կենդանուն", close) {
    Button(onClick = { vm.adopt("Կատու"); close() }, modifier = Modifier.fillMaxWidth()) { Text("🐈 Սև կատու") }
    Button(onClick = { vm.adopt("Շուն"); close() }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("🐕 Գերմանական հովվաշուն") }
}

@Composable private fun CatalogSheet(vm: GameViewModel, close: () -> Unit) = DialogFrame("Տան կատալոգ", close) {
    Text("Ներքին և արտաքին իրերը", fontSize = 13.sp)
    listOf("Բազմոց", "Սառնարան", "Զուգարան", "Մահճակալ", "Բույս", "Լողավազան", "Թռչնանոց", "Այգու սեղան").forEach { item ->
        AssistChip(onClick = { vm.addFurniture(item) }, label = { Text("+ $item") }, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable private fun ChatSheet(vm: GameViewModel, close: () -> Unit) {
    var text by remember { mutableStateOf("") }; var answer by remember { mutableStateOf(vm.concern) }; var facts by remember { mutableStateOf(vm.playerFacts) }
    DialogFrame("Զրույց ${vm.name}-ի հետ", { vm.rememberFacts(facts); close() }) {
        Text("Նա ռուսերեն կբողոքի և հայերեն կասի կարիքը։", fontSize = 12.sp)
        OutlinedTextField(text, { text = it }, label = { Text("Հարցրու՝ ի՞նչ ես ուզում") })
        Button(onClick = { answer = vm.reply(text) }, modifier = Modifier.padding(top = 8.dp)) { Text("Հարցնել") }
        Text(answer, modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.Medium)
        HorizontalDivider(Modifier.padding(vertical = 10.dp))
        OutlinedTextField(facts, { facts = it }, label = { Text("Քո մասին (կհիշի)") })
    }
}

@Composable private fun PairFinder(vm: GameViewModel, close: () -> Unit) {
    val cards = remember { mutableStateListOf("🍎", "🍎", "🏠", "🏠", "🐈", "🐈", "🌻", "🌻").apply { shuffle() } }
    var open by remember { mutableStateOf(listOf<Int>()) }; var solved by remember { mutableStateOf(setOf<Int>()) }; var message by remember { mutableStateOf("Գտիր բոլոր զույգերը՝ աշխատանքից գումար վաստակելու համար") }
    DialogFrame("💼 Pair Finder · հեշտ", close) {
        Text(message, fontSize = 13.sp)
        Column(Modifier.padding(top = 8.dp)) {
            cards.chunked(4).forEachIndexed { row, line -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 3.dp)) {
                line.forEachIndexed { col, emoji ->
                    val i = row * 4 + col; val show = i in open || i in solved
                    Card(Modifier.size(54.dp).clickable(enabled = i !in solved && i !in open) {
                        val next = open + i
                        if (next.size == 2) {
                            if (cards[next[0]] == cards[next[1]]) { solved += next; open = emptyList(); if (solved.size == cards.size) { vm.earnedFromWork(); message = "Աշխատավարձ +120 դրամ։ Առողջություն և ուրախություն փոքր-ինչ պակասել են։" } }
                            else { open = emptyList(); message = "Փորձիր նորից" }
                        } else open = next
                    }, colors = CardDefaults.cardColors(containerColor = if (show) Color(0xFFFFE9B0) else Color(0xFF5A7490))) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (show) emoji else "?", fontSize = 25.sp, color = if (show) Color.Unspecified else Color.White) } }
                }
            } }
        }
    }
}
