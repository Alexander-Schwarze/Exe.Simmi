package commands

import config.BuildInfo
import Command
import config.TwitchBotConfig
import commands

val helpCommand: Command = Command(
    names = listOf("help"),
    handler = {
        chat.sendMessage(
            TwitchBotConfig.channel,
            "Bot Version ${BuildInfo.version}. " +
                    "Available commands: " +
                    "${commands.joinToString("; ") { command -> command.names.joinToString("|") { "${TwitchBotConfig.commandPrefix}${it}" } }}.")
    }
)