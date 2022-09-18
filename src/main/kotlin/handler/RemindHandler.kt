package handler

import Reminder
import com.github.twitch4j.chat.TwitchChat
import config.TwitchBotConfig
import json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import logger
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RemindHandler(private val chat: TwitchChat, private val reminderFile: File, checkerScope: CoroutineScope) {
    private var reminders = setOf<Reminder>()
        private set(value) {
            field = value
            reminderFile.writeText(json.encodeToString(field))
        }

    init {
        reminders = if (!reminderFile.exists()) {
            reminderFile.createNewFile()
            logger.info("Reminder file created.")
            mutableSetOf()
        } else {
            try {
                json.decodeFromString<Set<Reminder>>(reminderFile.readText()).toMutableSet().also { currentRemindersData ->
                    logger.info("Existing reminder file found! Values: ${currentRemindersData.joinToString(" | ")}")
                }
            } catch (e: Exception) {
                logger.warn("Error while reading reminder file. Initializing empty set", e)
                mutableSetOf()
            }
        }

        checkerScope.launch {
            startReminderCheck()
        }
    }

    fun addToReminders(intervalTime: Duration, userName: String, remindMessage: String) {
        val message = "${TwitchBotConfig.channel} ${TwitchBotConfig.remindEmote} Time's up ${TwitchBotConfig.remindEmote} Reminder for: " + remindMessage.ifEmpty {
            "I don't know. Something I guess? $userName did not say for what ${TwitchBotConfig.remindEmoteFail}"
        }

        @Suppress("SuspiciousCollectionReassignment")
        reminders += Reminder(message, Clock.System.now() + intervalTime)
    }

    private suspend fun startReminderCheck() {
        while (true) {
            reminders
                .filter { it.timestampDue <= Clock.System.now() }
                .forEach {
                    chat.sendMessage(TwitchBotConfig.channel, it.message)
                    @Suppress("SuspiciousCollectionReassignment")
                    reminders -= it
                }

            delay(1.seconds)
        }
    }
}