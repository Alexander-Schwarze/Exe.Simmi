package commands

import TwitchBotConfig
import Command
import DiscordMessageContent
import sendMessageToDiscordBot

val feedbackCommand: Command = Command(
    names = listOf("fb", "feedback"),
    handler = { arguments ->
        val currentMessage: DiscordMessageContent = DiscordMessageContent(
            message = arguments.joinToString(" "),
            user = user.name,
            channel = DiscordBotConfig.feedbackChannelName
        )
        sendMessageToDiscordBot(currentMessage)
        chat.sendMessage(TwitchBotConfig.channel, "Message sent in ${DiscordBotConfig.feedbackChannelName}")
    }
)