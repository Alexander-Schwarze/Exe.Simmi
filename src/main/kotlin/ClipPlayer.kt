import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name

fun setupClipPlayer(){
    val clipDirectory = Paths.get(ClipPlayerConfig.clipLocation)

    if(!clipDirectory.isDirectory()){
        logger.error("Clip directory is not existent. Please make sure to give the right path")
        return
    }

    val clips = Files.walk(clipDirectory)
        .filter {
            it.extension in ClipPlayerConfig.allowedVideoFiles
        }.map {
            ClipInformation (
                name = it.name
            )
        }

    val oldClips: MutableList<ClipInformation>
    val playlistFile = Paths.get("data/currentClipPlaylist.json").toFile().also { file ->
        if (!file.exists()) {
            file.createNewFile()
            logger.info("Playlist file created")
        } else {
            oldClips = Json.decodeFromString(file.readText())
            logger.info("Existing playlist file found! Read Data. ${oldClips.joinToString(" | ") { "${it.name}: played = ${it.played}" }}")
        }
    }
    //File("data/currentClipPlaylist.json").writeText(Json.encodeToString())
}