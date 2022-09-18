package commands

import Command
import config.TwitchBotConfig
import logger
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val remindCommand: Command = Command(
    names = listOf("remind", "r"),
    description = "Saves a remind message for given duration. If the time is due, the bot will post the given message or a default one, if there was no message given.",
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

        if (intervalTime.inWholeMilliseconds <= 0) {
            chat.sendMessage(TwitchBotConfig.channel, "Invalid interval time. It must be greater than zero!")
            addedUserCooldown = 5.seconds
            return@Command
        }

        addedUserCooldown = TwitchBotConfig.userCooldown
        addedCommandCooldown = TwitchBotConfig.commandCooldown

        chat.sendMessage(TwitchBotConfig.channel, "${TwitchBotConfig.explanationEmote} You will be reminded in $intervalTime")
        remindHandler.addToReminders(intervalTime, messageEvent.user.name, arguments.drop(1).joinToString(" "))
    }
)