import androidx.compose.runtime.DisposableEffect
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
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatterBuilder
import javax.swing.JOptionPane
import kotlin.system.exitProcess
import kotlin.time.toJavaDuration


val logger: Logger = LoggerFactory.getLogger("Bot")
val commandHandlerCoroutineScope = CoroutineScope(Dispatchers.IO)


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

    application {
        DisposableEffect(Unit) {
            onDispose {
                twitchClient.chat.sendMessage(TwitchBotConfig.channel, "Bot shutting down peepoLeave")
                logger.info("App shutting down...")
            }
        }

        Window(
            state = WindowState(size = DpSize(400.dp, 200.dp)),
            title = "Exe.Simmi",
            onCloseRequest = ::exitApplication
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

    twitchClient.chat.run {
        connect()
        joinChannel(TwitchBotConfig.channel)
        sendMessage(TwitchBotConfig.channel, "Bot running peepoArrive")
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

        if (Instant.now().isBefore(nextAllowedCommandUsageInstant) && CommandPermission.MODERATOR !in messageEvent.permissions) {
            val secondsUntilTimeoutOver = Duration.between(Instant.now(), nextAllowedCommandUsageInstant).seconds

            twitchClient.chat.sendMessage(
                TwitchBotConfig.channel,
                "You are still on cooldown. Please try again in $secondsUntilTimeoutOver seconds."
            )
            logger.info("Unable to execute command due to ongoing cooldown.")

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
        }
    }

    logger.info("Twitch client started.")
    return twitchClient
}

suspend fun CommandHandlerScope.sendMessageToDiscordBot(discordMessageContent: DiscordMessageContent): TextChannel {
    val user = discordMessageContent.user
    val message = discordMessageContent.message

    val channel = discordClient.getChannelOf<TextChannel>(DiscordBotConfig.feedbackChannelId, EntitySupplyStrategy.cacheWithCachingRestFallback)
        ?: error("Invalid channel ID.")

    val channelName = channel.name
    val channelId = channel.id

    logger.info("User: $user | Message: $message | Channel Name: $channelName | Channel ID: $channelId")

    val twitchAuthor = EmbedBuilder.Author().apply {
        name = "Twitch user ${discordMessageContent.user}"
    }

    channel.createEmbed {
        title = "Suggestion for $channelName"
        author {
            name = "Twitch user ${discordMessageContent.user}"
        }
        description = discordMessageContent.message
        color = Color(233,166,35)
    }

    return channel
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