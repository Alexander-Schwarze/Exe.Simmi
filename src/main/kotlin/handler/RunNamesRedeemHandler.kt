package handler

import com.github.twitch4j.chat.TwitchChat
import json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import logger
import java.io.File
import kotlin.math.absoluteValue

class RunNamesRedeemHandler(private val chat: TwitchChat, private val runNamesFile: File) {
    private var runNames = listOf<String>()
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

    fun popNextRunName(): String {
        return if(runNames.isNotEmpty()) {
            runNames.first().also {
                runNames = runNames.drop(1).toMutableList()
                logger.info("Popped new runner: $it")
            }
        } else {
            ""
        }.also {
            logger.info("New run names list: ${runNames.joinToString("|")}")
        }
    }

    fun addRunName(name: String) {
        runNames = runNames + name
        logger.info("Added run name $name to the list!")
        logger.info("New run names list: ${runNames.joinToString("|")}")
    }

    fun getNextRunners(amount: Int): List<String> {
        return try {
            runNames.subList(0, amount.absoluteValue)
        } catch (e: java.lang.IndexOutOfBoundsException) {
            runNames.subList(0, runNames.size)
        }
    }
}