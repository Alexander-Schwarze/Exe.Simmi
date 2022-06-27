package commands

import TwitchBotConfig
import Command
import DiscordMessageContent
import sendMessageToDiscordBot

val gameCommand: Command = Command(
    names = listOf("g", "game"),
    handler = { arguments ->
        val message = arguments.joinToString(" ")
        if(message.trim().isEmpty()){
            chat.sendMessage(TwitchBotConfig.channel, "No input has been provided")
            return@Command
        }
        val currentMessage = DiscordMessageContent(
            message = message,
            user = user.name,
            channel = DiscordBotConfig.gameChannelName
        )
        sendMessageToDiscordBot(currentMessage)
        chat.sendMessage(TwitchBotConfig.channel, "Message sent in ${DiscordBotConfig.gameChannelName}")
    }
)