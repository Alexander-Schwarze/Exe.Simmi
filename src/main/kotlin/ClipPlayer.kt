import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ClipPlayer private constructor(clips: List<ClipInformation>, playListFile: File) {
    companion object {
        val instance = run {
            val clipDirectory = File(ClipPlayerConfig.clipLocation)

            if (!clipDirectory.isDirectory) {
                logger.error("Clip directory is nonexistent. Please make sure to give the right path.")
                return@run null
            }

            val playListFile = File("data/currentClipPlaylist.json")

            val clipsInCurrentPlaylist = playListFile.let { file ->
                if (!file.exists()) {
                    file.createNewFile()
                    logger.info("Playlist file created")
                    listOf()
                } else {
                    Json.decodeFromString<List<ClipInformation>>(file.readText()).also { currentPlaylistData ->
                        logger.info("Existing playlist file found! Values: ${currentPlaylistData.joinToString(" | ") { "${it.name}: played = ${it.played}" }}")
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

            logger.info("Clips in folder ${ClipPlayerConfig.clipLocation} after applying playlist values: ${clips.joinToString(" | ") { "${it.name}: played = ${it.played}" }}")

            ClipPlayer(clips, playListFile)
        }
    }

    var clips = clips
        private set

    var playListFile = playListFile
        private set

    init {
        updatePlaylistFile()
    }
    fun updatePlaylistFile(){
        playListFile.writeText(Json.encodeToString(clips))
    }

    fun resetPlaylistFile() {
        clips.forEach{
            // TODO: How? :(
            // it.played = false
        }
        updatePlaylistFile()
    }
}