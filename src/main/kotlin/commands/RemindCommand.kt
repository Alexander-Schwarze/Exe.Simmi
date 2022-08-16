package commands

import Command
import config.TwitchBotConfig
import logger
import startRemindInterval
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val remindCommand: Command = Command(
    names = listOf("remind", "r"),
    handler = { arguments ->
        if (messageEvent.user.name !in TwitchBotConfig.remindCommandUsers && messageEvent.user.id !in TwitchBotConfig.remindCommandUsers) {
            return@Command
        }

        if(messageEvent.user.id !in TwitchBotConfig.remindCommandUsers) {
            logger.warn("Whitelisted user ${messageEvent.user.name} used the remind command. Please use following ID in the properties file instead of the name: ${messageEvent.user.id}")
        }

        if (arguments.isEmpty()) {
            chat.sendMessage(
                TwitchBotConfig.channel,
                "No arguments have been provided. Please provide at least the waiting duration in minutes ${TwitchBotConfig.explanationEmote}"
            )
            addedUserCooldown = 5.seconds
            return@Command
        }

        val intervalTime = try {
            arguments[0].toDouble().minutes
        } catch (e: NumberFormatException) {
            0.minutes
        }

        if (intervalTime.isPositive()) {
            chat.sendMessage(TwitchBotConfig.channel, "Invalid interval time. It must be greater than zero!")
            addedUserCooldown = 5.seconds
            return@Command
        }

        addedUserCooldown = TwitchBotConfig.userCooldown
        addedCommandCooldown = TwitchBotConfig.commandCooldown

        chat.sendMessage(TwitchBotConfig.channel, TwitchBotConfig.explanationEmote)
        startRemindInterval(intervalTime, arguments.drop(1).joinToString(" "))
    }
)