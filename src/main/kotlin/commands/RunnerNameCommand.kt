package commands

import Command
import DiscordMessageContent
import config.TwitchBotConfig
import logger
import saveLastRunnersSplit
import sendMessageToDiscordBot
import updateCurrentRunnerName
import kotlin.time.Duration.Companion.seconds

val runnerNameCommand: Command = Command(
    names = listOf("rn", "runnername"),
    description = "Pops the next runner name out of the list and saves the current runner's split name to a discord channel. Either the current split name gets read automatically or provided manually. Usually, this command is only for few people enabled (e.g. only the streamer).",
    handler = { arguments ->
        if(messageEvent.user.name != TwitchBotConfig.channel) {
            return@Command
        }

        saveLastRunnersSplit(arguments.joinToString(" ").trim())

        val runnerName = runNamesRedeemHandler.popNextRunName()
        val twitchChatMessage = "${TwitchBotConfig.remindEmote} Next Runner is: " +
                if(runnerName != "") {
                    runnerName
                } else {
                    "No one, we are missing runners!"
                }
        updateCurrentRunnerName(runnerName)

        chat.sendMessage(TwitchBotConfig.channel, twitchChatMessage)
    }
)