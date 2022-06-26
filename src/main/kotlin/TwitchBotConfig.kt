import java.io.File
import java.util.*

object TwitchBotConfig {
    private val properties = Properties().apply {
        load(File("data/twitchBotConfig.properties").inputStream())
    }

    val channel: String = properties.getProperty("channel")
    val onlyMods = properties.getProperty("only_mods") == "true"
    val commandPrefix: String = properties.getProperty("command_prefix")
}