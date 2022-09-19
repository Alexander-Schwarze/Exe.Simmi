package commands

import Command
import config.TwitchBotConfig
import pluralForm

val runnersListCommand: Command = Command(
    names = listOf("rl", "runnerslist"),
    handler = {
        val desiredAmount = 5
        val runners = runNamesRedeemHandler.getNextRunners(desiredAmount)
        chat.sendMessage(
            TwitchBotConfig.channel,
            if (runners.isEmpty()) {
                "No further runners. Redeem it, I dare ya ${TwitchBotConfig.confirmEmote}"
            } else {
                "Currently in queue, the next " +
                        if(runners.size > 1) {
                            if (runners.size < desiredAmount) {
                                "${runners.size} "
                            } else {
                                "$desiredAmount "
                            }
                        } else {
                            ""
                        } +
                        "${"runner".pluralForm(runners.size)} ${"is".pluralForm(runners.size)}: ${
                            runners.map { it }.let { names ->
                                listOf(names.dropLast(1).joinToString(), names.last()).filter { it.isNotBlank() }
                                    .joinToString(" and ")
                            }
                        }"
            }
        )
    }
)