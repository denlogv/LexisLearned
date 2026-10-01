package dev.denlogv.lexislearned.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.denlogv.lexislearned.domain.Direction
import dev.denlogv.lexislearned.domain.StudyMode
import dev.denlogv.lexislearned.ui.AppProgress
import dev.denlogv.lexislearned.ui.appViewModel
import dev.denlogv.lexislearned.ui.plural

/**
 * The study screen: one step of the session at a time, then a summary.
 *
 * @param onBack called when the back button or "Done" is pressed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(onBack: () -> Unit) {
    val vm = appViewModel { app, handle -> StudyViewModel(app.repository, app.settings, handle) }
    val ui by vm.ui.collectAsState()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(ui.title) },
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
        )
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            when {
                ui.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                ui.finished -> Summary(ui, onBack)
                else -> ActiveStep(ui, vm.direction, vm.actions())
            }
        }
    }
}

/**
 * The running session: progress bar, status line and the current step.
 *
 * @param ui what to show.
 * @param d which side of a card is the question.
 * @param actions what the step's buttons do.
 */
@Composable
private fun ColumnScope.ActiveStep(ui: StudyUi, d: Direction, actions: StudyActions) {
    AppProgress(ui.progress, Modifier.fillMaxWidth())
    Text(
        ui.statusLine,
        Modifier.padding(16.dp, 8.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    key(ui.current.map { it.id }, ui.mode, ui.stepsDone) {
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) { StepCard(ui, d, actions) }
    }
}

/**
 * The card of the current step, chosen by mode.
 *
 * @param ui what to show.
 * @param d which side of a card is the question.
 * @param actions what the step's buttons do.
 */
@Composable
private fun StepCard(ui: StudyUi, d: Direction, actions: StudyActions) {
    val first = ui.current.first()
    when (ui.mode) {
        StudyMode.LEARN -> LearnCard(first, d, true, ui.exposure, actions)
        StudyMode.CHECK -> LearnCard(first, d, false, false, actions)
        StudyMode.SELECT ->
            if (ui.options.size >= 2) SelectCard(first, d, ui.options, actions.onGrade) else LearnCard(first, d, false, false, actions)
        StudyMode.TYPE -> TypeCard(first, d, actions.onGrade)
        StudyMode.PAIR -> PairBoard(ui.current, d, actions.onPair)
    }
}

/**
 * The end of a session, or a note that there was nothing to study.
 *
 * @param ui the final state.
 * @param onDone called when "Done" is pressed.
 */
@Composable
private fun Summary(ui: StudyUi, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (ui.totalWords == 0) {
            Text("Nothing to study right now", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text("All due cards are done. Come back later!", Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
        } else {
            Text("Session complete", style = MaterialTheme.typography.headlineSmall)
            Text("${plural(ui.completedWords, "word")} went through all modes", Modifier.padding(top = 8.dp))
            Text("${ui.correct} right · ${ui.wrong} missed answers", Modifier.padding(top = 4.dp))
        }
        Button(onDone, Modifier.padding(top = 24.dp)) { Text("Done") }
    }
}
