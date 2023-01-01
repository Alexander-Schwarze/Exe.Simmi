package handler

import config.TwitchBotConfig
import json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import logger
import java.io.File
import kotlin.math.absoluteValue

class RunNamesRedeemHandler(private val runNamesFile: File) {
    private var runNames = listOf<RunNameUser>()
        private set(value) {
            field = value
            runNamesFile.writeText(json.encodeToString(field))
        }

    private val defaultColor = "FFFFFF"

    init {
        runNames = if (!runNamesFile.exists()) {
            runNamesFile.createNewFile()
            logger.info("Run names file created.")
            mutableListOf()
        } else {
            try {
                json.decodeFromString<List<RunNameUser>>(runNamesFile.readText()).toMutableList().also { currentRemindersData ->
                    logger.info("Existing run names file found! Values: ${currentRemindersData.joinToString(" | ")}")
                }
            } catch (e: Exception) {
                logger.warn("Error while reading run names file. Did something alter its content? Manually check the content below, fix it, put it in and restart the app!", e)
                try {
                    logger.warn("\n" + runNamesFile.readText())
                } catch (e: Exception) {
                    logger.error("Something went wrong with reading the file content yet again. Aborting...")
                    throw ExceptionInInitializerError()
                }
                logger.info("Initializing empty list!")
                mutableListOf()
            }
        }
    }

    fun popNextRunName(): RunNameUser {
        return if(runNames.isNotEmpty()) {
            runNames.first().also {
                runNames = runNames.drop(1).toMutableList()
                logger.info("Popped new runner: $it")
            }
        } else {
            RunNameUser("", "")
        }.also {
            logger.info("New run names list: ${runNames.joinToString("|")}")
        }
    }

    fun addRunName(name: String) {
        runNames = (runNames + RunNameUser(name, defaultColor)).also {
            logger.info("Added run name $name to the list!")
            logger.info("New run names list: ${it.joinToString("|")}")
        }
    }

    fun getNextRunners(amount: Int): List<String> {
        return try {
            runNames.map { it.name }.subList(0, amount.absoluteValue)
        } catch (e: java.lang.IndexOutOfBoundsException) {
            runNames.map { it.name }.subList(0, runNames.size)
        }
    }

    fun getMessageForPositionInQueue(name: String): String {
        val index = runNames.map { it.name.lowercase() }.indexOf(name.lowercase())
        return if(index != -1) {
            "Stupid question, stop being impatient ${TwitchBotConfig.runnersListIndexEmote} $name's position in queue is ${index + 1} of ${runNames.size} ${TwitchBotConfig.explanationEmote}"
        } else {
            "$name is not in queue. They should redeem it, if they want to change this ${TwitchBotConfig.confirmEmote}"
        }
    }
}

@kotlinx.serialization.Serializable
data class RunNameUser (
    val name: String,
    val chatColor: String
)