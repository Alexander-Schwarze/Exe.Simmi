import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI
import kotlin.time.Duration.Companion.seconds

@Composable
@Preview
fun App() {
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
                        },
                    textDecoration = TextDecoration.Underline,
                    color = Color(0xff0b5b8e)
                )
            }

            Row(
                modifier = Modifier
                    .padding(bottom = 8.dp)
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