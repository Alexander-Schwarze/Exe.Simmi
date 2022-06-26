import java.io.File
import java.util.*

object DiscordBotConfig {
    private val properties = Properties().apply {
        load(File("data/discordBotconfig.properties").inputStream())
    }

    val feedbackChannelName: String = properties.getProperty("feedback_channel_name")
    val gameChannelName: String = properties.getProperty("game_channel_name")
}