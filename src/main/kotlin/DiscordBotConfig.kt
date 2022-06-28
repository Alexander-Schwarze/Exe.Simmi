import java.io.File
import java.util.*

object DiscordBotConfig {
    private val properties = Properties().apply {
        load(File("data/discordBotconfig.properties").inputStream())
    }

    val feedbackChannel = DiscordChannelData (
        name = properties.getProperty("feedback_channel_name"),
        id = properties.getProperty("feedback_channel_id").toLong()
    )
    val gameChannel = DiscordChannelData(
        name = properties.getProperty("game_channel_name"),
        id = properties.getProperty("game_channel_id").toLong()
    )
}