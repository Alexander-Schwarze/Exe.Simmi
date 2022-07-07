import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ClipPlayer private constructor(
    private val clips: List<String>,
    playedClips: List<String>,
    private val playListFile: File
) {
    companion object {
        val instance = run {
            val clipDirectory = File(ClipPlayerConfig.clipLocation)

            if (!clipDirectory.isDirectory) {
                logger.error("Clip directory is nonexistent. Please make sure to give the right path.")
                return@run null
            }

            val playListFile = File("data/currentClipPlaylist.json")

            val playedClips = playListFile.let { file ->
                if (!file.exists()) {
                    file.createNewFile()
                    logger.info("Playlist file created")
                    listOf()
                } else {
                    Json.decodeFromString<List<String>>(file.readText()).also { currentPlaylistData ->
                        logger.info("Existing playlist file found! Values: ${currentPlaylistData.joinToString(" | ")}")
                    }
                }
            }

            val clips = clipDirectory.walk()
                .filter {
                    it.extension in ClipPlayerConfig.allowedVideoFiles
                }
                .map { it.name }
                .toList()

            if (clips.isEmpty()) {
                logger.error("No clips in folder ${ClipPlayerConfig.clipLocation}")
                return@run null
            }

            logger.info("Clips in folder after applying playlist values ${ClipPlayerConfig.clipLocation} : ${clips.joinToString(" | ") { "$it: played = ${it in playedClips}" }}")

            ClipPlayer(clips, playedClips, playListFile)
        }
    }

    private var unplayedClips = playedClips
        private set(value) {
            playListFile.writeText(Json.encodeToString(clips))
            field = value
        }

    fun popNextRandomClip(): String {
        if (unplayedClips.isEmpty()) {
            resetPlaylistFile()
        }

        return unplayedClips.random().also {
            unplayedClips = unplayedClips - it
        }
    }

    fun resetPlaylistFile() {
        unplayedClips = clips
    }
}