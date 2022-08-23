import androidx.compose.runtime.DisposableEffect
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
import config.TwitchBotConfig
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.time.format.DateTimeFormatterBuilder
import javax.swing.JOptionPane
import kotlin.system.exitProcess
import kotlin.time.Duration


val logger: Logger = LoggerFactory.getLogger("Bot")
val backgroundCoroutineScope = CoroutineScope(Dispatchers.IO)

val json = Json {
    prettyPrint = true
}

private object State {
    val openSessions = mutableStateListOf<DefaultWebSocketServerSession>()
}

suspend fun main() = try {
    setupLogging()

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

    // TODO: This probably needs to get refactored
    RemindHandler.setupReminders()
    RemindHandler.chat = twitchClient.chat
    RemindHandler.checkForReminds()

    hostServer()
    logger.info("WebSocket hosted.")

    application {
        DisposableEffect(Unit) {
            onDispose {
                twitchClient.chat.sendMessage(TwitchBotConfig.channel, "Bot shutting down ${TwitchBotConfig.leaveEmote}")
                logger.info("App shutting down...")
            }
        }

        Window(
            state = WindowState(size = DpSize(500.dp, 250.dp)),
            title = "Exe.Simmi",
            onCloseRequest = ::exitApplication,
            icon = painterResource("icon.ico"),
            resizable = false
        ) {
            App()
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

        // This feature has been built because of ShardZero abusing bot features. The bot will not allow commands from blacklisted users
        if(messageEvent.user.name in TwitchBotConfig.blacklistedUsers || messageEvent.user.id in TwitchBotConfig.blacklistedUsers){
            twitchClient.chat.sendMessage(
                TwitchBotConfig.channel,
                "Imagine not being a blacklisted user. Couldn't be you ${messageEvent.user.name} ${TwitchBotConfig.blacklistEmote}"
            )
            if(messageEvent.user.id !in TwitchBotConfig.blacklistedUsers) {
                logger.warn("Blacklisted user ${messageEvent.user.name} tried using a command. Please use following ID in the properties file instead of the name: ${messageEvent.user.id}")
            }
            return@onEvent
        }

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
            Clock.System.now()
        }

        val nextAllowedGlobalCommandUsageInstant = nextAllowedCommandUsageInstantPerCommand.getOrPut(command) {
            Clock.System.now()
        }

        if ((Clock.System.now() - nextAllowedGlobalCommandUsageInstant).isNegative() && CommandPermission.MODERATOR !in messageEvent.permissions) {
            val secondsUntilTimeoutOver = nextAllowedGlobalCommandUsageInstant - Clock.System.now()

            twitchClient.chat.sendMessage(
                TwitchBotConfig.channel,
                "The command is still on cooldown. Please try again in $secondsUntilTimeoutOver."
            )
            logger.info("Unable to execute command due to ongoing command cooldown.")

            return@onEvent
        }

        if ((Clock.System.now() - nextAllowedCommandUsageInstant).isNegative() && CommandPermission.MODERATOR !in messageEvent.permissions) {
            val secondsUntilTimeoutOver = nextAllowedCommandUsageInstant - Clock.System.now()

            twitchClient.chat.sendMessage(
                TwitchBotConfig.channel,
                "You are still on cooldown. Please try again in $secondsUntilTimeoutOver."
            )
            logger.info("Unable to execute command due to ongoing user cooldown.")

            return@onEvent
        }

        val commandHandlerScope = CommandHandlerScope(
            discordClient = discordClient,
            chat = twitchClient.chat,
            messageEvent = messageEvent
        )

        backgroundCoroutineScope.launch {
            command.handler(commandHandlerScope, parts.drop(1))

            val key = command to messageEvent.user.name
            nextAllowedCommandUsageInstantPerUser[key] = nextAllowedCommandUsageInstantPerUser[key]!! + commandHandlerScope.addedUserCooldown

            nextAllowedCommandUsageInstantPerCommand[command] = nextAllowedCommandUsageInstantPerCommand[command]!! + commandHandlerScope.addedCommandCooldown
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
        install(PartialContent)
        install(AutoHeadResponse)

        routing {
            clipOverlayPage()

            webSocket("/socket") {
                val clipPlayerInstance = ClipPlayer.instance ?: run {
                    close(CloseReason(CloseReason.Codes.INTERNAL_ERROR, "Clip player not setup."))
                    logger.error("Clip player not setup.")
                    return@webSocket
                }

                logger.info("Got new connection.")
                State.openSessions.add(this)

                try {
                    for (frame in incoming) {
                        clipPlayerInstance.popNextRandomClip().let {
                            send(it)
                            logger.debug("Received video request from '${call.request.origin.remoteHost}', sending video '$it'.")
                        }
                    }
                } finally {
                    logger.info("User disconnected.")
                    State.openSessions.remove(this)
                }
            }

            static("/video") {
                files(ClipPlayerConfig.clipLocation)
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
        .format(Clock.System.now().toJavaInstant())
        .replace(':', '-')

    val logFile = Paths.get(LOG_DIRECTORY, "${logFileName}.log").toFile().also {
        if (!it.exists()) {
            it.createNewFile()
        }
    }

    System.setOut(PrintStream(MultiOutputStream(System.out, FileOutputStream(logFile))))

    logger.info("Log file '${logFile.name}' has been created.")
}