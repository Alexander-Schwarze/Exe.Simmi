
import com.github.twitch4j.chat.TwitchChat
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent
import com.github.twitch4j.common.events.domain.EventUser
import commands.*
import dev.kord.core.Kord
import kotlin.time.Duration

data class Command(
    val names: List<String>,
    val description: String,
    val handler: suspend CommandHandlerScope.(arguments: List<String>) -> Unit
)

data class CommandHandlerScope(
    val discordClient: Kord,
    val chat: TwitchChat,
    val messageEvent: ChannelMessageEvent,
    val remindHandler: RemindHandler,
    var addedUserCooldown: Duration = Duration.ZERO,
    var addedCommandCooldown: Duration = Duration.ZERO
)

val commands = listOf(
    helpCommand,
    feedbackCommand,
    gameSuggestionCommand,
    sendClipCommand,
    remindCommand
)