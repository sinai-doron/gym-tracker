package com.doron.gymtracker.ui.screens.settings

import com.doron.gymtracker.ui.components.appLanguage
import android.content.res.AssetManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.doron.gymtracker.R
import com.doron.gymtracker.data.catalogue.BodyMap
import com.doron.gymtracker.data.catalogue.BodyMapSource
import com.doron.gymtracker.data.catalogue.CatalogueCredits
import com.doron.gymtracker.data.catalogue.CreditedExercise
import com.doron.gymtracker.data.catalogue.ExerciseCatalogue
import com.doron.gymtracker.ui.theme.Purple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Open-source licences: what the app ships that others made, with the credit and licence text each
 * one asks for. The body figures (MIT) and the exercise library (CC BY-SA, from wger), read from
 * the assets that hold them so the credits can't drift from what ships.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicencesScreen(onBack: () -> Unit) {
    val assets = LocalContext.current.assets
    val content by produceState<Licences?>(null) { value = withContext(Dispatchers.IO) { Licences.read(assets) } }
    val lang = appLanguage()
    val uri = LocalUriHandler.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.licences_title)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        val licences = content ?: return@Scaffold
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "intro") { Body(stringResource(R.string.licences_intro)) }

            item(key = "body") {
                Section(stringResource(R.string.licences_body_title))
                Body(stringResource(R.string.licences_body_text, licences.bodyMap.name, licences.bodyMap.version))
                Link(licences.bodyMap.url.removePrefix("https://")) { uri.openUri(licences.bodyMap.url) }
            }
            item(key = "body-licence") {
                // The licence is English: left to right even when the app is in Hebrew
                Text(
                    licences.bodyMapLicence,
                    style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr, textAlign = TextAlign.Left),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                )
            }

            item(key = "catalogue") {
                Section(stringResource(R.string.licences_catalogue_title))
                Body(stringResource(R.string.licences_catalogue_text))
                Link(stringResource(R.string.licences_cc_by_sa)) { uri.openUri(CC_BY_SA_4) }
                Link("wger.de") { uri.openUri(WGER) }
                Text(
                    stringResource(R.string.licences_by_exercise),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(licences.catalogue.credited, key = { it.slug }) { exercise ->
                Credit(exercise, lang) { uri.openUri(exercise.source!!.url) }
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Link(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 0.dp)) {
        Text(text, color = Purple, style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr))
    }
}

@Composable
private fun Credit(exercise: CreditedExercise, lang: String, onClick: () -> Unit) {
    val source = exercise.source ?: return
    val name = exercise.names[lang] ?: exercise.names.getValue("en")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .clickable(onClickLabel = stringResource(R.string.licences_open, name), onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
        Text(
            stringResource(
                R.string.licences_credit,
                source.authors.joinToString(", ").ifEmpty { stringResource(R.string.licences_wger_contributors) },
                source.licenses.joinToString(", ")
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private class Licences(val bodyMap: BodyMapSource, val bodyMapLicence: String, val catalogue: CatalogueCredits) {
    companion object {
        fun read(assets: AssetManager): Licences {
            fun text(path: String) = assets.open(path).bufferedReader().use { it.readText() }
            return Licences(
                bodyMap = BodyMap.parse(text(BodyMap.ASSET)).source,
                bodyMapLicence = paragraphs(text(BodyMap.LICENSE_ASSET)),
                catalogue = CatalogueCredits.parse(text(ExerciseCatalogue.ASSET))
            )
        }
    }
}

/** The licence file's lines are broken at 80 characters; rejoin each paragraph so it wraps to the screen. */
private fun paragraphs(text: String): String =
    text.trim().split(Regex("\\n\\s*\\n")).joinToString("\n\n") { it.lines().joinToString(" ") { line -> line.trim() } }

private const val CC_BY_SA_4 = "https://creativecommons.org/licenses/by-sa/4.0/"
private const val WGER = "https://wger.de"
