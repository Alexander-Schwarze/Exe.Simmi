package commands

import TwitchBotConfig
import Command
import DiscordMessageContent
import sendMessageToDiscordBot

val gameSuggestionCommand: Command = Command(
    names = listOf("gs", "gamesuggestion"),
    handler = { arguments ->
        val message = arguments.joinToString(" ")
        if(message.trim().isEmpty()){
            chat.sendMessage(TwitchBotConfig.channel, "No input has been provided")
            return@Command
        }
        val currentMessageContent = DiscordMessageContent(
            message = message,
            user = user.name,
            channel = DiscordBotConfig.gameChannel
        )
        sendMessageToDiscordBot(currentMessageContent)
        chat.sendMessage(TwitchBotConfig.channel, "Message sent in ${DiscordBotConfig.gameChannel.name}")
    }
)