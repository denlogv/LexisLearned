package dev.denlogv.lexislearned.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.denlogv.lexislearned.ui.deck.ChapterScreen
import dev.denlogv.lexislearned.ui.deck.DeckScreen
import dev.denlogv.lexislearned.ui.deck.PartScreen
import dev.denlogv.lexislearned.ui.epub.EpubScreen
import dev.denlogv.lexislearned.ui.library.LibraryScreen
import dev.denlogv.lexislearned.ui.settings.SettingsScreen
import dev.denlogv.lexislearned.ui.study.StudyScreen

/** The navigation graph of the whole app; see [Routes] for the addresses. */
@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(nav, startDestination = Routes.LIBRARY) {
        libraryRoutes(nav)
        deckRoutes(nav)
        studyRoute(nav)
    }
}

/**
 * A navigation argument that holds a database id.
 *
 * @param name the argument name used in the route pattern.
 * @return the argument definition.
 */
private fun idArgument(name: String): NamedNavArgument = navArgument(name) { type = NavType.LongType }

/**
 * Adds the library, settings and EPUB screens.
 *
 * @receiver the graph being built.
 * @param nav the controller used to move between screens.
 */
private fun NavGraphBuilder.libraryRoutes(nav: NavHostController) {
    composable(Routes.LIBRARY) {
        LibraryScreen(
            onOpenDeck = { nav.navigate(Routes.deck(it)) },
            onSettings = { nav.navigate(Routes.SETTINGS) },
            onEpub = { nav.navigate(Routes.EPUB) },
        )
    }
    composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
    composable(Routes.EPUB) {
        EpubScreen(
            onBack = { nav.popBackStack() },
            onSettings = { nav.navigate(Routes.SETTINGS) },
            onOpenDeck = { id -> nav.navigate(Routes.deck(id)) { popUpTo(Routes.LIBRARY) } },
        )
    }
}

/**
 * Adds the book, part and chapter screens.
 *
 * @receiver the graph being built.
 * @param nav the controller used to move between screens.
 */
private fun NavGraphBuilder.deckRoutes(nav: NavHostController) {
    composable(Routes.DECK, arguments = listOf(idArgument("deckId"))) {
        DeckScreen(
            onBack = { nav.popBackStack() },
            onStudy = { d, c, p -> nav.navigate(Routes.study(d, c, p)) },
            onChapter = { nav.navigate(Routes.chapter(it)) },
            onPart = { nav.navigate(Routes.part(it)) },
        )
    }
    composable(Routes.PART, arguments = listOf(idArgument("partId"))) {
        PartScreen(
            onBack = { nav.popBackStack() },
            onStudy = { d, c, p -> nav.navigate(Routes.study(d, c, p)) },
            onChapter = { nav.navigate(Routes.chapter(it)) },
        )
    }
    composable(Routes.CHAPTER, arguments = listOf(idArgument("chapterId"))) {
        ChapterScreen(onBack = { nav.popBackStack() }, onStudy = { d, c, p -> nav.navigate(Routes.study(d, c, p)) })
    }
}

/**
 * Adds the study screen.
 *
 * @receiver the graph being built.
 * @param nav the controller used to leave the screen.
 */
private fun NavGraphBuilder.studyRoute(nav: NavHostController) {
    composable(Routes.STUDY, arguments = listOf(idArgument("deckId"), idArgument("chapterId"), idArgument("partId"))) {
        StudyScreen(onBack = { nav.popBackStack() })
    }
}
