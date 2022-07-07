import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.kord.rest.ratelimit.Reset
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

            Row (
                modifier = Modifier
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    // TODO: get currently playing clip name
                    text = "Currently Playing: ..."
                )
            }

            Row (
                modifier = Modifier
                    .padding(bottom = 6.dp)
            ) {
                Button(
                    onClick = {
                        // TODO: play and pause
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        /*
                        when (overlayStatus) {
                            is OverlayStatus.Running -> "Stop"
                            is OverlayStatus.Stopped -> "Start"
                        }
                        */
                        text = "Play / Pause"
                    )
                }
            }

            Row () {
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