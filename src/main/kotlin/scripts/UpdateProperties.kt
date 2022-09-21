package scripts

import java.io.File

// Compile with: kotlinc UpdateProperties.kt -include-runtime -d UpdateProperties_1-4-0.jar

const val latestVersion = "1.4.0"

val defaultPropertiesValues = listOf(
    // ClipPlayer properties
    mapOf(
        // Since Version: 1.1.0
        Pair("clip_location", "D:\\\\Path\\\\To\\\\Video\\\\Files"),
        Pair("allowed_video_files", "mp4,webm"),
        // Since Version: 1.2.0
        Pair("port", "12345")
    ),
    // DiscordBotConfig properties
    mapOf(
        // Since Version: 1.0.0
        Pair("feedback_channel_id", "1234567890"),
        Pair("game_channel_id", "1234567890"),
        // Since Version: 1.0.2
        Pair("clip_channel_id", "1234567890"),
        // Since Version: 1.1.0
        Pair("embed_accent_color", "#E9A623")
    ),
    // TwitchBotConfig properties
    mapOf(
        // Since Version: 1.0.0
        Pair("channel", "channelName"),
        Pair("only_mods", "false"),
        Pair("user_cooldown_seconds", "30"),
        Pair("command_prefix", "#"),
        Pair("command_cooldown_seconds", "10"),
        // Since Version: 1.0.2
        Pair("leave_emote", "peepoLeave"),
        Pair("arrive_emote", "peepoArrive"),
        Pair("confirm_emote", "NODDERS"),
        Pair("reject_emote", "NOPPERS"),
        Pair("explanation_emote", "NOTED"),
        // Since Version: 1.1.0
        Pair("allowed_domains", "clips.twitch.tv/,www.twitch.tv/,www.youtube.com/,youtu.be/"),
        // Since Version: 1.2.2
        Pair("blacklisted_users", "ShardZero"),
        Pair("blacklist_emote", "FeelsOkayMan"),
        // Since Version: 1.3.0
        Pair("remind_command_users", "simmiexe,alexshadowolex"),
        Pair("remind_emote", "DinkDonk"),
        Pair("remind_emote_fail", "WorryStick"),
        // Since Version: 1.4.0
        Pair("run_name_redeem_id", "I AM THE RUN"),
        Pair("amount_displayed_runner_names", "5")
    )
)

// This file holds all properties, that should exist for the latest version in all files.
// Executing it will write the properties with default values of the latest version.
fun main() {
    try {
        val propertiesFiles = listOf(
            File("data/clipPlayer.properties"),
            File("data/discordBotConfig.properties"),
            File("data/twitchBotConfig.properties")
        )


        val outputString = mutableListOf<String>()

        outputString += "Checking for updates, latest verson: $latestVersion"

        defaultPropertiesValues.forEachIndexed { index, currentDefaultPropertiesMap ->
            val currentPropertiesFile = propertiesFiles[index]
            var currentContent = mutableListOf<String>()
            currentDefaultPropertiesMap.forEach { property ->
                if (!currentPropertiesFile.exists()) {
                    currentPropertiesFile.createNewFile()
                    outputString += "Created properties file ${currentPropertiesFile.name}"
                } else if (currentContent.isEmpty()) {
                    currentContent = currentPropertiesFile.readLines().toMutableList()
                }

                if (currentContent.find { it.contains(property.key) } == null) {
                    currentContent += (property.key + "=" + property.value)
                    outputString += "Added property: \"${property.key}\" with default value \"${property.value}\""
                }
            }

            currentPropertiesFile.writeText(currentContent.joinToString("\n"))
        }

        outputString += "Successfully updated properties!"
        Runtime.getRuntime().exec(
            arrayOf(
                "cmd", "/c", "start", "cmd", "/k",
                "echo ${outputString.joinToString("& echo.")}"
            )
        )
    } catch (e: Exception) {
        Runtime.getRuntime().exec(
            arrayOf(
                "cmd", "/c", "start", "cmd", "/k",
                "echo An error occured, see the exception here:& echo.${e.message}"
            )
        )
    }
}