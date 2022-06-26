package commands

import TwitchBotConfig
import Command

val feedbackCommand: Command = Command(
    names = listOf("fb", "feedback"),
    handler = {
        chat.sendMessage(TwitchBotConfig.channel, "feedbackCommand")
    }
)