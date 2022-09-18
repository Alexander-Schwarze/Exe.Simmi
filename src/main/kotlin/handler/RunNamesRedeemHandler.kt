package handler

import com.github.twitch4j.chat.TwitchChat
import json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import logger
import java.io.File

class RunNamesRedeemHandler(private val chat: TwitchChat, private val runNamesFile: File) {
    private var runNames = mutableListOf<String>()
        private set(value) {
            field = value
            runNamesFile.writeText(json.encodeToString(field))
        }

    init {
        runNames = if (!runNamesFile.exists()) {
            runNamesFile.createNewFile()
            logger.info("Run names file created.")
            mutableListOf()
        } else {
            try {
                json.decodeFromString<List<String>>(runNamesFile.readText()).toMutableList().also { currentRemindersData ->
                    logger.info("Existing run names file found! Values: ${currentRemindersData.joinToString(" | ")}")
                }
            } catch (e: Exception) {
                logger.warn("Error while reading run names file. Initializing empty list", e)
                mutableListOf()
            }
        }

    }
}