import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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

val port = ClipPlayerConfig.port
@Composable
@Preview
fun App(
    scaffoldState: ScaffoldState
) {
    val coroutineScope = rememberCoroutineScope()

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
                text = "localhost:$port",
                modifier = Modifier
                    .clickable {
                        coroutineScope.launch {
                            Toolkit.getDefaultToolkit().systemClipboard.setContents(
                                StringSelection("http://localhost:$port"),
                                null
                            )
                            scaffoldState.snackbarHostState.showSnackbar(
                                message = "URL copied! Opening browser...",
                                duration = SnackbarDuration.Short
                            )

                            delay(2.seconds)

                            withContext(Dispatchers.IO) {
                                Desktop.getDesktop().browse(URI.create("http://localhost:$port"))
                            }
                        }
                    },
                textDecoration = TextDecoration.Underline,
                color = Color(0xff0b5b8e)
            )
        }
    }
}