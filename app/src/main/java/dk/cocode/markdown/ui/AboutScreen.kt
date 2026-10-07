package dk.cocode.markdown.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dk.cocode.markdown.R

private val credits = listOf(
    R.string.about_credit_license,
    R.string.about_credit_commonmark,
    R.string.about_credit_autolink,
    R.string.about_credit_androidx,
)

private val links = listOf(
    AboutLink.Website to R.string.about_website,
    AboutLink.Source to R.string.about_source,
    AboutLink.Issues to R.string.about_report,
)

@Composable
private fun SectionTitle(@StringRes title: Int) = Text(
    stringResource(title),
    style = MaterialTheme.typography.titleMedium,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(top = 16.dp).semantics { heading() },
)

@Composable
private fun Body(@StringRes text: Int) = Text(stringResource(text), style = MaterialTheme.typography.bodyLarge)

/**
 * The About page. Every link opens outside the app; when no app can open one, the page says so under the
 * links instead of failing silently.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit, targets: AboutTargets = appTargets) {
    val context = LocalContext.current
    val version = remember { versionName(context) }
    var noBrowser by rememberSaveable { mutableStateOf(false) }
    val open = { link: AboutLink ->
        val url = aboutUrl(link, targets)
        if (url != null) noBrowser = !openOutside(context, url.toUri())
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title), modifier = Modifier.semantics { heading() }) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.action_back)) } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 1. Name and version, and the way to the newest version.
            SectionTitle(R.string.about_name_title)
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.about_version, version), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = { open(AboutLink.Update) }) { Text(stringResource(R.string.about_check_updates)) }
            Body(R.string.about_update_note)
            // 2. What the app does.
            SectionTitle(R.string.about_what_title)
            Body(R.string.about_what)
            // 3. Privacy, and the policy page once there is one.
            SectionTitle(R.string.about_privacy_title)
            Body(R.string.about_privacy_body)
            if (aboutUrl(AboutLink.Privacy, targets) != null) {
                OutlinedButton(onClick = { open(AboutLink.Privacy) }) { Text(stringResource(R.string.about_privacy_link)) }
            }
            // 4. Links.
            SectionTitle(R.string.about_links_title)
            Body(R.string.about_links_note)
            links.forEach { (link, label) ->
                OutlinedButton(onClick = { open(link) }) { Text(stringResource(label)) }
            }
            if (noBrowser) Body(R.string.about_link_failed)
            // 5. Credits and licenses.
            SectionTitle(R.string.about_credits)
            credits.forEach { Body(it) }
            // 6. Made by Cocode.
            SectionTitle(R.string.about_made_by_title)
            Body(R.string.about_made_by)
            // 7. Support: kept empty until the Support phase of the cocode-apps standard; nothing shows here yet.
        }
    }
}
