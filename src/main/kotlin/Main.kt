import androidx.compose.runtime.DisposableEffect
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
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import config.GoogleSpreadSheetConfig
import config.TwitchBotConfig
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import handler.RemindHandler
import handler.RunNameUser
import handler.RunNamesRedeemHandler
import handler.SpreadSheetHandler
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
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.commons.text.similarity.LevenshteinDistance
import org.jsoup.Jsoup
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.time.format.DateTimeFormatterBuilder
import java.util.*
import javax.swing.JOptionPane
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds


val logger: Logger = LoggerFactory.getLogger("Bot")

val json = Json {
    prettyPrint = true
}

suspend fun main() = try {
    setupLogging()
    SpreadSheetHandler.instance.setupConnectionAndLoadData()

    val discordToken = File("data/discordtoken.txt").readText()
    val discordClient = Kord(discordToken)

    logger.info("Discord client started.")

    CoroutineScope(discordClient.coroutineContext).launch {
        discordClient.login {
            @OptIn(PrivilegedIntent::class)
            intents += Intent.MessageContent
        }
    }

    val backgroundCoroutineScope = CoroutineScope(Dispatchers.IO)
    val twitchClient = setupTwitchBot(discordClient, backgroundCoroutineScope)

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
    exitProcess(-1)
}

private suspend fun setupTwitchBot(discordClient: Kord, backgroundCoroutineScope: CoroutineScope): TwitchClient {
    val chatAccountToken = File("data/twitchtoken.txt").readText()
    val oAuth2Credential = OAuth2Credential("twitch", chatAccountToken)

    val twitchClient = TwitchClientBuilder.builder()
        .withEnableHelix(true)
        .withEnableChat(true)
        .withEnablePubSub(true)
        .withChatAccount(oAuth2Credential)
        .build()

    val nextAllowedCommandUsageInstantPerUser = mutableMapOf<Pair<Command, /* user: */ String>, Instant>()
    val nextAllowedCommandUsageInstantPerCommand = mutableMapOf<Command, Instant>()

    val remindHandler = RemindHandler(chat = twitchClient.chat, reminderFile = File("data/reminders.json"), checkerScope = backgroundCoroutineScope)
    val runNamesRedeemHandler = RunNamesRedeemHandler(runNamesFile = File("data/runNames.json"))

    twitchClient.chat.run {
        connect()
        joinChannel(TwitchBotConfig.channel)
        sendMessage(TwitchBotConfig.channel, "Bot running ${TwitchBotConfig.arriveEmote}")
    }

    val channelId = twitchClient.helix.getUsers(chatAccountToken, null, listOf(TwitchBotConfig.channel)).execute().users.first().id
    twitchClient.pubSub.listenForChannelPointsRedemptionEvents(
        oAuth2Credential,
        channelId
    )

    twitchClient.pubSub.listenForChannelPointsRedemptionEvents(oAuth2Credential, channelId)

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
            val secondsUntilTimeoutOver = (nextAllowedGlobalCommandUsageInstant - Clock.System.now()).inWholeSeconds.seconds

            twitchClient.chat.sendMessage(
                TwitchBotConfig.channel,
                "The command is still on cooldown. Please try again in $secondsUntilTimeoutOver."
            )
            logger.info("Unable to execute command due to ongoing command cooldown.")

            return@onEvent
        }

        if ((Clock.System.now() - nextAllowedCommandUsageInstant).isNegative() && CommandPermission.MODERATOR !in messageEvent.permissions) {
            val secondsUntilTimeoutOver = (nextAllowedCommandUsageInstant - Clock.System.now()).inWholeSeconds.seconds

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
            messageEvent = messageEvent,
            remindHandler = remindHandler,
            runNamesRedeemHandler = runNamesRedeemHandler
        )

        backgroundCoroutineScope.launch {
            command.handler(commandHandlerScope, parts.drop(1))

            val key = command to messageEvent.user.name
            nextAllowedCommandUsageInstantPerUser[key] = nextAllowedCommandUsageInstantPerUser[key]!! + commandHandlerScope.addedUserCooldown

            nextAllowedCommandUsageInstantPerCommand[command] = nextAllowedCommandUsageInstantPerCommand[command]!! + commandHandlerScope.addedCommandCooldown
        }
    }

    twitchClient.eventManager.onEvent(RewardRedeemedEvent::class.java) { redeemEvent ->

        val redeem = redeems.find { redeemEvent.redemption.reward.id in it.id || redeemEvent.redemption.reward.title in it.id }.also {
            if (it != null) {
                if(redeemEvent.redemption.reward.title in it.id) {
                    logger.warn("Redeem ${redeemEvent.redemption.reward.title}. Please use following ID in the properties file instead of the name: ${redeemEvent.redemption.reward.id}")
                }
            }
        } ?: return@onEvent

        val redeemHandlerScope = RedeemHandlerScope(
            chat = twitchClient.chat,
            redeemEvent = redeemEvent,
            runNamesRedeemHandler = runNamesRedeemHandler
        )

        backgroundCoroutineScope.launch {
            redeem.handler(redeemHandlerScope)
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

                try {
                    for (frame in incoming) {
                        clipPlayerInstance.popNextRandomClip().let {
                            send(it)
                            logger.debug("Received video request from '${call.request.origin.remoteHost}', sending video '$it'.")
                        }
                    }
                } finally {
                    logger.info("User disconnected.")
                }
            }

            static("/video") {
                files(ClipPlayerConfig.clipLocation)
            }
        }
    }.start(wait = false)
}

