import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.TextChannelBehavior
import dev.kord.core.entity.channel.TextChannel
import java.io.OutputStream

class MultiOutputStream(private vararg val streams: OutputStream) : OutputStream() {
    override fun close() = streams.forEach(OutputStream::close)
    override fun flush() = streams.forEach(OutputStream::flush)

    override fun write(b: Int) = streams.forEach {
        it.write(b)
    }

    override fun write(b: ByteArray) = streams.forEach {
        it.write(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) = streams.forEach {
        it.write(b, off, len)
    }
}

data class DiscordMessageContent (
    val message: String,
    val user: String,
    val channelId: Snowflake
)