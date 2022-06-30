import dev.kord.common.entity.Snowflake
import java.io.File
import java.util.*

object DiscordBotConfig {
    private val properties = Properties().apply {
        load(File("data/discordBotconfig.properties").inputStream())
    }

    val feedbackChannelId = Snowflake(properties.getProperty("feedback_channel_id").toLong())
    val gameChannelId = Snowflake(properties.getProperty("game_channel_id").toLong())
    val clipChannelId = Snowflake(properties.getProperty("clip_channel_id").toLong())
}