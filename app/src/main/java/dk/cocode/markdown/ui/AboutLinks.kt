package dk.cocode.markdown.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

enum class AboutLink { Update, Website, Source, Issues, Privacy }

/**
 * The two facts about where the app is published that decide the About links: whether F-Droid lists the app
 * (`fdroid` in apps.yml) and the privacy policy page (`privacy` in apps.yml, null until it exists).
 */
data class AboutTargets(val onFdroid: Boolean, val privacyUrl: String?)

/** What this build of the app links to. Change it when apps.yml changes. */
val appTargets = AboutTargets(onFdroid = false, privacyUrl = null)

private const val APPLICATION_ID = "dk.cocode.markdown"
private const val SITE = "https://markdown.cocode.dk"
private const val REPO = "https://github.com/cocodedk/markdown"

/** Where [link] leads. Null only for [AboutLink.Privacy] while there is no policy page to open. */
fun aboutUrl(link: AboutLink, targets: AboutTargets): String? = when (link) {
    AboutLink.Update ->
        if (targets.onFdroid) "https://f-droid.org/packages/$APPLICATION_ID/" else "$REPO/releases/latest"
    AboutLink.Website -> SITE
    AboutLink.Source -> REPO
    AboutLink.Issues -> "$REPO/issues"
    AboutLink.Privacy -> targets.privacyUrl
}

/** The version name Android holds for this app, such as "0.2.0". */
fun versionName(context: Context): String {
    val packages = context.packageManager
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packages.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packages.getPackageInfo(context.packageName, 0)
    }
    return info.versionName.orEmpty()
}
