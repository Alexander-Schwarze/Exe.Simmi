package commands

import TwitchBotConfig
import Command
import DiscordMessageContent
import sendMessageToDiscordBot
import kotlin.time.Duration.Companion.seconds

val feedbackCommand: Command = Command(
    names = listOf("fb", "feedback"),
    handler = { arguments ->
        val message = arguments.joinToString(" ")
        if (message.trim().isEmpty()) {
            chat.sendMessage(TwitchBotConfig.channel, "No input has been provided")
            addedUserCooldown = 5.seconds
            return@Command
        }

        val currentMessageContent = DiscordMessageContent(
            message = message,
            user = user.name,
            channelId = DiscordBotConfig.feedbackChannelId
        )

        val channel = sendMessageToDiscordBot(currentMessageContent)
        chat.sendMessage(TwitchBotConfig.channel, "Message sent in #${channel.name}.")

        addedUserCooldown = TwitchBotConfig.userCooldown
        addedCommandCooldown = TwitchBotConfig.commandCooldown
    }
)