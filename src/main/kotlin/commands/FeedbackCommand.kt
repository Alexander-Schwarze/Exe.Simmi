package commands

import TwitchBotConfig
import Command
import DiscordMessageContent
import sendMessageToDiscordBot

val feedbackCommand: Command = Command(
    names = listOf("fb", "feedback"),
    handler = { arguments ->
        // TODO: Get channel name from discord bot config
        val currentMessage: DiscordMessageContent = DiscordMessageContent(
            message = arguments.joinToString(" "),
            user = user.name,
            channel = "#feedback"
        )
        sendMessageToDiscordBot(currentMessage)
        chat.sendMessage(TwitchBotConfig.channel, "Message sent in #feedback")
    }
)