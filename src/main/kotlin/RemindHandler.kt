import RemindHandler.reminders
import com.github.twitch4j.chat.TwitchChat
import config.TwitchBotConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object RemindHandler {
    fun setupReminders () {
        reminderFile = File("data/reminders.json")

        reminders = if (!reminderFile!!.exists()) {
            reminderFile!!.createNewFile()
            logger.info("Reminder file created.")
            mutableSetOf()
        } else {
            try {
                json.decodeFromString<MutableSet<Reminder>>(reminderFile!!.readText()).toMutableSet().also { currentRemindersData ->
                    logger.info("Existing reminder file found! Values: ${currentRemindersData.joinToString(" | ")}")
                }
            } catch (e: Exception) {
                logger.warn("Error while reading reminder file. Initializing empty set", e)
                mutableSetOf()
            }
        }
    }

    private var reminderFile: File? = null
    private var reminders: MutableSet<Reminder> = mutableSetOf()
        private set(value) {
            field = value
            reminderFile?.writeText(json.encodeToString(field))
        }
    // TODO: Remove as soon as solution was found
    var chat: TwitchChat? = null

    fun CommandHandlerScope.addToReminders(intervalTime: Duration, remindMessage: String) {
        val message = "${TwitchBotConfig.channel} ${TwitchBotConfig.remindEmote} Time's up ${TwitchBotConfig.remindEmote} Reminder for: " + remindMessage.ifEmpty {
            "I don't know. Something I guess? ${messageEvent.user.name} did not say for what ${TwitchBotConfig.remindEmoteFail}"
        }

        reminders = (reminders + Reminder(message, Clock.System.now() + intervalTime)).toMutableSet()
    }

    fun checkForReminds() {
        backgroundCoroutineScope.launch {
            while (true) {
                val dueReminders = reminders.filter { it.timestampDue <= Clock.System.now() }
                if (dueReminders.isNotEmpty()) {
                    dueReminders.forEach {
                        chat?.sendMessage(TwitchBotConfig.channel, it.message)
                        reminders = (reminders - it).toMutableSet()
                    }
                }
                delay(0.1.seconds)
            }
        }
    }
}