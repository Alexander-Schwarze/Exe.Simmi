package commands

import Command
import config.TwitchBotConfig

val runnerNameCommand: Command = Command(
    names = listOf("rn", "runnername"),
    handler = {
        if(messageEvent.user.id != TwitchBotConfig.channel) {
            return@Command
        }
        val runnerName = runNamesRedeemHandler.popNextRunName()
        val message = "${TwitchBotConfig.remindEmote} Next Runner is: " +
                if(runnerName != "") {
                    runnerName
                } else {
                    "No one, we are missing runners!"
                }
        chat.sendMessage(TwitchBotConfig.channel, message)
    }
)