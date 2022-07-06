import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

class ClipPlayer private constructor(clips: List<ClipInformation>) {
    companion object {
        val instance = run {
            val clipDirectory = File(ClipPlayerConfig.clipLocation)

            if (!clipDirectory.isDirectory) {
                logger.error("Clip directory is nonexistent. Please make sure to give the right path.")
                return@run null
            }

            val clipsInCurrentPlaylist = File("data/currentClipPlaylist.json").let { file ->
                if (!file.exists()) {
                    file.createNewFile()
                    logger.info("Playlist file created")
                    listOf()
                } else {
                    Json.decodeFromString<List<ClipInformation>>(file.readText()).also { currentPlaylistData ->
                        logger.info("Existing playlist file found! Read Data. ${currentPlaylistData.joinToString(" | ") { "${it.name}: played = ${it.played}" }}")
                    }
                }
            }

            val clips = clipDirectory.walk()
                .filter {
                    it.extension in ClipPlayerConfig.allowedVideoFiles
                }
                .map { clipFile ->
                    ClipInformation(
                        name = clipFile.name,
                        played = clipsInCurrentPlaylist.any { clipFile.name == it.name }
                    )
                }
                .toList()

            if (clips.isEmpty()) {
                logger.error("No clips in folder ${ClipPlayerConfig.clipLocation}")
                return@run null
            }

            ClipPlayer(clips)
        }
    }

    var clips = clips
        private set
}