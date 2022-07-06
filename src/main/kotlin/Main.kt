import androidx.compose.material.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.github.philippheuer.credentialmanager.domain.OAuth2Credential
import com.github.twitch4j.TwitchClient
import com.github.twitch4j.TwitchClientBuilder
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import com.github.twitch4j.common.enums.CommandPermission
import dev.kord.common.Color
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.EmbedBuilder.Limits.title
import io.ktor.server.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.server.engine.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.server.websocket.WebSockets
import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatterBuilder
import javax.sound.sampled.Clip
import javax.swing.JOptionPane
import kotlin.random.Random
import kotlin.system.exitProcess
import kotlin.time.toJavaDuration


val logger: Logger = LoggerFactory.getLogger("Bot")
val commandHandlerCoroutineScope = CoroutineScope(Dispatchers.IO)
var clipPlayer = ClipPlayer()

private object State {
    val scaffoldState = ScaffoldState(DrawerState(DrawerValue.Closed) { true }, SnackbarHostState())
    val openSessions = mutableStateListOf<DefaultWebSocketServerSession>()
}

suspend fun main() = try {
    setupLogging()
    clipPlayer.setupClipPlayer()

    val discordToken = File("data/discordtoken.txt").readText()
    val discordClient = Kord(discordToken)

    logger.info("Discord client started.")

    CoroutineScope(discordClient.coroutineContext).launch {
        discordClient.login {
            @OptIn(PrivilegedIntent::class)
            intents += Intent.MessageContent
        }
    }

    val twitchClient = setupTwitchBot(discordClient)

    application {
        LaunchedEffect(Unit){
            hostServer()
        }
        logger.info("Websocket hosted")

        DisposableEffect(Unit) {
            onDispose {
                twitchClient.chat.sendMessage(TwitchBotConfig.channel, "Bot shutting down ${TwitchBotConfig.leaveEmote}")
                logger.info("App shutting down...")
            }
        }

        Window(
            state = WindowState(size = DpSize(500.dp, 800.dp)),
            title = "Exe.Simmi",
            onCloseRequest = ::exitApplication,
            icon = painterResource("icon.ico")
        ) {
            Scaffold(
                scaffoldState = State.scaffoldState
            ) {
                App(
                    scaffoldState = State.scaffoldState
                )
            }
        }
    }
} catch (e: Throwable) {
    JOptionPane.showMessageDialog(null, e.message + "\n" + StringWriter().also { e.printStackTrace(PrintWriter(it)) }, "InfoBox: File Debugger", JOptionPane.INFORMATION_MESSAGE)
    logger.error("Error while executing program.", e)
    exitProcess(0)
}

