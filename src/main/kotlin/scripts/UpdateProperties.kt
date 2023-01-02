package scripts

import java.io.File

// Compile with: kotlinc UpdateProperties.kt -include-runtime -d UpdateProperties_2-0-0.jar

const val latestVersion = "2.0.0"
val propertiesFolder = File("data\\properties")

val propertiesFilesToProperties = mapOf(
    Pair(
        File("${propertiesFolder.absolutePath}\\clipPlayer.properties"),
        // ClipPlayer properties
        mapOf(
            // Since Version: 1.1.0
            Pair("clip_location", "D:\\\\Path\\\\To\\\\Video\\\\Files"),
            Pair("allowed_video_files", "mp4,webm"),
            // Since Version: 1.2.0
            Pair("port", "12345")
        )
    ),
    Pair(
        File("${propertiesFolder.absolutePath}\\discordBotConfig.properties"),
        // DiscordBotConfig properties
        mapOf(
            // Since Version: 1.0.0
            Pair("feedback_channel_id", "1234567890"),
            Pair("game_channel_id", "1234567890"),
            // Since Version: 1.0.2
            Pair("clip_channel_id", "1234567890"),
            // Since Version: 1.1.0
            Pair("embed_accent_color", "#E9A623"),
            // Since Version: 1.4.3
            Pair("ended_run_channel_id", "1234567890")
        ),
    ),
    Pair(
        File("${propertiesFolder.absolutePath}\\googleSpreadSheetConfig.properties"),
        // GoogleSpreadSheetConfig properties
        mapOf(
            // Since Version: 2.0.0
            Pair("spread_sheet_id", "abc123"),
            Pair("sheet_name", "Sheet Name"),
            Pair("first_data_cell", "A1"),
            Pair("last_data_cell", "B2")
        ),
    ),
    Pair(
        File("${propertiesFolder.absolutePath}\\twitchBotConfig.properties"),
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
            Pair("amount_displayed_runner_names", "5"),
            // Since Version: 1.4.2
            Pair("current_runner_name_pre_text", "Current Runner:"),
            Pair("current_runner_name_post_text", "!dslb"),
            // Since Version: 1.4.3
            Pair("hit_counter_location", "D:\\\\Path\\\\To\\\\HitCounterManager"),
            Pair("runners_list_index_emote", "WorryStick")
        )
    )
)

// This file holds all properties, that should exist for the latest version in all files.
// Executing it will write the properties with default values of the latest version.
fun main() {
    try {
        val outputString = mutableListOf<String>()

        outputString += "Checking for updates, latest verson: $latestVersion"

        outputString += if(propertiesFolder.exists() && propertiesFolder.isDirectory) {
            ""
        } else {
            propertiesFolder.mkdirs()
            "& echo.Created properties-folder \"${propertiesFolder.name}\""
        }

        propertiesFilesToProperties.forEach { fileMapEntry ->
            val currentPropertiesFile = fileMapEntry.key
            val properties = fileMapEntry.value
            var currentContent = mutableListOf<String>()
            properties.forEach { property ->
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