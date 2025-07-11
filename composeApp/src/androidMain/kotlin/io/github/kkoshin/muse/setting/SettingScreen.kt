package io.github.kkoshin.muse.setting

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.kkoshin.muse.AccountManager
import io.github.kkoshin.muse.BuildConfig
import io.github.kkoshin.muse.repo.MuseRepo
import io.github.kkoshin.muse.tts.CharacterQuota
import io.github.kkoshin.muse.tts.TTSManager
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import muse.composeapp.generated.resources.Res
import muse.composeapp.generated.resources.ic_telegram_logo
import muse.composeapp.generated.resources.setting
import okio.Path.Companion.toOkioPath
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.rememberKoinInject

@Serializable
object SettingArgs

@Composable
fun SettingScreen(
    modifier: Modifier = Modifier,
    onLaunchVoiceScreen: (Set<String>) -> Unit,
    onLaunchOpenSourceScreen: () -> Unit,
) {
    val context = LocalContext.current
    val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val ttsManager = rememberKoinInject<TTSManager>()
    val accountManager = rememberKoinInject<AccountManager>()
    val scope = rememberCoroutineScope()

    var availableVoiceIds: Set<String>? by remember {
        mutableStateOf(null)
    }

    var quota: CharacterQuota? by remember {
        mutableStateOf(null)
    }

    val apiKeyValue: String? by accountManager.apiKey.collectAsState(null)

    LaunchedEffect(apiKeyValue) {
        if (apiKeyValue.isNullOrEmpty()) return@LaunchedEffect
        accountManager.setElevenLabsApiKey(apiKeyValue!!)
        availableVoiceIds = ttsManager.queryAvailableVoiceIds() ?: emptySet()
        quota = ttsManager.queryQuota().getOrNull()
        quota?.status?.let {
            accountManager.setSubscriptionStatus(it)
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                backgroundColor = MaterialTheme.colors.surface,
                navigationIcon = {
                    IconButton(onClick = {
                        backPressedDispatcher?.onBackPressed()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                title = {
                    Text(text = stringResource(Res.string.setting))
                },
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                preferenceCategory(
                    key = "elevenlabs",
                    title = {
                        Text("ElevenLabs", color = MaterialTheme.colors.primary)
                    },
                )
                editTextPreference(
                    key = "api_key",
                    value = apiKeyValue ?: "",
                    onValueUpdate = { newValue ->
                        if (newValue.isNotEmpty()) {
                            scope.launch {
                                accountManager.setElevenLabsApiKey(newValue)
                            }
                        }
                    },
                    title = { Text(text = "API key") },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                        )
                    },
                    summary = {
                        SummaryText(
                            text = if (apiKeyValue.isNullOrEmpty()) "Not set" else apiKeyValue!!.replaceRange(
                                0,
                                apiKeyValue!!.length - 2,
                                "•".repeat(apiKeyValue!!.length - 2),
                            )
                        )
                    },
                    dialogTitle = "ElevenLabs API Key",
                    inputLabel = "API Key",
                    widgetContainer = {
                        IconButton(onClick = {
                            context.openURL("https://elevenlabs.io/app/speech-synthesis/text-to-speech")
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Launch, "launch")
                        }
                    },
                )
                if (apiKeyValue != null) {
                    preference(
                        key = "quota",
                        enabled = availableVoiceIds != null,
                        icon = {
                            Icon(Icons.Outlined.Numbers, "voice")
                        },
                        title = {
                            Text("Character quota")
                        },
                        summary = {
                            SummaryText(
                                quota?.let {
                                    "${it.remaining}/${it.total}"
                                } ?: "-/-"
                            )
                        },
                    )
                    preference(
                        key = "voice_setting",
                        enabled = availableVoiceIds != null,
                        icon = {
                            Icon(Icons.Outlined.Audiotrack, "voice")
                        },
                        title = {
                            Text("Voices accent")
                        },
                        summary = {
                            availableVoiceIds?.let {
                                SummaryText(
                                    if (it.isEmpty()) {
                                        "No voices selected"
                                    } else {
                                        "${it.size} voice(s) selected"
                                    }
                                )
                            }
                        },
                        onClick = {
                            onLaunchVoiceScreen(availableVoiceIds!!)
                        },
                    )
                }
                preference(
                    key = "export_folder",
                    icon = {
                        Icon(Icons.Outlined.Folder, "export folder")
                    },
                    title = {
                        Text("Export folder")
                    },
                    summary = {
                        SummaryText(
                            Environment
                                .getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS,
                                ).toOkioPath()
                                .resolve("../${MuseRepo.getExportRelativePath(context)}", true)
                                .toString(),
                        )
                    },
                )

                preferenceCategory(
                    key = "about",
                    title = {
                        Text("About", color = MaterialTheme.colors.primary)
                    },
                )
                preference(
                    key = "license",
                    title = {
                        Text("Open source license")
                    },
                    onClick = {
                        onLaunchOpenSourceScreen()
                    },
                )
                preference(
                    key = "feedback",
                    title = {
                        Text("Send feedback")
                    },
                    icon = {
                        Icon(Icons.Default.MailOutline, contentDescription = null)
                    },
                    summary = {
                        SummaryText("Bug report, feature request, etc.")
                    },
                    onClick = {
                        context.openURL("https://github.com/kkoshin/Muse/issues")
                    },
                )
                preference(
                    key = "telegram",
                    icon = {
                        Icon(
                            painter = painterResource(Res.drawable.ic_telegram_logo),
                            contentDescription = null,
                        )
                    },
                    title = {
                        Text("Discuss on Telegram")
                    },
                    summary = {
                        SummaryText(text = "Primary timezone: UTC+8")
                    },
                    onClick = {
                        context.openURL("https://t.me/muse_app")
                    },
                )
                preference(
                    key = "version",
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                        )
                    },
                    title = {
                        Text("Version")
                    },
                    summary = {
                        SummaryText(text = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})")
                    },
                    onClick = {
                        context.openURL("https://github.com/kkoshin/Muse/releases")
                    },
                )
            }
        },
    )
}

@Composable
private fun SummaryText(text: String) {
    Text(
        text,
        color = if (MaterialTheme.colors.isLight) Color.DarkGray.copy(0.7f) else Color.LightGray.copy(
            alpha = 0.7f
        ),
        style = MaterialTheme.typography.body2,
    )
}

internal fun Context.openURL(url: String) {
    val intent = CustomTabsIntent
        .Builder()
        .build()
    intent.launchUrl(this, Uri.parse(url))
}