private suspend fun setupTwitchBot(discordClient: Kord): TwitchClient {
    val chatAccountToken = File("data/twitchtoken.txt").readText()

    val twitchClient = TwitchClientBuilder.builder()
        .withEnableHelix(true)
        .withEnableChat(true)
        .withChatAccount(OAuth2Credential("twitch", chatAccountToken))
        .build()

    val nextAllowedCommandUsageInstantPerUser = mutableMapOf<Pair<Command, /* user: */ String>, Instant>()
    val nextAllowedCommandUsageInstantPerCommand = mutableMapOf<Command, Instant>()

    twitchClient.chat.run {
        connect()
        joinChannel(TwitchBotConfig.channel)
        sendMessage(TwitchBotConfig.channel, "Bot running ${TwitchBotConfig.arriveEmote}")
    }

    twitchClient.eventManager.onEvent(ChannelMessageEvent::class.java) { messageEvent ->
        val message = messageEvent.message
        if (!message.startsWith(TwitchBotConfig.commandPrefix)) {
            return@onEvent
        }

        val parts = message.substringAfter(TwitchBotConfig.commandPrefix).split(" ")
        val command = commands.find { parts.first().lowercase() in it.names } ?: return@onEvent

        if (TwitchBotConfig.onlyMods && CommandPermission.MODERATOR !in messageEvent.permissions) {
            twitchClient.chat.sendMessage(
                TwitchBotConfig.channel,
                "You do not have the required permissions to use this command."
            )
            logger.info("User '${messageEvent.user.name}' does not have the necessary permissions to call command '${command.names.first()}'")

            return@onEvent
        }

        logger.info("User '${messageEvent.user.name}' tried using command '${command.names.first()}' with arguments: ${parts.drop(1).joinToString()}")

        val nextAllowedCommandUsageInstant = nextAllowedCommandUsageInstantPerUser.getOrPut(command to messageEvent.user.name) {
            Instant.now()
        }

        val nextAllowedGlobalCommandUsageInstant = nextAllowedCommandUsageInstantPerCommand.getOrPut(command) {
            Instant.now()
        }

        if (Instant.now().isBefore(nextAllowedGlobalCommandUsageInstant) && CommandPermission.MODERATOR !in messageEvent.permissions) {
            val secondsUntilTimeoutOver = Duration.between(Instant.now(), nextAllowedGlobalCommandUsageInstant).seconds

            twitchClient.chat.sendMessage(
                TwitchBotConfig.channel,
                "The command is still on cooldown. Please try again in $secondsUntilTimeoutOver seconds."
            )
            logger.info("Unable to execute command due to ongoing command cooldown.")

            return@onEvent
        }

        if (Instant.now().isBefore(nextAllowedCommandUsageInstant) && CommandPermission.MODERATOR !in messageEvent.permissions) {
            val secondsUntilTimeoutOver = Duration.between(Instant.now(), nextAllowedCommandUsageInstant).seconds

            twitchClient.chat.sendMessage(
                TwitchBotConfig.channel,
                "You are still on cooldown. Please try again in $secondsUntilTimeoutOver seconds."
            )
            logger.info("Unable to execute command due to ongoing user cooldown.")

            return@onEvent
        }

        val commandHandlerScope = CommandHandlerScope(
            discordClient = discordClient,
            chat = twitchClient.chat,
            user = messageEvent.user
        )

        commandHandlerCoroutineScope.launch {
            command.handler(commandHandlerScope, parts.drop(1))

            val key = command to messageEvent.user.name
            nextAllowedCommandUsageInstantPerUser[key] = nextAllowedCommandUsageInstantPerUser[key]!!.plus(commandHandlerScope.addedUserCooldown.toJavaDuration())

            nextAllowedCommandUsageInstantPerCommand[command] = nextAllowedCommandUsageInstantPerCommand[command]!!.plus(commandHandlerScope.addedCommandCooldown.toJavaDuration())
        }
    }

    logger.info("Twitch client started.")
    return twitchClient
}

suspend fun CommandHandlerScope.sendMessageToDiscordBot(discordMessageContent: DiscordMessageContent): TextChannel {
    val user = discordMessageContent.user
    val messageTitle = discordMessageContent.title
    val message = discordMessageContent.message

    val channel = discordClient.getChannelOf<TextChannel>(discordMessageContent.channelId, EntitySupplyStrategy.cacheWithCachingRestFallback)
        ?: error("Invalid channel ID.")

    val channelName = channel.name
    val channelId = channel.id

    logger.info("User: $user | Title: $messageTitle | Message/Link: $message | Channel Name: $channelName | Channel ID: $channelId")

    channel.createEmbed {
        title = messageTitle + channelName
        author {
            name = "Twitch user $user"
        }
        description = when (message) {
            is DiscordMessageContent.Message.FromLink -> ""
            is DiscordMessageContent.Message.FromText -> message.text
        }
        color = DiscordBotConfig.embedAccentColor
    }

    if (message is DiscordMessageContent.Message.FromLink) {
        channel.createMessage(message.link)
    }

    logger.info("Embed/Message created on Discord Channel $channelName")

    return channel
}

private fun hostServer() {
    embeddedServer(CIO, port = ClipPlayerConfig.port) {
        install(WebSockets)

        routing {
            clipOverlayPage()

            webSocket("/socket") {
                logger.info("Got new connection.")
                State.openSessions.add(this)

                try {
                    @Suppress("ControlFlowWithEmptyBody")
                    for (frame in incoming) {
                        // ignore
                    }
                } finally {
                    logger.info("User disconnected.")
                    State.openSessions.remove(this)
                }
            }
        }
    }.start(wait = false)
}

private const val LOG_DIRECTORY = "logs"

fun setupLogging() {
    Files.createDirectories(Paths.get(LOG_DIRECTORY))

    val logFileName = DateTimeFormatterBuilder()
        .appendInstant(0)
        .toFormatter()
        .format(Instant.now())
        .replace(':', '-')

    val logFile = Paths.get(LOG_DIRECTORY, "${logFileName}.log").toFile().also {
        if (!it.exists()) {
            it.createNewFile()
        }
    }

    System.setOut(PrintStream(MultiOutputStream(System.out, FileOutputStream(logFile))))

    logger.info("Log file '${logFile.name}' has been created.")
}