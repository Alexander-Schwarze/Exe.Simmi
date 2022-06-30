package commands

import TwitchBotConfig
import Command
import DiscordMessageContent
import sendMessageToDiscordBot
import kotlin.time.Duration.Companion.seconds
import logger

val gameSuggestionCommand: Command = Command(
    names = listOf("gs", "gamesuggestion"),
    handler = { arguments ->
        val message = arguments.joinToString(" ")
        if (message.trim().isEmpty()) {
            chat.sendMessage(TwitchBotConfig.channel, "No input has been provided ${TwitchBotConfig.rejectEmote}")
            addedUserCooldown = 5.seconds
            return@Command
        }

        val currentMessageContent = DiscordMessageContent(
            message = message,
            user = user.name,
            channelId = DiscordBotConfig.gameChannelId
        )

        val channel = sendMessageToDiscordBot(currentMessageContent)
        val messageSentOnTwitchChat = chat.sendMessage(TwitchBotConfig.channel, "Message sent in #${channel.name} ${TwitchBotConfig.confirmEmote}")
        logger.info("Message sent to Twich Chat: $messageSentOnTwitchChat")

        addedUserCooldown = TwitchBotConfig.userCooldown
        addedCommandCooldown = TwitchBotConfig.commandCooldown
    }
)