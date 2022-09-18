package commands

import config.BuildInfo
import Command
import config.TwitchBotConfig
import commands

val helpCommand: Command = Command(
    names = listOf("help"),
    description = "Displays bot version and all available commands. If a valid command is given as argument, the description of said command will be displayed instead.",
    handler = { arguments ->
        var message =
                """ 
                    Bot Version ${BuildInfo.version}. 
                    Available commands: 
                    ${commands.joinToString("; ") { command -> command.names.joinToString("|") { "${TwitchBotConfig.commandPrefix}${it}" } }}.
                """.trimIndent()

        val command = commands.find { arguments.firstOrNull() in it.names }
        if(command != null) {
            message =
                """
                    Command ${arguments.first()}: 
                    ${command.description}
                """.trimIndent()
        }
        chat.sendMessage(
            TwitchBotConfig.channel,
            message
        )
    }
)