import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIconDefaults
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.github.tkuenneth.nativeparameterstoreaccess.NativeParameterStoreAccess
import com.github.tkuenneth.nativeparameterstoreaccess.WindowsRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import kotlin.time.Duration.Companion.seconds

val lightColorPalette = lightColors(
    primary = Color(0xff4466ff),
    onPrimary = Color.White,
    secondary = Color(0xff0b5b8e),
    background = Color.White,
    onBackground = Color.Black,
)

val darkColorPalette = darkColors(
    primary = Color(0xff2244bb),
    onPrimary = Color.White,
    secondary = Color(0xff5bbbfe),
    background = Color.DarkGray,
    onBackground = Color.White,
)

@Composable
@Preview
fun App() {
    var isInDarkMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            isInDarkMode = if (NativeParameterStoreAccess.IS_WINDOWS) {
                WindowsRegistry.getWindowsRegistryEntry("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize", "AppsUseLightTheme") == 0x0
            } else {
                false
            }

            delay(1.seconds)
        }
    }

    MaterialTheme(
        colors = if (isInDarkMode) {
            darkColorPalette
        } else {
            lightColorPalette
        }
    ) {
        val coroutineScope = rememberCoroutineScope()
        val scaffoldState = rememberScaffoldState()

        Scaffold(
            scaffoldState = scaffoldState
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxSize()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        style = MaterialTheme.typography.body1,
                        text = "Clips hosted on "
                    )

                    Text(
                        style = MaterialTheme.typography.body1,
                        text = "http://localhost:${ClipPlayerConfig.port}",
                        modifier = Modifier
                            .clickable {
                                coroutineScope.launch {
                                    Toolkit.getDefaultToolkit().systemClipboard.setContents(
                                        StringSelection("http://localhost:${ClipPlayerConfig.port}"),
                                        null
                                    )

                                    scaffoldState.snackbarHostState.showSnackbar(
                                        message = "URL copied! Opening browser...",
                                        duration = SnackbarDuration.Short
                                    )

                                    delay(2.seconds)

                                    withContext(Dispatchers.IO) {
                                        Desktop.getDesktop().browse(URI.create("http://localhost:${ClipPlayerConfig.port}"))
                                    }
                                }
                            }
                            .padding(3.dp)
                            .pointerHoverIcon(PointerIconDefaults.Hand),
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colors.secondary
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = "Currently Playing: ${ClipPlayer.instance?.currentlyPlayingClip?.collectAsState()?.value ?: "Nothing"}"
                    )
                }

                Row {
                    Button(
                        onClick = {
                            ClipPlayer.instance?.resetPlaylistFile()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Reset Playlist"
                        )
                    }
                }
            }
        }
    }
}