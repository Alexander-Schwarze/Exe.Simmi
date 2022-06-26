package commands

import TwitchBotConfig
import Command
import DiscordMessageContent
import sendMessageToDiscordBot

val gameCommand: Command = Command(
    names = listOf("g", "game"),
    handler = { arguments ->
        val currentMessage = DiscordMessageContent(
            message = arguments.joinToString(" "),
            user = user.name,
            channel = DiscordBotConfig.gameChannelName
        )
        sendMessageToDiscordBot(currentMessage)
        chat.sendMessage(TwitchBotConfig.channel, "Message sent in ${DiscordBotConfig.gameChannelName}")
    }
)