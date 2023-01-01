package commands

import Command
import config.TwitchBotConfig
import logger
import pluralForm

val runnersListCommand: Command = Command(
    names = listOf("rl", "runnerslist"),
    description = "Gives a list of the next ${TwitchBotConfig.amountDisplayedRunnerNames} runner names for ${TwitchBotConfig.channel}'s challenge runs. If a name is provided after the command, it will give the name's next place in queue instead.",
    handler = {arguments ->
        val desiredAmount = TwitchBotConfig.amountDisplayedRunnerNames
        val runners = runNamesRedeemHandler.getNextRunnersList(desiredAmount)
        val name = if(arguments.isNotEmpty()) {
            arguments[0].trim().also { logger.info("Command got called with arguments, name to search for is \"$it\"") }
        } else {
            null
        }
        chat.sendMessage(
            TwitchBotConfig.channel,
            if(name != null) {
                runNamesRedeemHandler.getMessageForPositionInQueue(name).also { logger.info("Searched for name in queue") }
            } else {
                if (runners.isEmpty()) {
                    "No further runners. Redeem it, I dare ya ${TwitchBotConfig.confirmEmote}"
                } else {
                    "Currently in queue, the next " +
                            if (runners.size > 1) {
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
                }.also { logger.info("Posted the current queue") }
            }
        )

        addedUserCooldown = TwitchBotConfig.userCooldown
        addedCommandCooldown = TwitchBotConfig.commandCooldown
    }
)