private const val CURRENT_RUNNER_NAME_DISPLAY_FILE = "data\\currentRunnerNameDisplay.txt"
private const val CURRENT_RUNNER_NAME_FILE = "data\\currentRunner.json"
fun updateCurrentRunnerName(currentRunner: RunNameUser) {
    try {
        logger.info("Updating current runner name in display file \"$CURRENT_RUNNER_NAME_DISPLAY_FILE\"")
        val currentRunnerNameDisplayFile = File(CURRENT_RUNNER_NAME_DISPLAY_FILE)

        if(!currentRunnerNameDisplayFile.exists()) {
            currentRunnerNameDisplayFile.createNewFile()
        }

        currentRunnerNameDisplayFile.writeText(TwitchBotConfig.currentRunnerNamePreText + " " + currentRunner.name + " | " + TwitchBotConfig.currentRunnerNamePostText)

        logger.info("Updating current runner name in data file \"$CURRENT_RUNNER_NAME_FILE\"")
        val currentRunnerNameFile = File(CURRENT_RUNNER_NAME_FILE)

        if(!currentRunnerNameFile.exists()) {
            currentRunnerNameFile.createNewFile()
        }

        currentRunnerNameFile.writeText(json.encodeToString(currentRunner))
        logger.info("Finished updating current runner name in files")
    } catch (e: Exception) {
        logger.error("An error occurred while using the function updateCurrentRunnerName\n", e)
    }
}

suspend fun CommandHandlerScope.saveLastRunnersSplit(overrideSplit: String) {
    val splitName = overrideSplit.ifEmpty {
        logger.info("Extracting name from HitCounter")
        getCurrentSplitFromHitCounter()
    }

    val (index, levinshteinDistance) = getIndexFromSplitName(splitName)

    val currentRunner = json.decodeFromString<RunNameUser>(File(CURRENT_RUNNER_NAME_FILE).readText())

    val message = "Runner \"${currentRunner.name}\" died on split $splitName" +
            if(levinshteinDistance > 3) {
                "\nThe manual split name was not as close to an existing one. Check the Leaderboard if the value is set correct"
            } else {
                ""
            }

    SpreadSheetHandler.instance.updateSpreadSheetLeaderboard(currentRunner, index)

    val currentMessageContent = DiscordMessageContent(
        message = DiscordMessageContent.Message.FromText(message),
        title = "Runner Update for ",
        user = TwitchBotConfig.channel,
        channelId = DiscordBotConfig.endedRunChannelId
    )

    val channel = sendMessageToDiscordBot(currentMessageContent)
    chat.sendMessage(TwitchBotConfig.channel, "Ended run message for \"${currentRunner.name}\" sent in #${channel.name} ${TwitchBotConfig.confirmEmote}")
    logger.info("Finished saving last runners split")
}

const val ACTIVE_SPLIT_STRING = "\"split_active\": "
fun getCurrentSplitFromHitCounter(): String {
    return try {
        val scriptElement = Jsoup.parse(File(TwitchBotConfig.hitCounterLocation + "\\HitCounter.html")).getElementsByTag("script").first()

        val lines = scriptElement?.html()?.split("\n")
        org.jsoup.parser.Parser.unescapeEntities(
            lines?.find { it.startsWith(ACTIVE_SPLIT_STRING) }?.substringAfter(ACTIVE_SPLIT_STRING)?.substringBefore(",")?.toInt()
                ?.let { index ->
                    lines.filter {
                        it.contains("[") && it.contains("]")
                    }.let {
                        it + "beyond infinity (after last split)"
                    }[index].split(",")[0].let { item ->
                        val result = item.replace("\"", "")
                        result.replace("[", "")
                    }
                } ?: "Error".also { logger.error("Error occurred while getting the index and accessing the array") },
            true
        )
    } catch (e: Exception) {
        logger.error("An error occurred while reading from HitCounterManager.", e)
        "Error"
    }
}

fun getIndexFromSplitName(splitName: String): Pair<Int, Int> {
    return try {
        val scriptElement = Jsoup.parse(File(TwitchBotConfig.hitCounterLocation + "\\HitCounter.html")).getElementsByTag("script").first()
        val cleanedUpSplitNames = scriptElement?.html()?.split("\n")?.filter {
            it.contains("[") && it.contains("]")
        }?.map {
            org.jsoup.parser.Parser.unescapeEntities(
                it.split(",")[0].let { item ->
                    val result = item.replace("\"", "")
                    result.replace("[", "")
                },
                true
            )
        }

        val nameAndDistance = cleanedUpSplitNames?.map {
            it to LevenshteinDistance.getDefaultInstance().apply(it.lowercase(), splitName.lowercase())
        }?.minBy { (_, levenshteinDistance) -> levenshteinDistance }

        val index = cleanedUpSplitNames?.indexOf(nameAndDistance?.first!!)

        Pair(index!!, nameAndDistance?.second!!)
    } catch (e: Exception) {
        logger.error("An error occurred while reading from HitCounterManager to get the index.", e)
        Pair(-1, -1)
    }
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