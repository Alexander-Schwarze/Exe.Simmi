package commands

import TwitchBotConfig
import Command
import DiscordMessageContent
import sendMessageToDiscordBot

val feedbackCommand: Command = Command(
    names = listOf("fb", "feedback"),
    handler = { arguments ->
        val message = arguments.joinToString(" ")
        if(message.trim().isEmpty()){
            chat.sendMessage(TwitchBotConfig.channel, "No input has been provided")
            return@Command
        }
        val currentMessageContent: DiscordMessageContent = DiscordMessageContent(
            message = message,
            user = user.name,
            channel = DiscordBotConfig.feedbackChannel
        )
        sendMessageToDiscordBot(currentMessageContent)
        chat.sendMessage(TwitchBotConfig.channel, "Message sent in ${DiscordBotConfig.feedbackChannel.name}")
    }
)