package commands

import TwitchBotConfig
import Command
import DiscordMessageContent
import sendMessageToDiscordBot

val gameCommand: Command = Command(
    names = listOf("game"),
    handler = { arguments ->
        // TODO: Get channel name from discord bot config
        val currentMessage = DiscordMessageContent(
            message = arguments.joinToString(" "),
            user = user.name,
            channel = "#spievorschläge"
        )
        sendMessageToDiscordBot(currentMessage)
        chat.sendMessage(TwitchBotConfig.channel, "Message sent in #feeback")
    }
)