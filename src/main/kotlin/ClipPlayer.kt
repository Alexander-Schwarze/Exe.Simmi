import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name

class ClipPlayer {
    private var clips = mutableListOf<ClipInformation>()

    fun setupClipPlayer() {
        val clipDirectory = Paths.get(ClipPlayerConfig.clipLocation)

        if (!clipDirectory.isDirectory()) {
            logger.error("Clip directory is not existent. Please make sure to give the right path")
            return
        }

        clips = Files.walk(clipDirectory)
            .filter {
                it.extension in ClipPlayerConfig.allowedVideoFiles
            }.map {
                ClipInformation(
                    name = it.name
                )
            }.toList()

        if (clips.isEmpty()) {
            logger.error("No clips in folder ${ClipPlayerConfig.clipLocation}")
            return
        }

        var oldClips = mutableListOf<ClipInformation>()
        val playlistFile = Paths.get("data/currentClipPlaylist.json").toFile().also { file ->
            if (!file.exists()) {
                file.createNewFile()
                logger.info("Playlist file created")
            } else {
                oldClips = Json.decodeFromString(file.readText())
                logger.info("Existing playlist file found! Read Data. ${oldClips.joinToString(" | ") { "${it.name}: played = ${it.played}" }}")
            }
        }

        // Update video files if they got played or not
        clips.forEach { clip ->
            if (oldClips.any { clip.name == it.name }) {
                clip.played = oldClips.first { it.name == clip.name }.played
            }
        }

        logger.info("Clip player setup finished")
        //File("data/currentClipPlaylist.json").writeText(Json.encodeToString())
    }
}