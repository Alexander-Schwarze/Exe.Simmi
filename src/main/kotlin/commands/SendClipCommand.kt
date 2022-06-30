package commands

import TwitchBotConfig
import Command
import DiscordMessageContent
import TwitchBotConfig.explanationEmote
import dev.kord.rest.builder.message.EmbedBuilder.Limits.title
import sendMessageToDiscordBot
import kotlin.time.Duration.Companion.seconds
import logger

val sendClipCommand: Command = Command(
    names = listOf("sc", "sendclip"),
    handler = { arguments ->
        val link = arguments.findLast {
            var eligibleLink = false
            for (i in TwitchBotConfig.allowedLinks){
                eligibleLink = it.startsWith(i)
                if(eligibleLink)
                    break
            }
            eligibleLink
        }

        if (link.isNullOrBlank()) {
            chat.sendMessage(TwitchBotConfig.channel,
                "No link has been provided ${TwitchBotConfig.rejectEmote} " +
                        "Following link types are allowed: " +
                        TwitchBotConfig.allowedLinks.map { "'${it}'" }.let { links ->
                            listOf(links.dropLast(1).joinToString(), links.last()).filter { it.isNotBlank() }.joinToString(" and ")} +
                        " $explanationEmote")
            addedUserCooldown = 5.seconds
            return@Command
        }

        val currentMessageContent = DiscordMessageContent(
            message = "",
            messageLink = link,
            title = "Clip for ",
            user = user.name,
            channelId = DiscordBotConfig.clipChannelId
        )

        val channel = sendMessageToDiscordBot(currentMessageContent)
        val messageSentOnTwitchChat = chat.sendMessage(TwitchBotConfig.channel, "Message sent in #${channel.name} ${TwitchBotConfig.confirmEmote}")
        logger.info("Message sent to Twich Chat: $messageSentOnTwitchChat")

        addedUserCooldown = TwitchBotConfig.userCooldown
        addedCommandCooldown = TwitchBotConfig.commandCooldown
    }
)