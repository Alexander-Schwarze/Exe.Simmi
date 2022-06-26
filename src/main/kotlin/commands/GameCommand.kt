package commands

import TwitchBotConfig
import Command

val gameCommand: Command = Command(
    names = listOf("game"),
    handler = {
        chat.sendMessage(TwitchBotConfig.channel, "gameCommand")
    }
)